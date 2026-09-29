package com.hardgym.item;

import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class DumbbellItem extends Item {
    private static final String SESSION_REPS_KEY = "HardGymSessionReps";

    private final int weightKg;
    private final int requiredStrength;
    private final int strengthXpPerRep;
    private final int bicepsXpPerRep;
    private final int staminaCost;
    private final int fatigueGain;
    private final int baseTicksPerRep;

    public DumbbellItem(int weightKg,
                        int requiredStrength,
                        int strengthXpPerRep,
                        int bicepsXpPerRep,
                        int staminaCost,
                        int fatigueGain,
                        int baseTicksPerRep,
                        Settings settings) {
        super(settings);
        this.weightKg = weightKg;
        this.requiredStrength = requiredStrength;
        this.strengthXpPerRep = strengthXpPerRep;
        this.bicepsXpPerRep = bicepsXpPerRep;
        this.staminaCost = staminaCost;
        this.fatigueGain = fatigueGain;
        this.baseTicksPerRep = baseTicksPerRep;
    }

    public int getWeightKg() {
        return weightKg;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (BenchPressManager.isTraining(user.getUuid()) || SquatManager.isTraining(user.getUuid())
                    || DeadliftManager.isTraining(user.getUuid())) {
                user.sendMessage(new LiteralText("Сначала закончи текущий подход.").formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }
            GymPlayerData data = (GymPlayerData) user;
            int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());

            if (strengthLevel < requiredStrength) {
                user.sendMessage(new LiteralText("Слишком тяжело. Нужен Strength " + requiredStrength + ".")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
                user.sendMessage(new LiteralText("Ты выдохся. Немного отдохни.")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            stack.getOrCreateTag().putInt(SESSION_REPS_KEY, 0);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        int elapsedTicks = getMaxUseTime(stack) - remainingUseTicks;
        int ticksPerRep = getTicksPerRep(player);

        if (elapsedTicks <= 0 || elapsedTicks % ticksPerRep != 0) {
            return;
        }

        performRep(player, stack);
    }

    private int getTicksPerRep(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int strengthAboveRequirement = Math.max(0, strengthLevel - requiredStrength);
        return Math.max(10, baseTicksPerRep - strengthAboveRequirement * 2);
    }

    private void performRep(ServerPlayerEntity player, ItemStack stack) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());

        if (strengthLevel < requiredStrength) {
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

        data.hardgym$setStamina(data.hardgym$getStamina() - staminaCost);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + fatigueGain);
        data.hardgym$setStrengthXp(data.hardgym$getStrengthXp() + strengthXpPerRep);
        StrengthDecay.markTraining(player);
        data.hardgym$setBicepsXp(data.hardgym$getBicepsXp() + bicepsXpPerRep);
        data.hardgym$setTotalReps(data.hardgym$getTotalReps() + 1);

        // Тренировка дополнительно расходует голод через vanilla exhaustion.
        // Чем тяжелее гантель, тем больше энергии уходит за повтор.
        player.getHungerManager().addExhaustion(dumbbellExhaustion(weightKg));

        int sessionReps = stack.getOrCreateTag().getInt(SESSION_REPS_KEY) + 1;
        stack.getOrCreateTag().putInt(SESSION_REPS_KEY, sessionReps);

        int newStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int newBicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());

        GymHud.showDumbbell(player, weightKg, sessionReps,
                data.hardgym$getStamina(), data.hardgym$getFatigue());

        if (newStrengthLevel > oldStrengthLevel) {
            player.sendMessage(new LiteralText("HardGym: Strength теперь " + newStrengthLevel + "!")
                    .formatted(Formatting.GOLD), false);
        }

        if (newBicepsLevel > oldBicepsLevel) {
            player.sendMessage(new LiteralText("HardGym: Biceps теперь " + newBicepsLevel + "!")
                    .formatted(Formatting.AQUA), false);
        }
    }

    private static float dumbbellExhaustion(int weightKg) {
        // Усиленный расход: 5 кг = 0.60, 10 кг = 0.80, 20 кг = 1.20 exhaustion за повтор.
        return 0.40F + weightKg * 0.04F;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(new LiteralText(weightKg + " кг").formatted(Formatting.GRAY));
        tooltip.add(new LiteralText("Упражнение: подъём на бицепс").formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Нужен Strength " + requiredStrength).formatted(Formatting.DARK_GRAY));
        tooltip.add(new LiteralText("Зажми ПКМ, чтобы тренироваться").formatted(Formatting.YELLOW));
        tooltip.add(new LiteralText("Чем ты сильнее, тем быстрее повтор").formatted(Formatting.GREEN));
    }
}
