package com.hardgym.client;

import com.hardgym.animation.ExerciseAnimation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Optional bridge to PlayerAnimator/BendyLib.
 *
 * HardGym is intentionally compiled without either library because this project uses
 * the old Fabric Loom 0.7 toolchain. Older Loom cannot parse the v2 access widener
 * used by some PlayerAnimator builds. Reflection keeps the normal HardGym build clean,
 * while still enabling real mid-arm bending when the two library mods are installed.
 */
public final class OptionalPlayerAnimator {
    // Only training BEND values override other mods; all other transforms pass through.
    private static final int TRAINING_PRIORITY = Integer.MAX_VALUE;
    private static final Map<AbstractClientPlayerEntity, RegisteredLayer> REGISTERED_PLAYERS =
            new WeakHashMap<>();
    private static AnimationBridge bridge;
    private static boolean unavailable;
    private static boolean errorPrinted;

    private OptionalPlayerAnimator() {}

    public static boolean hasBendLayer(LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity) || bridge == null
                || !(FabricLoader.getInstance().isModLoaded("bendylib")
                || FabricLoader.getInstance().isModLoaded("bendy-lib"))) return false;
        RegisteredLayer layer = REGISTERED_PLAYERS.get(entity);
        if (layer == null || layer.player.get() != entity || !layer.enabled || layer.animation == null) return false;
        try {
            Object stack = bridge.getLayer.invoke(null, entity);
            return stack != null && stack == layer.stack.get()
                    && (bridge.layers == null || bridge.contains(stack, layer.animation));
        } catch (Throwable error) {
            return false;
        }
    }

    public static void ensureRegistered(AbstractClientPlayerEntity player) {
        if (player == null || unavailable) return;
        RegisteredLayer layer = REGISTERED_PLAYERS.get(player);
        if (layer != null && layer.player.get() != player) {
            // Entity.equals/hashCode compare network IDs. Respawn can reuse an ID
            // for a different player object, but the proxy must follow that new object.
            disableLayer(layer);
            // Remove first: WeakHashMap.put would otherwise retain the OLD weak key,
            // allowing collection of the corpse to discard the new registration too.
            REGISTERED_PLAYERS.remove(player);
            layer = null;
        }
        if (layer == null) {
            layer = new RegisteredLayer(player);
            REGISTERED_PLAYERS.put(player, layer);
        }
        if (layer.retryTicks > 0) {
            layer.retryTicks--;
            return;
        }
        try {
            if (bridge == null) bridge = new AnimationBridge();
            Object stack = bridge.getLayer.invoke(null, player);
            if (stack == null) return; // PlayerAnimator can still be initializing this entity.
            Object previousStack = layer.stack.get();
            if (stack == previousStack && bridge.contains(stack, layer.animation)) return;
            if (layer.animation == null) layer.animation = createAnimation(layer);
            if (previousStack != null && previousStack != stack) {
                // A reload can give the same entity a new stack. Detach only our own layer.
                try {
                    bridge.removeLayer.invoke(previousStack, layer.animation);
                } catch (Throwable ignored) {
                    // An obsolete stack must not prevent registration in the current one.
                }
            }
            // Removal by identity is safe even if a failed add already inserted the layer.
            // Never remove by priority: that could delete another mod's animation.
            layer.stack = new WeakReference<>(stack);
            bridge.removeLayer.invoke(stack, layer.animation);
            bridge.addLayer.invoke(stack, TRAINING_PRIORITY, layer.animation);
        } catch (ClassNotFoundException missingLibrary) {
            unavailable = true; // The libraries remain optional.
        } catch (Throwable error) {
            // Retry this player after one second; other players continue registering normally.
            layer.retryTicks = 20;
            if (!errorPrinted) {
                errorPrinted = true;
                System.err.println("[HardGym] PlayerAnimator joint-bend integration failed; will retry: " + error);
            }
        }
    }

    public static void clearRegistrations() {
        for (RegisteredLayer layer : REGISTERED_PLAYERS.values()) disableLayer(layer);
        REGISTERED_PLAYERS.clear();
    }

    private static void disableLayer(RegisteredLayer layer) {
        layer.enabled = false;
        Object stack = layer.stack.get();
        if (bridge != null && stack != null && layer.animation != null) {
            try {
                bridge.removeLayer.invoke(stack, layer.animation);
            } catch (Throwable ignored) {
                // Even if detaching fails, this obsolete animation is now inactive.
            }
        }
    }

    private static Object createAnimation(RegisteredLayer layer) {
        Constructor<?> vecCtor = bridge.vecCtor;
        return Proxy.newProxyInstance(
                    OptionalPlayerAnimator.class.getClassLoader(),
                    new Class<?>[]{bridge.animationInterface},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        AbstractClientPlayerEntity player = layer.player.get();

                        if (name.equals("isActive")) {
                            return layer.enabled && player != null && (BenchClientState.findActiveBench(player) != null
                                    || SquatClientState.findActiveSquat(player) != null
                                    || DeadliftClientState.findActiveDeadlift(player) != null
                                    || DumbbellArmPose.isCurling(player));
                        }

                        if (name.equals("get3DTransform") && args != null && args.length >= 4) {
                            if (!layer.enabled || player == null) return args[3];
                            String modelName = String.valueOf(args[0]);
                            String transformName;
                            Object transformType = args[1];
                            if (transformType instanceof Enum<?>) {
                                transformName = ((Enum<?>) transformType).name();
                            } else {
                                transformName = String.valueOf(transformType);
                            }

                            if ("BEND".equals(transformName)) {
                                float tickDelta = ((Number)args[2]).floatValue();
                                // Use the same synchronized bar phase for both elbows and shoulders.
                                // Keep the accepted knee pose fixed throughout the rep.
                                BenchClientState.BenchMatch bench = BenchClientState.findActiveBench(player);
                                if (bench != null) {
                                    float anim = ExerciseClientState.phase(player.world, bench.pos, bench.state, tickDelta);
                                    if ("rightArm".equals(modelName) || "right_arm".equals(modelName)
                                            || "leftArm".equals(modelName) || "left_arm".equals(modelName)) {
                                        // Same forward-bend sign as the working squat arm pose.
                                        return vecCtor.newInstance(0.0F, -BenchPressArmPose.elbowBend(anim), 0.0F);
                                    }
                                    float kneeBend = 1.45F;
                                    if ("rightLeg".equals(modelName) || "right_leg".equals(modelName)
                                            || "leftLeg".equals(modelName) || "left_leg".equals(modelName)) {
                                        return vecCtor.newInstance(0.0F, kneeBend, 0.0F);
                                    }
                                    return args[3];
                                }

                                // Deadlift: bend both legs at the knee.  The bend is strongest on the
                                // floor, weaker around the knees, and almost gone at lockout.
                                DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(player);
                                if (deadlift != null) {
                                    float anim = ExerciseClientState.phase(player.world, deadlift.pos, deadlift.state, tickDelta);
                                    // Bottom deadlift position: keep the thigh exactly as in v0.8.6.
                                    // The user's manual sign flip confirmed this is the correct shin direction.
                                    // Reduce the bend amount so the lower leg straightens toward vertical and
                                    // the foot drops closer to the ground.
                                    float kneeBend = ExerciseAnimation.pose(anim, 1.3F, 0.55F, 0.04F);
                                    if ("rightLeg".equals(modelName) || "right_leg".equals(modelName)
                                            || "leftLeg".equals(modelName) || "left_leg".equals(modelName)) {
                                        return vecCtor.newInstance(0.0F, kneeBend, 0.0F);
                                    }
                                    return args[3];
                                }

                                // Squat: keep the working bent-elbow grip and now bend both knees
                                // through the movement as well. ANIM 0 = standing/top,
                                // ANIM 1 = halfway down, ANIM 2 = bottom squat.
                                SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(player);
                                if (squat != null) {
                                    float anim = ExerciseClientState.phase(player.world, squat.pos, squat.state, tickDelta);

                                    if ("rightArm".equals(modelName) || "right_arm".equals(modelName)) {
                                        return vecCtor.newInstance(-0.24F, -1.82F, 0.0F);
                                    }
                                    if ("leftArm".equals(modelName) || "left_arm".equals(modelName)) {
                                        return vecCtor.newInstance(0.24F, -1.82F, 0.0F);
                                    }

                                    // Keep the SAME positive knee-bend sign as the working deadlift.
                                    // The backwards squat was caused by the upper-leg pitch being
                                    // positive, not by this BEND sign.
                                    float kneeBend = ExerciseAnimation.pose(anim, 0.04F, 0.72F, 1.45F);
                                    if ("rightLeg".equals(modelName) || "right_leg".equals(modelName)
                                            || "leftLeg".equals(modelName) || "left_leg".equals(modelName)) {
                                        return vecCtor.newInstance(0.0F, kneeBend, 0.0F);
                                    }
                                    return args[3];
                                }

                                if (DumbbellArmPose.isCurling(player)) {
                                    boolean activePart = (DumbbellArmPose.usesArm(player, Arm.RIGHT)
                                            && ("rightArm".equals(modelName) || "right_arm".equals(modelName)))
                                            || (DumbbellArmPose.usesArm(player, Arm.LEFT)
                                            && ("leftArm".equals(modelName) || "left_arm".equals(modelName)));
                                    if (activePart) {
                                        float lift = DumbbellArmPose.lift(player, tickDelta);
                                        return vecCtor.newInstance(0.0F, -DumbbellArmPose.elbowBend(lift), 0.0F);
                                    }
                                }
                            }
                            return args[3];
                        }

                        // setupAnim/tick and similar void hooks do not need work here.
                        if (method.getReturnType() == Void.TYPE) return null;

                        // Keep Proxy's basic Object methods sane.
                        if (name.equals("toString")) return "HardGymTrainingBendAnimation";
                        if (name.equals("hashCode")) return System.identityHashCode(proxy);
                        if (name.equals("equals")) return proxy == (args == null ? null : args[0]);

                        // PlayerAnimator versions can add default convenience methods.
                        // Return a sensible neutral value without hard-linking to their classes.
                        Class<?> returnType = method.getReturnType();
                        if (returnType == Boolean.TYPE) return false;
                        if (returnType == Byte.TYPE) return (byte) 0;
                        if (returnType == Short.TYPE) return (short) 0;
                        if (returnType == Integer.TYPE) return 0;
                        if (returnType == Long.TYPE) return 0L;
                        if (returnType == Float.TYPE) return 0.0F;
                        if (returnType == Double.TYPE) return 0.0D;
                        if (returnType == Character.TYPE) return '\0';
                        if (returnType.isEnum()) {
                            Object[] constants = returnType.getEnumConstants();
                            if (constants != null) {
                                for (Object constant : constants) {
                                    if (constant instanceof Enum<?> && "NONE".equals(((Enum<?>) constant).name())) {
                                        return constant;
                                    }
                                }
                                if (constants.length > 0) return constants[0];
                            }
                        }
                        try {
                            Constructor<?> ctor = returnType.getDeclaredConstructor();
                            ctor.setAccessible(true);
                            return ctor.newInstance();
                        } catch (Throwable ignored) {
                            return null;
                        }
                    });

    }

    private static final class RegisteredLayer {
        final WeakReference<AbstractClientPlayerEntity> player;
        WeakReference<Object> stack = new WeakReference<>(null);
        Object animation;
        boolean enabled = true;
        int retryTicks;

        RegisteredLayer(AbstractClientPlayerEntity player) {
            this.player = new WeakReference<>(player);
        }
    }

    private static final class AnimationBridge {
        final Class<?> animationInterface;
        final Constructor<?> vecCtor;
        final Method getLayer;
        final Method addLayer;
        final Method removeLayer;
        final Field layers;
        final Method getRight;

        AnimationBridge() throws ReflectiveOperationException {
            ClassLoader loader = OptionalPlayerAnimator.class.getClassLoader();
            Class<?> access = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess", true, loader);
            animationInterface = Class.forName("dev.kosmx.playerAnim.api.layered.IAnimation", true, loader);
            vecCtor = Class.forName("dev.kosmx.playerAnim.core.util.Vec3f", true, loader)
                    .getConstructor(float.class, float.class, float.class);
            getLayer = access.getMethod("getPlayerAnimLayer", AbstractClientPlayerEntity.class);
            Class<?> stackType = getLayer.getReturnType();
            addLayer = stackType.getMethod("addAnimLayer", int.class, animationInterface);
            removeLayer = stackType.getMethod("removeLayer", animationInterface);

            // PlayerAnimator 1.16 exposes removal, but no public contains/get-layers API.
            // Inspect its existing list so healthy stacks are left alone on normal ticks.
            Field list = null;
            Method right = null;
            try {
                list = stackType.getDeclaredField("layers");
                list.setAccessible(true);
                right = Class.forName("dev.kosmx.playerAnim.core.util.Pair", true, loader).getMethod("getRight");
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                list = null;
                right = null;
            }
            layers = list;
            getRight = right;
        }

        boolean contains(Object stack, Object animation) throws ReflectiveOperationException {
            // On a different build without the list, refresh by identity without accumulating layers.
            if (layers == null || animation == null) return false;
            for (Object entry : (Iterable<?>)layers.get(stack)) {
                if (getRight.invoke(entry) == animation) return true;
            }
            return false;
        }
    }
}
