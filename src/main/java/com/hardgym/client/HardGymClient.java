package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.player.GymPhysiqueClientState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class HardGymClient implements ClientModInitializer {
    private boolean lastBenchPressHeld = false;
    private boolean lastSquatHeld = false;
    private boolean lastDeadliftHeld = false;
    private ClientWorld animationWorld;
    private AbstractClientPlayerEntity animationPlayer;

    @Override
    public void onInitializeClient() {
        EquipmentRackOutlineRenderer.register();
        BlockEntityRendererRegistry.INSTANCE.register(HardGymMod.BARBELL_BLOCK_ENTITY, BarbellBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(HardGymMod.EQUIPMENT_RACK_BLOCK_ENTITY, EquipmentRackBlockEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(HardGymMod.PHYSIQUE_SYNC_PACKET,
                (client, handler, buf, responseSender) -> {
                    int strength = buf.readInt();
                    int bench = buf.readInt();
                    int squat = buf.readInt();
                    int deadlift = buf.readInt();
                    int biceps = buf.readInt();
                    client.execute(() -> GymPhysiqueClientState.update(strength, bench, squat, deadlift, biceps));
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
                OptionalPlayerAnimator.clearRegistrations();
                animationWorld = null;
                animationPlayer = null;
                GymPhysiqueClientState.reset();
                return;
            }

            if (animationWorld != client.world || animationPlayer != client.player) {
                OptionalPlayerAnimator.clearRegistrations();
                animationWorld = client.world;
                animationPlayer = client.player;
                lastBenchPressHeld = false;
                lastSquatHeld = false;
                lastDeadliftHeld = false;
            }
            // Restore lost/replaced bend layers for local and remote players.
            for (AbstractClientPlayerEntity visiblePlayer : client.world.getPlayers()) {
                if (visiblePlayer != client.player) OptionalPlayerAnimator.ensureRegistered(visiblePlayer);
            }
            // The local avatar can be replaced before the world's player list catches up.
            OptionalPlayerAnimator.ensureRegistered(client.player);

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
