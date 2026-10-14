package com.hardgym.item;

import com.hardgym.HardGymMod;
import com.hardgym.block.BarbellStationBlock;
import com.hardgym.block.EquipmentRackBlock;
import com.hardgym.player.BarbellLoad;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** One item installs one plate on each side of a bar. */
public final class PlatePairItem extends Item {
    private final int plateWeightKg;

    public PlatePairItem(int plateWeightKg, Settings settings) {
        super(settings);
        if (!BarbellLoad.isPlateWeight(plateWeightKg)) throw new IllegalArgumentException("Invalid plate weight");
        this.plateWeightKg = plateWeightKg;
    }

    public int getPlateWeightKg() {
        return plateWeightKg;
    }

    public static Item forWeight(int weightKg) {
        switch (weightKg) {
            case 10: return HardGymMod.PLATE_PAIR_10KG;
            case 15: return HardGymMod.PLATE_PAIR_15KG;
            case 20: return HardGymMod.PLATE_PAIR_20KG;
            case 25: return HardGymMod.PLATE_PAIR_25KG;
            case 50: return HardGymMod.PLATE_PAIR_50KG;
            default: throw new IllegalArgumentException("Invalid plate weight");
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Block block = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
        if (block instanceof EquipmentRackBlock && context.getPlayer() != null) {
            return ((EquipmentRackBlock) block).interact(context.getWorld(), context.getBlockPos(),
                    context.getPlayer(), context.getHand(), context.getHitPos());
        }
        if (!(block instanceof BarbellStationBlock) || context.getPlayer() == null) return ActionResult.PASS;
        // Sneaking skips vanilla block interaction; route item use through the same server logic.
        return ((BarbellStationBlock) block).interact(context.getWorld(), context.getBlockPos(),
                context.getPlayer(), context.getHand());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(new TranslatableText("tooltip.hardgym.plate_pair", plateWeightKg, 2 * plateWeightKg)
                .formatted(Formatting.GRAY));
        tooltip.add(new TranslatableText("tooltip.hardgym.plate_pair_use").formatted(Formatting.YELLOW));
    }
}
