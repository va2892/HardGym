package com.hardgym.item;

import com.hardgym.block.EquipmentRackBlock;
import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import com.hardgym.player.DumbbellPlayerState;
import com.hardgym.player.GymPhysiqueClientState;
import com.hardgym.player.GymStats;
import com.hardgym.player.StrengthDecay;
import com.hardgym.training.BenchPressManager;
import com.hardgym.training.DeadliftManager;
import com.hardgym.training.SquatManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/** Shared training logic; only paired dumbbells are registered as items. */
public abstract class DumbbellItem extends Item {
    private static final String SESSION_REPS_KEY = "HardGymSessionReps";
    private static final String SESSION_TICKS_KEY = "HardGymDumbbellTicksPerRep";
    private static final int MIN_TICKS_PER_REP = 28;

    private final int weightKg;
    private final int requiredBicepsLevel;
    private final int strengthXpPerRep;
    private final int bicepsXpPerRep;
    private final int staminaCost;
    private final int fatiguePerSet;
    private final int baseTicksPerRep;

    protected DumbbellItem(int weightKg,
                        int requiredBicepsLevel,
                        int strengthXpPerRep,
                        int bicepsXpPerRep,
                        int staminaCost,
                        int fatiguePerSet,
                        int baseTicksPerRep,
                        Settings settings) {
        super(settings);
        this.weightKg = weightKg;
        this.requiredBicepsLevel = requiredBicepsLevel;
        this.strengthXpPerRep = strengthXpPerRep;
        this.bicepsXpPerRep = bicepsXpPerRep;
        this.staminaCost = staminaCost;
        this.fatiguePerSet = fatiguePerSet;
        this.baseTicksPerRep = baseTicksPerRep;
    }

    public int getWeightKg() {
        return weightKg;
    }

    public abstract int getDumbbellCount();

    /** The server snapshots the tempo in the held stack, which also reaches observers. */
    public int getSessionTicksPerRep(ItemStack stack) {
        int ticks = stack.hasTag() ? stack.getTag().getInt(SESSION_TICKS_KEY) : 0;
        return ticks > 0 ? Math.max(MIN_TICKS_PER_REP, Math.min(baseTicksPerRep, ticks))
                : baseTicksPerRep;
    }

    private int getFullSetReps() {
        return Math.max(1, 100 / staminaCost);
    }

    private int getFatigueGainForRep(int completedRepsBefore) {
        int target = getFullSetReps();
        long before = Math.max(0, completedRepsBefore);
        // Round cumulative cost UP: short sets still cost FAT, and restarting RMB
        // cannot repeatedly obtain a free first rep by resetting the counter.
        long beforeFat = (before * fatiguePerSet + target - 1) / target;
        long afterFat = ((before + 1) * fatiguePerSet + target - 1) / target;
        return (int)(afterFat - beforeFat);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getWorld().getBlockState(context.getBlockPos()).getBlock()
                instanceof EquipmentRackBlock) {
            EquipmentRackBlock rack = (EquipmentRackBlock) context.getWorld().getBlockState(context.getBlockPos()).getBlock();
            return rack.interact(context.getWorld(), context.getBlockPos(), context.getPlayer(),
                    context.getHand(), context.getHitPos());
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        DumbbellPlayerState curl = (DumbbellPlayerState) user;
        // Local RMB prediction is not permission to animate. The server writes
        // this tracked weight only after validating the actual player data.
        curl.hardgym$setDumbbellTrainingWeight(0);

        if (world.isClient && GymPhysiqueClientState.getBicepsLevel() < requiredBicepsLevel) {
            return TypedActionResult.fail(stack);
        }

        if (getDumbbellCount() == 2 && !user.getStackInHand(
                hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND).isEmpty()) {
            if (!world.isClient) user.sendMessage(new LiteralText("Для пары гантелей освободи вторую руку.")
                    .formatted(Formatting.YELLOW), true);
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            if (BenchPressManager.isTraining(user.getUuid()) || SquatManager.isTraining(user.getUuid())
                    || DeadliftManager.isTraining(user.getUuid())) {
                user.sendMessage(new LiteralText("Сначала закончи текущий подход.").formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }
            GymPlayerData data = (GymPlayerData) user;
            int bicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());

            if (bicepsLevel < requiredBicepsLevel) {
                user.sendMessage(new LiteralText("Слишком тяжело. Нужен Biceps Lv." + requiredBicepsLevel + ".")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
                user.sendMessage(new LiteralText("Ты выдохся. Немного отдохни.")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            stack.getOrCreateTag().putInt(SESSION_REPS_KEY, 0);
            stack.getOrCreateTag().putInt(SESSION_TICKS_KEY, getTicksPerRep(user));
        }

        user.setCurrentHand(hand);
        if (!world.isClient) {
            curl.hardgym$setDumbbellTrainingWeight(weightKg);
            GymPlayerData data = (GymPlayerData) user;
            if (user instanceof ServerPlayerEntity) {
                GymHud.showDumbbell((ServerPlayerEntity) user, weightKg, getDumbbellCount() == 2, 0,
                        data.hardgym$getStamina(), data.hardgym$getFatigue());
            }
        }
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof DumbbellPlayerState) {
            ((DumbbellPlayerState) user).hardgym$setDumbbellTrainingWeight(0);
        }
        if (!world.isClient && user instanceof ServerPlayerEntity) {
            GymHud.showRecoveryOrHide((ServerPlayerEntity) user);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        if (((DumbbellPlayerState) player).hardgym$getDumbbellTrainingWeight() != weightKg) {
            player.stopUsingItem();
            return;
        }
        if (getDumbbellCount() == 2 && !player.getStackInHand(
                player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND).isEmpty()) {
            player.stopUsingItem();
            return;
        }
        int elapsedTicks = getMaxUseTime(stack) - remainingUseTicks;
        int ticksPerRep = getSessionTicksPerRep(stack);

        if (elapsedTicks <= 0 || elapsedTicks % ticksPerRep != 0) {
            return;
        }

        performRep(player, stack);
    }

    private int getTicksPerRep(PlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int bicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());
        int practiceAboveRequirement = Math.max(0, bicepsLevel - requiredBicepsLevel);
        return Math.max(MIN_TICKS_PER_REP, baseTicksPerRep - practiceAboveRequirement * 2);
    }

    private void performRep(ServerPlayerEntity player, ItemStack stack) {
        GymPlayerData data = (GymPlayerData) player;
        int bicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());

        if (bicepsLevel < requiredBicepsLevel) {
            player.sendMessage(new LiteralText("Этот вес пока слишком тяжёлый.").formatted(Formatting.RED), true);
            player.stopUsingItem();
            return;
        }

        if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
            player.sendMessage(new LiteralText("ОТКАЗ! Отдохни перед следующим подходом.")
                    .formatted(Formatting.RED), true);
            player.stopUsingItem();
            return;
        }

        int oldStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int oldBicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());
        int completedRepsBefore = stack.getOrCreateTag().getInt(SESSION_REPS_KEY);

