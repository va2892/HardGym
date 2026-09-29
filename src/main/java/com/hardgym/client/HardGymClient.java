package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.player.GymPhysiqueClientState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class HardGymClient implements ClientModInitializer {
    private boolean lastBenchPressHeld = false;
    private boolean lastSquatHeld = false;
    private boolean lastDeadliftHeld = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HardGymMod.DEADLIFT_STATE_PACKET,
                (client, handler, buf, responseSender) -> {
                    UUID lifterUuid = buf.readUuid();
                    boolean active = buf.readBoolean();
                    BlockPos barPos = active ? buf.readBlockPos() : null;
                    client.execute(() -> DeadliftClientState.setSyncedDeadlift(lifterUuid, barPos, active));
                });

        ClientPlayNetworking.registerGlobalReceiver(HardGymMod.PHYSIQUE_SYNC_PACKET,
                (client, handler, buf, responseSender) -> {
                    int strength = buf.readInt();
                    int bench = buf.readInt();
                    int squat = buf.readInt();
                    int deadlift = buf.readInt();
                    client.execute(() -> GymPhysiqueClientState.update(strength, bench, squat, deadlift));
                });

        ClientPlayNetworking.registerGlobalReceiver(HardGymMod.GYM_HUD_PACKET,
                (client, handler, buf, responseSender) -> {
                    int mode = buf.readUnsignedByte();
                    int weightKg = buf.readInt();
                    int reps = buf.readInt();
                    int stamina = buf.readInt();
                    int fatigue = buf.readInt();
                    int progress = buf.readInt();
                    int health = buf.readInt();
                    int maxUnlockedKg = buf.readInt();
                    client.execute(() -> GymHudOverlay.update(
                            mode, weightKg, reps, stamina, fatigue, progress, health, maxUnlockedKg
                    ));
                });

        HudRenderCallback.EVENT.register((matrices, tickDelta) -> GymHudOverlay.render(matrices));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GymHudOverlay.tick();
            if (client.player == null || client.world == null) {
                lastBenchPressHeld = false;
                lastSquatHeld = false;
                lastDeadliftHeld = false;
                DeadliftClientState.clearSyncedDeadlifts();
                return;
            }

            // Optional integration: register the real elbow-bend layer for every
            // player entity visible on this client. This is required so multiplayer
            // observers also render the bent-elbow squat pose, not just themselves.
            for (AbstractClientPlayerEntity visiblePlayer : client.world.getPlayers()) {
                OptionalPlayerAnimator.ensureRegistered(visiblePlayer);
            }

            boolean onActiveBench = BenchClientState.findActiveBench(client.player) != null;
            boolean benchHeld = onActiveBench && client.options.keyUse.isPressed();
            if (benchHeld != lastBenchPressHeld) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(benchHeld);
                ClientPlayNetworking.send(HardGymMod.BENCH_INPUT_PACKET, buf);
                lastBenchPressHeld = benchHeld;
            }

            boolean onActiveSquat = SquatClientState.findActiveSquat(client.player) != null;
            boolean squatHeld = onActiveSquat && client.options.keyUse.isPressed();
            if (squatHeld != lastSquatHeld) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(squatHeld);
                ClientPlayNetworking.send(HardGymMod.SQUAT_INPUT_PACKET, buf);
                lastSquatHeld = squatHeld;
            }

            boolean onActiveDeadlift = DeadliftClientState.findActiveDeadlift(client.player) != null;
            boolean deadliftHeld = onActiveDeadlift && client.options.keyUse.isPressed();
            if (deadliftHeld != lastDeadliftHeld) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(deadliftHeld);
                ClientPlayNetworking.send(HardGymMod.DEADLIFT_INPUT_PACKET, buf);
                lastDeadliftHeld = deadliftHeld;
            }
        });
    }
}
