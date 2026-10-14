package com.hardgym.client;

import com.hardgym.animation.ExerciseAnimation;
import com.hardgym.item.DumbbellItem;
import com.hardgym.item.DumbbellPairItem;
import com.hardgym.player.DumbbellPlayerState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

/** A full curl follows the same use timer and snapshotted tempo as the server rep. */
public final class DumbbellArmPose {
    private DumbbellArmPose() { }

    public static boolean isCurling(LivingEntity entity) {
        return entity instanceof DumbbellPlayerState && entity.isAlive() && entity.isUsingItem()
                && entity.getActiveItem().getItem() instanceof DumbbellItem
                && ((DumbbellPlayerState) entity).hardgym$getDumbbellTrainingWeight()
                == ((DumbbellItem) entity.getActiveItem().getItem()).getWeightKg();
    }

    public static Arm activeArm(LivingEntity entity) {
        Arm mainArm = entity.getMainArm();
        return entity.getActiveHand() == Hand.MAIN_HAND ? mainArm : mainArm.getOpposite();
    }

    public static boolean usesArm(LivingEntity entity, Arm arm) {
        return isCurling(entity) && (entity.getActiveItem().getItem() instanceof DumbbellPairItem
                || activeArm(entity) == arm);
    }

    /** Returns the one real pair item only when the other hand is available. */
    public static ItemStack heldPair(LivingEntity entity) {
        ItemStack main = entity.getStackInHand(Hand.MAIN_HAND);
        ItemStack off = entity.getStackInHand(Hand.OFF_HAND);
        if (main.getItem() instanceof DumbbellPairItem && off.isEmpty()) return main;
        if (off.getItem() instanceof DumbbellPairItem && main.isEmpty()) return off;
        return ItemStack.EMPTY;
    }

    public static float lift(LivingEntity entity, float tickDelta) {
        ItemStack stack = entity.getActiveItem();
        if (!isCurling(entity)) return 0.0F;
        int ticks = ((DumbbellItem)stack.getItem()).getSessionTicksPerRep(stack);
        float elapsed = entity.getItemUseTime() + Math.max(0.0F, Math.min(1.0F, tickDelta));
        float phase = (elapsed % ticks) / ticks;
        // Lift, briefly squeeze at the top, lower, then settle at the bottom.
        if (phase < 0.40F) return ExerciseAnimation.ease(phase / 0.40F);
        if (phase < 0.50F) return 1.0F;
        if (phase < 0.90F) return 1.0F - ExerciseAnimation.ease((phase - 0.50F) / 0.40F);
        return 0.0F;
    }

    public static float elbowBend(float lift) {
        // About 5 -> 86 degrees: a hammer curl toward this arm's shoulder,
        // with room for the plate above the hand at the top.
        return 0.08F + 1.42F * lift;
    }

    public static float shoulderPitch(float lift, boolean bentElbow) {
        // With BendyLib the upper arm stays beside the torso; only the forearm curls.
        return bentElbow ? -0.06F - 0.02F * lift : -0.08F - 1.00F * lift;
    }

}