        data.hardgym$setStamina(data.hardgym$getStamina() - staminaCost);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + getFatigueGainForRep(completedRepsBefore));
        data.hardgym$setStrengthXp(data.hardgym$getStrengthXp() + strengthXpPerRep);
        StrengthDecay.markTraining(player);
        data.hardgym$setBicepsXp(data.hardgym$getBicepsXp() + bicepsXpPerRep);
        data.hardgym$setTotalReps(data.hardgym$getTotalReps() + 1);

        // Тренировка дополнительно расходует голод через vanilla exhaustion.
        // Чем тяжелее гантель, тем больше энергии уходит за повтор.
        player.getHungerManager().addExhaustion(dumbbellExhaustion(weightKg));

        int sessionReps = completedRepsBefore + 1;
        stack.getOrCreateTag().putInt(SESSION_REPS_KEY, sessionReps);

        int newStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int newBicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());

        GymHud.showDumbbell(player, weightKg, getDumbbellCount() == 2, sessionReps,
                data.hardgym$getStamina(), data.hardgym$getFatigue());

        if (newStrengthLevel > oldStrengthLevel) {
            player.sendMessage(new LiteralText("HardGym: Strength теперь " + newStrengthLevel + "!")
                    .formatted(Formatting.GOLD), false);
        }

        if (newBicepsLevel > oldBicepsLevel) {
            player.sendMessage(new LiteralText("HardGym: Biceps теперь " + newBicepsLevel + "!")
                    .formatted(Formatting.AQUA), false);
        }

        if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
            player.stopUsingItem();
        }
    }

    private static float dumbbellExhaustion(int weightKg) {
        // Расход за общий повтор: по 10 кг = 0.80, по 20 кг = 1.20 exhaustion.
        return 0.40F + weightKg * 0.04F;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        // The curl is rendered by our model/bend layer, not vanilla bow aiming.
        return UseAction.NONE;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(new LiteralText("2 × " + weightKg + " кг — вес каждой гантели").formatted(Formatting.GRAY));
        tooltip.add(new LiteralText("Упражнение: молотки двумя руками").formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Нужен Biceps Lv." + requiredBicepsLevel).formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Зажми ПКМ, чтобы тренироваться").formatted(Formatting.YELLOW));
        tooltip.add(new LiteralText("Вторая рука должна быть свободна").formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("На стойке занимает 2 места; забирается целой парой").formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Biceps ускоряет повторы").formatted(Formatting.GREEN));
        tooltip.add(new LiteralText(getFullSetReps() + " повторов: +" + fatiguePerSet + "% FAT")
                .formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Biceps увеличивает скорость копания").formatted(Formatting.AQUA));
    }
}
