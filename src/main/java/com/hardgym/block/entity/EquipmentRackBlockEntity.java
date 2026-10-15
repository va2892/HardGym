package com.hardgym.block.entity;

import com.hardgym.HardGymMod;
import com.hardgym.block.EquipmentRackBlock;
import com.hardgym.block.EquipmentRackLayout;
import com.hardgym.item.PlatePairItem;
import com.hardgym.item.DumbbellPairItem;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class EquipmentRackBlockEntity extends BlockEntity implements Inventory, BlockEntityClientSerializable, Tickable {
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(6, ItemStack.EMPTY);
    private static final String PARTNERS_KEY = "DumbbellPairPartners";
    private final int[] partners = new int[6];
    private static final String CROSS_PARTNERS_KEY = "DumbbellCrossPartners";
    private static final String CROSS_DISPLAYS_KEY = "DumbbellCrossDisplays";
    private final Link[] crossPartners = new Link[6];
    private final Display[] crossDisplays = new Display[6];
    private int maintenanceTicks;

    private static final class Link {
        final BlockPos rack;
        final int slot;
        final String id;
        Link(BlockPos rack, int slot, String id) {
            this.rack = rack.toImmutable(); this.slot = slot; this.id = id;
        }
        boolean matches(BlockPos rack, int slot, String id) {
            return this.rack.equals(rack) && this.slot == slot && this.id.equals(id);
        }
    }

    private static final class Display {
        final Link owner;
        ItemStack stack;
        Display(Link owner, ItemStack stack) { this.owner = owner; this.stack = stack.copy(); }
    }

    private static final class Place {
        final EquipmentRackBlockEntity rack;
        final int slot;
        Place(EquipmentRackBlockEntity rack, int slot) { this.rack = rack; this.slot = slot; }
    }

    public EquipmentRackBlockEntity() {
        super(HardGymMod.EQUIPMENT_RACK_BLOCK_ENTITY);
        Arrays.fill(partners, -1);
    }

    public boolean isPlateRack() {
        BlockState state = getCachedState();
        return state != null && state.getBlock() instanceof EquipmentRackBlock && ((EquipmentRackBlock) state.getBlock()).isPlateRack();
    }

    @Override
    public int size() {
        return isPlateRack() ? 5 : 6;
    }

    @Override
    public int getMaxCountPerStack() {
        return isPlateRack() ? EquipmentRackLayout.PAIRS_PER_PEG : 1;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size() || stack.isEmpty()) return false;
        BlockState state = getCachedState();
        if (state == null || !(state.getBlock() instanceof EquipmentRackBlock) || !((EquipmentRackBlock) state.getBlock()).accepts(stack.getItem())) return false;
        if (isPlateRack()) return ((PlatePairItem) stack.getItem()).getPlateWeightKg() == EquipmentRackLayout.plateWeight(slot);
        if (ownerOf(slot) != slot || crossDisplays[slot] != null) return false;
        return !isPair(stack) || findPartner(new Place(this, slot), connectedRacks(), false) != null;
    }

    public int insert(ItemStack held, int preferredSlot, int requested) {
        if (held.isEmpty() || requested <= 0) return 0;
        if (!isPlateRack() && held.getItem() instanceof DumbbellPairItem) {
            return insertPair(held, preferredSlot);
        }
        int remaining = Math.min(requested, held.getCount());
        int inserted = 0;
        int first = isPlateRack() && held.getItem() instanceof PlatePairItem
                ? EquipmentRackLayout.plateSlot(((PlatePairItem) held.getItem()).getPlateWeightKg())
                : Math.max(0, Math.min(size() - 1, preferredSlot));
        for (int offset = 0; offset < (isPlateRack() ? 1 : size()) && remaining > 0; offset++) {
            int slot = (first + offset) % size();
            if (!isValid(slot, held)) continue;
            ItemStack stored = stacks.get(slot);
            if (!stored.isEmpty() && (!ItemStack.areItemsEqual(stored, held) || !ItemStack.areTagsEqual(stored, held))) continue;
            int room = Math.min(getMaxCountPerStack(), held.getMaxCount()) - stored.getCount();
            int moved = Math.min(remaining, Math.max(0, room));
            if (moved == 0) continue;
            if (stored.isEmpty()) stacks.set(slot, held.split(moved));
            else {
                stored.increment(moved);
                held.decrement(moved);
            }
            inserted += moved;
            remaining -= moved;
        }
        if (inserted > 0) markDirty();
        return inserted;
    }

    public int storedCount() {
        int total = 0;
        for (int slot = 0; slot < size(); slot++) total += getStack(slot).getCount();
        return total;
    }

    public int occupiedDumbbellSlots() {
        int total = 0;
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (!getDisplayedStack(slot).isEmpty()) total++;
        }
        return total;
    }

    public ItemStack getDisplayedStack(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        Display display = crossDisplays[slot];
        return display == null ? stacks.get(ownerOf(slot)) : display.stack;
    }

    public boolean hasDisplayedEquipment() {
        return isPlateRack() ? !isEmpty() : occupiedDumbbellSlots() > 0;
    }

    public int sectionCapacity() { return connectedRacks().size() * 6; }

    public int sectionOccupiedSlots() {
        int count = 0;
        for (EquipmentRackBlockEntity rack : connectedRacks()) count += rack.occupiedDumbbellSlots();
        return count;
    }

    private Direction rightDirection() {
        return getCachedState().get(EquipmentRackBlock.FACING).rotateYClockwise();
    }

    private EquipmentRackBlockEntity loadedRack(BlockPos location) {
        EquipmentRackBlockEntity rack = entityAt(location);
        if (rack == null) return null;
        BlockState a = getCachedState(), b = rack.getCachedState();
        return EquipmentRackBlock.connects(a, b) ? rack : null;
    }

    private EquipmentRackBlockEntity entityAt(BlockPos location) {
        if (world == null || !world.isChunkLoaded(location)) return null;
        BlockEntity entity = world.getBlockEntity(location);
        return entity instanceof EquipmentRackBlockEntity ? (EquipmentRackBlockEntity) entity : null;
    }

    private List<EquipmentRackBlockEntity> connectedRacks() {
        List<EquipmentRackBlockEntity> row = new ArrayList<>();
        row.add(this);
        if (world == null || isPlateRack()) return row;
        Direction right = rightDirection();
        BlockPos cursor = pos;
        EquipmentRackBlockEntity next;
        while ((next = loadedRack(cursor.offset(right.getOpposite()))) != null) {
            row.add(0, next); cursor = next.pos;
        }
        cursor = pos;
        while ((next = loadedRack(cursor.offset(right))) != null) {
            row.add(next); cursor = next.pos;
        }
        return row;
    }

    private int connectionStatus(BlockPos other) {
        Direction right = rightDirection();
        int dx = other.getX() - pos.getX(), dz = other.getZ() - pos.getZ();
        int distance = dx * right.getOffsetX() + dz * right.getOffsetZ();
        if (other.getY() != pos.getY() || dx != distance * right.getOffsetX() || dz != distance * right.getOffsetZ()) return 0;
        Direction step = distance < 0 ? right.getOpposite() : right;
        BlockPos cursor = pos;
        for (int i = 0; i < Math.abs(distance); i++) {
            cursor = cursor.offset(step);
            if (!world.isChunkLoaded(cursor)) return -1;
            if (!EquipmentRackBlock.connects(getCachedState(), world.getBlockState(cursor))) return 0;
        }
        return 1;
    }

    private int ownerOf(int slot) {
        for (int owner = 0; owner < partners.length; owner++) {
            if (partners[owner] == slot && isPair(stacks.get(owner))) return owner;
        }
        return slot;
    }

    private static boolean isPair(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof DumbbellPairItem;
    }

    private int findPartner(int primary) {
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int candidate = 0; candidate < stacks.size(); candidate++) {
            if (candidate == primary || !stacks.get(candidate).isEmpty() || crossDisplays[candidate] != null) continue;
            int owner = ownerOf(candidate);
            if (owner != candidate && owner != primary) continue;
            int dx = primary % 3 - candidate % 3;
            int dy = primary / 3 - candidate / 3;
            int distance = dx * dx + dy * dy * 4;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean availableFor(int slot, Place primary) {
        if (!stacks.get(slot).isEmpty()) return false;
        int localOwner = ownerOf(slot);
        if (localOwner != slot && (primary.rack != this || localOwner != primary.slot)) return false;
        Display display = crossDisplays[slot];
        Link link = primary.rack.crossPartners[primary.slot];
        return display == null || link != null && display.owner.matches(primary.rack.pos, primary.slot, link.id);
    }

    private Place findPartner(Place primary, List<EquipmentRackBlockEntity> row, boolean sameShelf) {
        Place best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int origin = row.indexOf(primary.rack);
        if (origin < 0) return null;
        for (int block = 0; block < row.size(); block++) {
            EquipmentRackBlockEntity rack = row.get(block);
            for (int slot = 0; slot < 6; slot++) {
                if (rack == primary.rack && slot == primary.slot || sameShelf && slot / 3 != primary.slot / 3 || !rack.availableFor(slot, primary)) continue;
                double dx = (block - origin) * 16.0D + (slot % 3 - primary.slot % 3) * 5.0D;
                double dy = (slot / 3 - primary.slot / 3) * 7.0D;
                double distance = dx * dx + (dy == 0 ? 0 : 1.0E12D);
                if (distance < bestDistance) { best = new Place(rack, slot); bestDistance = distance; }
            }
        }
        return best;
    }

    private void reservePartner(int primary, Place partner) {
        if (partner.rack == this) partners[primary] = partner.slot;
        else {
            String id = UUID.randomUUID().toString();
            crossPartners[primary] = new Link(partner.rack.pos, partner.slot, id);
            partner.rack.crossDisplays[partner.slot] = new Display(new Link(pos, primary, id), stacks.get(primary));
            partner.rack.markDirty();
        }
    }

    private void releasePartner(int primary) {
        partners[primary] = -1;
        Link link = crossPartners[primary];
        crossPartners[primary] = null;
        if (link == null) return;
        EquipmentRackBlockEntity target = entityAt(link.rack);
        if (target == null) return;
        Display display = target.crossDisplays[link.slot];
        if (display != null && display.owner.matches(pos, primary, link.id)) {
            target.crossDisplays[link.slot] = null; target.markDirty();
        }
    }

    private int insertPair(ItemStack held, int preferredSlot) {
        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof EquipmentRackBlock) || !((EquipmentRackBlock) state.getBlock()).accepts(held.getItem())) return 0;
        List<EquipmentRackBlockEntity> row = connectedRacks();
        for (EquipmentRackBlockEntity rack : row) rack.reconcileLinks();
        int first = Math.max(0, Math.min(5, preferredSlot));
        int width = row.size() * 3, start = row.indexOf(this) * 3;
        for (int pass = 0; pass < 3; pass++) {
            int shelf = pass == 1 ? 1 - first / 3 : first / 3;
            for (int offset = 0; offset < width; offset++) {
                int column = (start + offset) % width;
                EquipmentRackBlockEntity rack = row.get(column / 3);
                int slot = shelf * 3 + column % 3;
                Place primary = new Place(rack, slot);
                if (!rack.availableFor(slot, primary)) continue;
                Place partner = rack.findPartner(primary, row, pass < 2);
                if (partner == null) continue;
                rack.stacks.set(slot, held.split(1));
                rack.reservePartner(slot, partner);
                rack.markDirty();
                return 1;
            }
        }
        return 0;
    }

    public List<ItemStack> removeAll() {
        List<ItemStack> removed = new ArrayList<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = removeStack(slot);
            if (!stack.isEmpty()) removed.add(stack);
        }
        return removed;
    }

    public List<ItemStack> removeSection() {
        List<ItemStack> removed = new ArrayList<>();
        for (EquipmentRackBlockEntity rack : connectedRacks()) removed.addAll(rack.removeAll());
        return removed;
    }

    @Override
    public boolean isEmpty() {
        return storedCount() == 0;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot < 0 || slot >= size() ? ItemStack.EMPTY : stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= size() || amount <= 0) return ItemStack.EMPTY;
        Display display = crossDisplays[slot];
        if (display != null) {
            EquipmentRackBlockEntity owner = entityAt(display.owner.rack);
            if (owner == null) return ItemStack.EMPTY;
            Link partner = owner.crossPartners[display.owner.slot];
            if (partner == null || !partner.matches(pos, slot, display.owner.id)) {
                crossDisplays[slot] = null; markDirty(); return ItemStack.EMPTY;
            }
            return owner.removeStack(display.owner.slot, amount);
        }
        slot = ownerOf(slot);
        ItemStack removed = Inventories.splitStack(stacks, slot, amount);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return removeStack(slot, Integer.MAX_VALUE);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size() || ownerOf(slot) != slot || crossDisplays[slot] != null || (!stack.isEmpty() && !isValid(slot, stack))) return;
        Place partner = isPair(stack) ? findPartner(new Place(this, slot), connectedRacks(), false) : null;
        if (isPair(stack) && partner == null) return;
        if (!stack.isEmpty()) stack.setCount(Math.min(stack.getCount(), Math.min(getMaxCountPerStack(), stack.getMaxCount())));
        releasePartner(slot);
        stacks.set(slot, stack);
        if (partner != null) reservePartner(slot, partner);
        markDirty();
    }

    @Override
    public void clear() {
        removeAll();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void markDirty() {
        for (int slot = 0; slot < partners.length; slot++) {
            if (!isPair(stacks.get(slot))) releasePartner(slot);
        }
        super.markDirty();
        if (world != null && !world.isClient) sync();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || ++maintenanceTicks % 20 != 1 || isPlateRack()) return;
        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof EquipmentRackBlock)) return;
        BlockState connected = ((EquipmentRackBlock) state.getBlock()).withConnections(state, world, pos);
        if (!connected.equals(state)) world.setBlockState(pos, connected, 3);
        reconcileLinks();
    }

    private void reconcileLinks() {
        if (world == null || world.isClient || isPlateRack()) return;
        boolean changed = false;
        for (int slot = 0; slot < 6; slot++) {
            Display display = crossDisplays[slot];
            if (display == null || !world.isChunkLoaded(display.owner.rack)) continue;
            int connection = connectionStatus(display.owner.rack);
            if (connection < 0) continue;
            EquipmentRackBlockEntity owner = loadedRack(display.owner.rack);
            if (owner == null && connection == 1) continue;
            Link link = owner == null ? null : owner.crossPartners[display.owner.slot];
            if (connection == 0 || link == null || !link.matches(pos, slot, display.owner.id) || !isPair(owner.stacks.get(display.owner.slot))) {
                crossDisplays[slot] = null; changed = true;
            } else if (!ItemStack.areItemsEqual(display.stack, owner.stacks.get(display.owner.slot)) || !ItemStack.areTagsEqual(display.stack, owner.stacks.get(display.owner.slot))) {
                display.stack = owner.stacks.get(display.owner.slot).copy(); changed = true;
            }
        }
        List<EquipmentRackBlockEntity> row = null;
        for (int slot = 0; slot < 6; slot++) {
            if (!isPair(stacks.get(slot))) continue;
            Link link = crossPartners[slot];
            if (link != null) {
                int connection = connectionStatus(link.rack);
                if (connection < 0) continue;
                EquipmentRackBlockEntity target = loadedRack(link.rack);
                if (target == null && connection == 1) continue;
                Place primary = new Place(this, slot);
                if (connection == 1 && target.availableFor(link.slot, primary)) {
                    Display display = target.crossDisplays[link.slot];
                    if (display == null) {
                        target.crossDisplays[link.slot] = new Display(new Link(pos, slot, link.id), stacks.get(slot));
                        target.markDirty();
                    }
                    continue;
                }
                releasePartner(slot); changed = true;
            }
            int partner = partners[slot];
            if (partner >= 0 && partner / 3 == slot / 3) continue;
            if (row == null) row = connectedRacks();
            Place better = findPartner(new Place(this, slot), row, partner >= 0);
            if (better == null || better.rack == this && better.slot == partner) continue;
            releasePartner(slot); reservePartner(slot, better); changed = true;
        }
        if (changed) markDirty();
    }

    private void readItems(NbtCompound tag) {
        stacks.clear();
        Inventories.readNbt(tag, stacks);
        Arrays.fill(partners, -1);
        Arrays.fill(crossPartners, null);
        Arrays.fill(crossDisplays, null);
        NbtList links = tag.getList(CROSS_PARTNERS_KEY, 10);
        for (int i = 0; i < links.size(); i++) {
            NbtCompound record = links.getCompound(i);
            int slot = record.getInt("Slot");
            Link link = readLink(record);
            if (slot >= 0 && slot < 6 && isPair(stacks.get(slot)) && link != null && !link.rack.equals(pos)) crossPartners[slot] = link;
        }
        int[] savedPartners = tag.getIntArray(PARTNERS_KEY);
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (!isPair(stacks.get(slot)) || crossPartners[slot] != null) continue;
            int partner = slot < savedPartners.length ? savedPartners[slot] : -1;
            if (partner < 0 || partner >= stacks.size() || partner == slot || !stacks.get(partner).isEmpty() || ownerOf(partner) != partner) {
                partner = findPartner(slot);
            }
            partners[slot] = partner;
        }
        NbtList displays = tag.getList(CROSS_DISPLAYS_KEY, 10);
        for (int i = 0; i < displays.size(); i++) {
            NbtCompound record = displays.getCompound(i);
            int slot = record.getInt("Slot");
            Link owner = readLink(record);
            ItemStack display = ItemStack.fromNbt(record.getCompound("Display"));
            if (slot >= 0 && slot < 6 && stacks.get(slot).isEmpty() && ownerOf(slot) == slot && owner != null && !owner.rack.equals(pos) && isPair(display)) {
                display.setCount(1); crossDisplays[slot] = new Display(owner, display);
            }
        }
    }

    private static Link readLink(NbtCompound record) {
        int slot = record.getInt("OtherSlot");
        String id = record.getString("LinkId");
        return slot < 0 || slot >= 6 || id.isEmpty() ? null : new Link(
                new BlockPos(record.getInt("OtherX"), record.getInt("OtherY"), record.getInt("OtherZ")), slot, id);
    }

    private static NbtCompound writeLink(int slot, Link link) {
        NbtCompound record = new NbtCompound();
        record.putInt("Slot", slot); record.putInt("OtherSlot", link.slot);
        record.putInt("OtherX", link.rack.getX()); record.putInt("OtherY", link.rack.getY());
        record.putInt("OtherZ", link.rack.getZ()); record.putString("LinkId", link.id);
        return record;
    }

    private NbtCompound writeItems(NbtCompound tag) {
        Inventories.writeNbt(tag, stacks);
        tag.putIntArray(PARTNERS_KEY, partners);
        NbtList links = new NbtList(), displays = new NbtList();
        for (int slot = 0; slot < 6; slot++) {
            if (crossPartners[slot] != null) links.add(writeLink(slot, crossPartners[slot]));
            if (crossDisplays[slot] != null) {
                NbtCompound record = writeLink(slot, crossDisplays[slot].owner);
                record.put("Display", crossDisplays[slot].stack.writeNbt(new NbtCompound()));
                displays.add(record);
            }
        }
        tag.put(CROSS_PARTNERS_KEY, links); tag.put(CROSS_DISPLAYS_KEY, displays);
        return tag;
    }

    @Override
    public void fromTag(BlockState state, NbtCompound tag) {
        super.fromTag(state, tag);
        readItems(tag);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        return writeItems(tag);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return writeNbt(new NbtCompound());
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        return writeItems(tag);
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        readItems(tag);
    }
}
