package com.hardgym.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
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
    private static final Set<AbstractClientPlayerEntity> REGISTERED_PLAYERS =
            Collections.newSetFromMap(new WeakHashMap<AbstractClientPlayerEntity, Boolean>());
    private static boolean unavailable;
    private static boolean errorPrinted;

    private OptionalPlayerAnimator() {}

    public static void ensureRegistered(AbstractClientPlayerEntity player) {
        if (player == null || unavailable || REGISTERED_PLAYERS.contains(player)) return;

        try {
            ClassLoader loader = OptionalPlayerAnimator.class.getClassLoader();
            Class<?> accessClass = Class.forName(
                    "dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess", true, loader);
            Class<?> animationInterface = Class.forName(
                    "dev.kosmx.playerAnim.api.layered.IAnimation", true, loader);
            Class<?> vecClass = Class.forName(
                    "dev.kosmx.playerAnim.core.util.Vec3f", true, loader);

            Constructor<?> vecCtor = vecClass.getConstructor(float.class, float.class, float.class);

            Method getLayer = null;
            for (Method method : accessClass.getMethods()) {
                if (method.getName().equals("getPlayerAnimLayer") && method.getParameterTypes().length == 1) {
                    getLayer = method;
                    break;
                }
            }
            if (getLayer == null) throw new NoSuchMethodException("PlayerAnimationAccess.getPlayerAnimLayer");

            Object animationStack = getLayer.invoke(null, player);
            if (animationStack == null) return;

            Object animation = Proxy.newProxyInstance(
                    loader,
                    new Class<?>[]{animationInterface},
                    (proxy, method, args) -> {
                        String name = method.getName();

                        if (name.equals("isActive")) {
                            return SquatClientState.findActiveSquat(player) != null
                                    || DeadliftClientState.findActiveDeadlift(player) != null;
                        }

                        if (name.equals("get3DTransform") && args != null && args.length >= 4) {
                            String modelName = String.valueOf(args[0]);
                            String transformName;
                            Object transformType = args[1];
                            if (transformType instanceof Enum<?>) {
                                transformName = ((Enum<?>) transformType).name();
                            } else {
                                transformName = String.valueOf(transformType);
                            }

                            if ("BEND".equals(transformName)) {
                                // Deadlift: bend both legs at the knee.  The bend is strongest on the
                                // floor, weaker around the knees, and almost gone at lockout.
                                DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(player);
                                if (deadlift != null) {
                                    int anim = deadlift.state.get(com.hardgym.block.DeadliftBarBlock.ANIM);
                                    // Bottom deadlift position: keep the thigh exactly as in v0.8.6.
                                    // The user's manual sign flip confirmed this is the correct shin direction.
                                    // Reduce the bend amount so the lower leg straightens toward vertical and
                                    // the foot drops closer to the ground.
                                    float kneeBend = anim == 0 ? 1.3F : (anim == 1 ? 0.55F : 0.04F);
                                    if ("rightLeg".equals(modelName) || "right_leg".equals(modelName)
                                            || "leftLeg".equals(modelName) || "left_leg".equals(modelName)) {
                                        return vecCtor.newInstance(0.0F, kneeBend, 0.0F);
                                    }
                                    return args[3];
                                }

                                // Squat: keep the working bent-elbow grip from the previous versions.
                                if (SquatClientState.findActiveSquat(player) != null) {
                                    if ("rightArm".equals(modelName) || "right_arm".equals(modelName)) {
                                        return vecCtor.newInstance(-0.24F, -1.82F, 0.0F);
                                    }
                                    if ("leftArm".equals(modelName) || "left_arm".equals(modelName)) {
                                        return vecCtor.newInstance(0.24F, -1.82F, 0.0F);
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

            Method addLayer = null;
            for (Method method : animationStack.getClass().getMethods()) {
                if (method.getName().equals("addAnimLayer") && method.getParameterTypes().length == 2) {
                    addLayer = method;
                    break;
                }
            }
            if (addLayer == null) throw new NoSuchMethodException("AnimationStack.addAnimLayer");

            addLayer.invoke(animationStack, 900, animation);
            REGISTERED_PLAYERS.add(player);
        } catch (ClassNotFoundException missingLibrary) {
            // Libraries are optional for normal HardGym; only the true elbow bend is disabled.
            unavailable = true;
        } catch (Throwable error) {
            // Do not break the whole mod if a different PlayerAnimator build changes its API.
            if (!errorPrinted) {
                errorPrinted = true;
                System.err.println("[HardGym] PlayerAnimator elbow-bend integration failed: " + error);
            }
        }
    }
}
