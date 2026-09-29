package com.hardgym.player;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class StrengthDamage {
    // Fixed UUID so the modifier is replaced instead of stacking.
    private static final UUID STRENGTH_DAMAGE_UUID = UUID.fromString("5b4823d7-3d6a-4d5c-a06a-75cf0b4bb783");
    private static final String MODIFIER_NAME = "HardGym Strength Damage";

    private StrengthDamage() {
    }

    /**
     * Bare-hand attack damage progression. Vanilla player base damage is 1.0.
     * Lv.1 starts weaker at 0.5 damage. Every Strength level adds 0.2 damage.
     * Examples: Lv.1=0.5, Lv.4=1.1, Lv.10=2.3, Lv.18=3.9.
     */
    public static double bareHandDamageForLevel(int strengthLevel) {
        int level = Math.max(1, strengthLevel);
        return 0.5D + (level - 1) * 0.20D;
    }

    public static double modifierForLevel(int strengthLevel) {
        return bareHandDamageForLevel(strengthLevel) - 1.0D;
    }

    public static void update(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int level = GymStats.levelForXp(data.hardgym$getStrengthXp());
        double wanted = modifierForLevel(level);

        EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }

        EntityAttributeModifier old = attackDamage.getModifier(STRENGTH_DAMAGE_UUID);
        if (old != null && Math.abs(old.getValue() - wanted) < 0.0001D) {
            return;
        }

        if (old != null) {
            attackDamage.removeModifier(STRENGTH_DAMAGE_UUID);
        }

        attackDamage.addTemporaryModifier(new EntityAttributeModifier(
                STRENGTH_DAMAGE_UUID,
                MODIFIER_NAME,
                wanted,
                EntityAttributeModifier.Operation.ADDITION
        ));
    }
}
