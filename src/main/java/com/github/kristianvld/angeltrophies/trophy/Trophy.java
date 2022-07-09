package com.github.kristianvld.angeltrophies.trophy;

import com.github.kristianvld.angeltrophies.Main;
import com.github.kristianvld.angeltrophies.couch.CouchRole;
import com.github.kristianvld.angeltrophies.couch.CouchUtil;
import com.github.kristianvld.angeltrophies.util.BlockVectorTagType;
import com.github.kristianvld.angeltrophies.util.UUIDTagType;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

public class Trophy {

    public final static Material SLAB_TYPE = Material.BIRCH_SLAB;

    static NamespacedKey OWNER_KEY;
    static NamespacedKey DIRECTION_KEY;
    static NamespacedKey SEAT_KEY;
    static NamespacedKey TROPHY_PARENT_KEY;
    static NamespacedKey SEAT_BLOCK_KEY;

    static NamespacedKey COUCH_GROUP;
    static NamespacedKey COUCH_ROLE;

    private final String itemName;
    private final ItemStack exampleItem;

    private final boolean floor;
    private final boolean floorSmall;
    private final double floorOffset;
    private final boolean floorPlaceSlab;
    private final float floorRotationResolution;

    private final boolean wall;
    private final boolean wallSmall;
    private final double wallOffset;

    private final String couchGroup;
    private final CouchRole couchRole;

    public Trophy(String item, boolean floor, boolean floorSmall, double floorOffset, boolean wall, boolean wallSmall, double wallOffset, boolean floorPlaceSlab, float floorRotationResolution, String couchGroup, CouchRole couchRole) {
        itemName = item;
        exampleItem = OraxenItems.exists(item) ? OraxenItems.getItemById(item).build() : null;
        if (exampleItem == null || exampleItem.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid Oraxen item provided for trophy '" + item + "'.");
        }
        this.floor = floor;
        this.floorSmall = floorSmall;
        this.floorOffset = floorOffset;
        this.wall = wall;
        this.wallSmall = wallSmall;
        this.wallOffset = wallOffset;
        this.floorPlaceSlab = floorPlaceSlab;
        this.floorRotationResolution = floorRotationResolution;
        this.couchGroup = couchGroup;
        this.couchRole = couchRole;
        if ((couchGroup == null || couchRole == null) && (couchGroup != null || couchRole != null)) {
            throw new IllegalArgumentException("Both CouchGroup and CouchRole needs to be define, not just one for the trophy '" + item + "'.");
        }
    }

    public String getName() {
        return itemName;
    }

    public String getCouchGroup() {
        return couchGroup;
    }

    public CouchRole getCouchRole() {
        return couchRole;
    }

    public ItemStack getExampleItem() {
        return exampleItem.clone();
    }

