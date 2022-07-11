package com.github.kristianvld.angeltrophies.couch;

import com.github.kristianvld.angeltrophies.Main;
import com.github.kristianvld.angeltrophies.trophy.Trophy;
import com.github.kristianvld.angeltrophies.util.Pair;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockVector;

import java.util.*;

public class CouchUtil {

    public static final Map<Pair<String, CouchRole>, Trophy> trophies = new HashMap<>();

    /**
     * Repopulate CouchUtil.trophies with
     *
     * @param trophies
     */
    public static void buildCache(Collection<Trophy> trophies) {
        CouchUtil.trophies.clear();
        Set<String> groups = new HashSet<>();
        for (Trophy t : trophies) {
            if (t.getCouchRole() != null) {
                Pair key = new Pair(t.getCouchGroup(), t.getCouchRole());
                if (CouchUtil.trophies.containsKey(key)) {
                    throw new IllegalArgumentException("The couch part " + t.getCouchGroup() + ":" + t.getCouchRole() + " has been defined twice for the items " + CouchUtil.trophies.get(key).getName() + " and " + t.getName());
                }
                CouchUtil.trophies.put(key, t);
                Main.getInstance().getLogger().info("Loaded couch part " + t.getName() + ", group: " + t.getCouchGroup() + ", role: " + t.getCouchRole());
                groups.add(t.getCouchGroup());
            }
        }

        for (String group : groups) {
            Trophy single = CouchUtil.trophies.get(new Pair(group, CouchRole.Single));
            if (single == null) {
                throw new IllegalStateException("Couch group '" + group + "' is missing default role Single!");
            }
        }
    }

    public static Trophy getTrophy(String couchGroup, CouchRole couchRole) {
        return trophies.get(new Pair(couchGroup, couchRole));
    }

    public static Trophy getTrophy(Trophy trophy, Block block, float yaw) {
        if (trophy.getCouchRole() == null) {
            return trophy;
        }
        String group = trophy.getCouchGroup();

        CouchRole role = getRole(block, yaw, group);

        if (role == CouchRole.Single) {
            return trophy;
        }
        return getTrophy(group, role);
    }

    private static BlockVector yawToVector(float yaw) {
        int x = (int) Math.round(-1 * Math.sin(Math.toRadians(yaw)));
        int z = (int) Math.round(1 * Math.cos(Math.toRadians(yaw)));
        return new BlockVector(x, 0, z);
    }

    /**
     * If the entity in an adjacent block has a couch role and a group that matches parameter 3, return that trophy.
     *
     * @param block
     * @param yaw
     * @param group
     * @return
     */
    public static Entity getRelative(Block block, float yaw, String group) {
        BlockVector dir = yawToVector(yaw);
        // If there is a trophy in the relative position
        Entity trophy = Trophy.getTrophy(block.getRelative(dir.getBlockX(), 0, dir.getBlockZ()));
        String groupId = Trophy.getCouchGroupID(trophy);
        // If the group of the trophy matches the group we're looking for, return trophy.
        if (group.equals(groupId)) {
            return trophy;
        }
        return null;
    }

    public static CouchRole getRole(Block block, float yaw, String group){

        CouchRole role = CouchRole.Single;

        yaw = Math.round(yaw / 90) * 90;
        Entity back = getRelative(block, yaw, group);
        yaw += 90;
        Entity right = getRelative(block, yaw, group);
        yaw += 90;
        Entity forward = getRelative(block, yaw, group);
        yaw += 90;
        Entity left = getRelative(block, yaw, group);
        System.out.println("Directions Checked");

        if (right != null) {
            System.out.println("right is not null");
            if (trophies.containsKey(new Pair(group, CouchRole.LeftEnd))) {
                role = CouchRole.LeftEnd;
            }
            if (forward != null) {
                if (trophies.containsKey(new Pair(group, CouchRole.InnerCorner))) {
                    role = CouchRole.InnerCorner;
                }
            } else if (left != null) {
                if (trophies.containsKey(new Pair(group, CouchRole.Middle))) {
                    role = CouchRole.Middle;
                }
            } else if (back != null) {
                if (trophies.containsKey(new Pair(group, CouchRole.OuterCorner))) {
                    role = CouchRole.OuterCorner;
                }
            }
        } else if (left != null) {
            System.out.println("Left is not null");
            if (trophies.containsKey(new Pair(group, CouchRole.RightEnd))) {
                role = CouchRole.RightEnd;
            }
        }
        return role;
    }

    public void recalculateCouchesRecursive(Block block, List<Block> calculated){
        Entity trophy = get
    }

}
