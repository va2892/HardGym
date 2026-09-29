package com.hardgym.player;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class GymAttributeManager {
    private static final UUID DAMAGE_UUID = UUID.fromString("5b4823d7-3d6a-4d5c-a06a-75cf0b4bb783");
    private static final UUID SPEED_UUID = UUID.fromString("b8a62082-29b0-4d2a-9be9-21901c2af001");
    private static final UUID HEALTH_UUID = UUID.fromString("cb1671a2-b026-45d5-83cc-21901c2af002");

    private GymAttributeManager() { }

    public static void update(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int benchLevel = GymStats.levelForXp(data.hardgym$getBenchXp());
        int squatLevel = GymStats.levelForXp(data.hardgym$getSquatXp());

        // Attribute base values for vanilla players are 1.0 damage, 0.10 speed, 20 health.
        replaceAddition(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DAMAGE_UUID,
                "HardGym Bench Damage", GymPhysicalStats.attackDamageForBenchLevel(benchLevel) - 1.0D);
        replaceAddition(player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), SPEED_UUID,
                "HardGym Squat Speed", GymPhysicalStats.movementSpeedForSquatLevel(squatLevel) - 0.10D);
        replaceAddition(player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), HEALTH_UUID,
                "HardGym Strength Health", GymPhysicalStats.maxHealthForStrengthLevel(strengthLevel) - 20.0D);

        float maxHealth = player.getMaxHealth();
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private static void replaceAddition(EntityAttributeInstance attribute, UUID uuid, String name, double value) {
        if (attribute == null) return;
        EntityAttributeModifier old = attribute.getModifier(uuid);
        if (old != null && Math.abs(old.getValue() - value) < 0.0001D) return;
        if (old != null) attribute.removeModifier(uuid);
        attribute.addTemporaryModifier(new EntityAttributeModifier(uuid, name, value, EntityAttributeModifier.Operation.ADDITION));
    }
}