    public ArmorStand place(UUID owner, ItemStack itemstack, Block block, boolean small, BlockFace blockFace, float yaw, Vector offset, boolean placeSlab) {
        for (Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof Hanging) {
                if (((Hanging) e).getAttachedFace() == blockFace) {
                    return null;
                }
            }
            if (blockFace == getFace(e)) {
                return null;
            }
        }
        if (placeSlab && block.getType() != Material.AIR) {
            return null;
        }
        Location loc = block.getLocation().add(0.5, 0, 0.5).add(offset);
        loc.setYaw(yaw);
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.getEquipment().setHelmet(itemstack);
        stand.setInvulnerable(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCollidable(false);
        stand.setSmall(small);
        stand.setMarker(false); // turn to true to disable hitbox, would be hard to detect pickup
        stand.getPersistentDataContainer().set(OWNER_KEY, UUIDTagType.UUID, owner);
        stand.getPersistentDataContainer().set(DIRECTION_KEY, UUIDTagType.INTEGER, blockFace.ordinal());
        loc.getWorld().playSound(loc, Sound.ENTITY_ARMOR_STAND_PLACE, 0.7f, 0.7f);
        if (placeSlab) {
            block.setType(SLAB_TYPE);
            ArmorStand seat = loc.getWorld().spawn(loc.clone().add(0, 0.3, 0), ArmorStand.class);
            seat.setMarker(true);
            seat.setGravity(false);
            seat.setInvulnerable(true);
            seat.setVisible(false);

            seat.getPersistentDataContainer().set(TROPHY_PARENT_KEY, UUIDTagType.UUID, stand.getUniqueId());
            stand.getPersistentDataContainer().set(SEAT_KEY, UUIDTagType.UUID, seat.getUniqueId());

            BlockVector blockLoc = loc.toVector().toBlockVector();
            seat.getPersistentDataContainer().set(SEAT_BLOCK_KEY, BlockVectorTagType.BLOCK_VECTOR, blockLoc);
            stand.getPersistentDataContainer().set(SEAT_BLOCK_KEY, BlockVectorTagType.BLOCK_VECTOR, blockLoc);

            if (couchGroup != null && couchRole != null) {
                stand.getPersistentDataContainer().set(COUCH_GROUP, UUIDTagType.STRING, couchGroup);
                stand.getPersistentDataContainer().set(COUCH_ROLE, UUIDTagType.STRING, couchRole.name());
            }
        }
        return stand;
    }

    public ArmorStand place(Player player, Block block, BlockFace face, EquipmentSlot hand, ItemStack item) {
        Vector offset;
        boolean small;
        float yaw;
        boolean placeSlab = false;
        if (face == BlockFace.DOWN && floor) {
            offset = face.getDirection().multiply(-floorOffset);
            small = floorSmall;
            yaw = 180 + Math.round(player.getLocation().getYaw() / floorRotationResolution) * floorRotationResolution;
            placeSlab = floorPlaceSlab;
        } else if (wall && (face == BlockFace.EAST || face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.WEST)) {
            offset = face.getDirection().multiply(wallOffset);
            small = wallSmall;
            yaw = (float) Math.toDegrees(face.getDirection().angle(BlockFace.SOUTH.getDirection()));
        } else {
            return null;
        }
        ItemStack trophy = item.clone();
        trophy.setAmount(1);
        ArmorStand stand = place(player.getUniqueId(), trophy, block, small, face, yaw, offset, placeSlab);
        if (stand != null) {
            item = player.getEquipment().getItem(hand);
            item.setAmount(item.getAmount() - 1);
            item = item.getAmount() > 0 ? item : null;
            player.getEquipment().setItem(hand, item);
        }
        return stand;
    }

    public boolean matches(ItemStack item) {
        return itemName.equals(OraxenItems.getIdByItem(item));
    }

    public static boolean isTrophy(Entity entity) {
        return entity instanceof ArmorStand
                && entity.isValid()
                && entity.getPersistentDataContainer().has(OWNER_KEY, UUIDTagType.UUID);
    }

    public static BlockFace getFace(Entity entity) {
        return isTrophy(entity) ? BlockFace.values()[entity.getPersistentDataContainer().get(DIRECTION_KEY, UUIDTagType.INTEGER)] : null;
    }

    public static boolean isSeat(Entity entity) {
        return entity instanceof ArmorStand
                && entity.isValid()
                && entity.getPersistentDataContainer().has(TROPHY_PARENT_KEY, UUIDTagType.UUID);
    }

    public static Entity getSeat(Entity trophy) {
        if (isSeat(trophy)) {
            return trophy;
        }
        return getEntity(trophy, SEAT_KEY);
    }

    public static Entity getTrophy(Entity seat) {
        if (isTrophy(seat)) {
            return seat;
        }
        return getEntity(seat, TROPHY_PARENT_KEY);
    }

