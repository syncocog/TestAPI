package com.projectcrowd.engine;

import com.projectcrowd.ProjectCrowdPlugin;
import com.projectcrowd.bot.BotEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

public class MovementEngine {

    private final ProjectCrowdPlugin plugin;
    private final Random random = new Random();

    public MovementEngine(ProjectCrowdPlugin plugin) {
        this.plugin = plugin;
    }

    public void randomWalk(BotEntity bot, double radius) {
        Location loc = bot.getLocation();
        Location target = loc.clone().add(random.nextDouble() * radius - radius / 2.0, 0, random.nextDouble() * radius - radius / 2.0);
        navigateAStar(bot, target, 12, 800);
    }

    public void tickMovement(BotEntity bot) {
        // Head rotation toward movement direction is implicit in lookAt calls
        if (random.nextDouble() < 0.1) {
            // Occasional small jump by teleporting slightly upward if safe
            Location l = bot.getLocation().clone();
            Block above = l.clone().add(0, 1, 0).getBlock();
            if (above.getType() == Material.AIR) {
                bot.getBukkitEntity().teleport(l.add(0, 0.5, 0));
            }
        }
    }

    // Very basic grid A* on block centers, bounded by maxNodes and timeBudgetMs
    public void navigateAStar(BotEntity bot, Location goal, int maxNodes, int timeBudgetMs) {
        Location start = clampToBlockCenter(bot.getLocation());
        Location target = clampToBlockCenter(goal);
        if (!Objects.equals(start.getWorld(), target.getWorld())) return;

        long deadline = System.currentTimeMillis() + Math.max(20, timeBudgetMs);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Key, Node> all = new HashMap<>();
        Key startKey = new Key(start.getBlockX(), start.getBlockY(), start.getBlockZ());
        Node startNode = new Node(startKey, null, 0, heuristic(start, target));
        open.add(startNode);
        all.put(startKey, startNode);

        Node best = startNode;

        int expansions = 0;
        while (!open.isEmpty() && System.currentTimeMillis() < deadline && expansions < maxNodes) {
            Node current = open.poll();
            expansions++;
            best = current;
            if (current.key.x == target.getBlockX() && current.key.y == target.getBlockY() && current.key.z == target.getBlockZ()) {
                best = current; break;
            }
            for (int[] d : NEIGHBORS) {
                int nx = current.key.x + d[0];
                int ny = current.key.y + d[1];
                int nz = current.key.z + d[2];
                if (!isWalkable(bot, nx, ny, nz)) continue;
                Key nk = new Key(nx, ny, nz);
                double g = current.g + 1;
                double h = heuristic(blockCenter(nx, ny, nz), target);
                Node old = all.get(nk);
                if (old == null || g + h < old.f) {
                    Node nn = new Node(nk, current, g, h);
                    all.put(nk, nn);
                    open.add(nn);
                }
            }
        }

        // Follow first step towards best
        List<Location> path = reconstruct(best);
        if (path.size() >= 2) {
            Location next = path.get(1);
            bot.lookAt(next);
            bot.moveTowards(next, 0.6);
        } else {
            // Fallback simple move towards goal
            bot.lookAt(goal);
            bot.moveTowards(goal, 0.5);
        }
    }

    private Location clampToBlockCenter(Location l) {
        return new Location(l.getWorld(), l.getBlockX() + 0.5, l.getBlockY(), l.getBlockZ() + 0.5);
    }

    private double heuristic(Location a, Location b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    private Location blockCenter(int x, int y, int z) {
        return new Location(null, x + 0.5, y, z + 0.5);
    }

    private List<Location> reconstruct(Node end) {
        List<Location> list = new ArrayList<>();
        Node cur = end;
        while (cur != null) {
            list.add(new Location(null, cur.key.x + 0.5, cur.key.y, cur.key.z + 0.5));
            cur = cur.parent;
        }
        Collections.reverse(list);
        return list;
    }

    private boolean isWalkable(BotEntity bot, int x, int y, int z) {
        Location base = new Location(bot.getWorld(), x, y, z);
        Block feet = base.getBlock();
        Block head = base.clone().add(0, 1, 0).getBlock();
        Block below = base.clone().subtract(0, 1, 0).getBlock();
        return feet.getType() == Material.AIR && head.getType() == Material.AIR && below.getType().isSolid();
    }

    private static final int[][] NEIGHBORS = new int[][]{
            {1,0,0},{-1,0,0},{0,0,1},{0,0,-1}, // horizontal
            {0,1,0},{0,-1,0} // up/down
    };

    private static class Key {
        final int x,y,z;
        Key(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
        @Override public boolean equals(Object o){if(this==o)return true; if(!(o instanceof Key k))return false; return x==k.x&&y==k.y&&z==k.z;}
        @Override public int hashCode(){return Objects.hash(x,y,z);} }
    private static class Node {
        final Key key; final Node parent; final double g; final double h; final double f;
        Node(Key key, Node parent, double g, double h){this.key=key;this.parent=parent;this.g=g;this.h=h;this.f=g+h;}
    }
}