    private static Entity getEntity(Entity seat, NamespacedKey trophyParentKey) {
        if (seat instanceof ArmorStand
                && seat.isValid()
                && seat.getPersistentDataContainer().has(trophyParentKey, UUIDTagType.UUID)) {
            UUID trophy = seat.getPersistentDataContainer().get(trophyParentKey, UUIDTagType.UUID);
            for (Entity e : seat.getLocation().getChunk().getEntities()) {
                if (e.getUniqueId().equals(trophy)) {
                    return e;
                }
            }
        }
        return null;
    }

    public static BlockVector getBlockVector(Entity trophy) {
        if (trophy instanceof ArmorStand
                && trophy.isValid()
                && trophy.getPersistentDataContainer().has(SEAT_BLOCK_KEY, BlockVectorTagType.BLOCK_VECTOR)) {
            return trophy.getPersistentDataContainer().get(SEAT_BLOCK_KEY, BlockVectorTagType.BLOCK_VECTOR);
        }
        return null;
    }

    public static Entity getTrophy(Block block) {
        BlockVector bv = block.getLocation().toVector().toBlockVector();
        for (Entity e : block.getChunk().getEntities()) {
            if (Objects.equals(bv, getBlockVector(e))) {
                return getTrophy(e);
            }
        }
        return null;
    }

    public static CouchRole getCouchRole(Entity trophy) {
        if (isTrophy(trophy) && trophy.getPersistentDataContainer().has(COUCH_ROLE, UUIDTagType.STRING)) {
            return CouchRole.valueOf(trophy.getPersistentDataContainer().get(COUCH_ROLE, UUIDTagType.STRING));
        }
        return null;
    }

    public static String getCouchGroupID(Entity trophy) {
        if (isTrophy(trophy) && trophy.getPersistentDataContainer().has(COUCH_ROLE, UUIDTagType.STRING)) {
            return trophy.getPersistentDataContainer().get(COUCH_GROUP, UUIDTagType.STRING);
        }
        return null;
    }

    public static boolean pickup(Player player, Entity entity) {
        if (!isTrophy(entity)) {
            return false;
        }
        ItemStack helmet = ((ArmorStand) entity).getEquipment().getHelmet();
        CouchRole role = getCouchRole(entity);
        if (role != null && role != CouchRole.Single) {
            String id = getCouchGroupID(entity);
            Trophy single = CouchUtil.getTrophy(id, CouchRole.Single);
            if (single != null) {
                helmet = single.getExampleItem();
            }
        }
        for (ItemStack item : player.getInventory().addItem(helmet).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), item);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 0.7f, 0.7f);
        if (entity.getPersistentDataContainer().has(SEAT_KEY, UUIDTagType.UUID)) {
            Entity seat = getSeat(entity);
            if (seat != null) {
                seat.remove();
            }
            BlockVector bv = getBlockVector(entity);
            if (bv != null) {
                Block block = entity.getWorld().getBlockAt(bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
                block.setType(Material.AIR);
            } else {
                // Old legacy handling, check nearby blocks up and down for slabs
                Block slab = entity.getLocation().getBlock();
                if (slab.getType() == SLAB_TYPE) {
                    slab.setType(Material.AIR);
                }
                slab = slab.getRelative(BlockFace.UP);
                if (slab.getType() == SLAB_TYPE) {
                    slab.setType(Material.AIR);
                }
                slab = slab.getRelative(BlockFace.DOWN, 2);
                if (slab.getType() == SLAB_TYPE) {
                    slab.setType(Material.AIR);
                }
            }
        }
        entity.remove();
        return true;
    }

    public static void init(Main main) {
        OWNER_KEY = new NamespacedKey(main, "trophy_owner");
        DIRECTION_KEY = new NamespacedKey(main, "trophy_direction");
        SEAT_KEY = new NamespacedKey(main, "trophy_seat");
        TROPHY_PARENT_KEY = new NamespacedKey(main, "trophy_parent");
        SEAT_BLOCK_KEY = new NamespacedKey(main, "trophy_seat_block");

        COUCH_GROUP = new NamespacedKey(main, "trophy_couch_group");
        COUCH_ROLE = new NamespacedKey(main, "trophy_couch_role");
    }
}
