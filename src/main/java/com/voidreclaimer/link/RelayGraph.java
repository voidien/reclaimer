package com.voidreclaimer.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a seat's signal reaches a place, and how strong it is there. It reads nothing from the world: nodes and bridges are
 * given, in world coordinates.
 *
 * <p>A node passes the signal to anything in its dimension within its range, so a hop is one way: a seat reaches a relay inside
 * the seat's range, and that relay's own range decides how far it carries on. A {@link Bridge} joins two nodes across a dimension
 * with no loss. A chain is as strong as its weakest hop, and the strongest chain wins.
 */
public final class RelayGraph {
    /** A place in a dimension, in world coordinates. */
    public record Place(ResourceKey<Level> dimension, Vec3 position) {}

    /** Something that passes the signal on, out to {@code range} blocks. */
    public record Node(Place place, double range) {}

    /** Two nodes joined whatever lies between them, both ways: the mouths of a lit portal pair. */
    public record Bridge(Node a, Node b) {}

    /**
     * What the graph says about a place.
     *
     * @param reachable whether any chain reaches it; false means the signal is lost
     * @param strength  the strongest chain's weakest hop, 0 to 1
     * @param hops      nodes on that chain after the seat; 0 when the seat reaches the place itself. Of equally strong chains, any
     */
    public record Reading(boolean reachable, double strength, int hops) {
        public static final Reading NONE = new Reading(false, 0, 0);
    }

    /** Relays, then each bridge's two ends side by side. */
    private final Node[] nodes;
    /** The index of the node across each node's bridge, or -1. */
    private final int[] across;

    public RelayGraph(List<Node> relays, List<Bridge> bridges) {
        List<Node> all = new ArrayList<>(relays);
        for (Bridge bridge : bridges) {
            all.add(bridge.a());
            all.add(bridge.b());
        }
        nodes = all.toArray(Node[]::new);
        across = new int[nodes.length];
        Arrays.fill(across, -1);
        for (int i = 0; i < bridges.size(); i++) {
            int a = relays.size() + 2 * i;
            across[a] = a + 1;
            across[a + 1] = a;
        }
    }

    /**
     * The signal from {@code seat} at {@code target}. {@code band} is the share of every range that carries static.
     *
     * <p>Best chain first, like Dijkstra's search with "weakest hop so far" for distance: a node's value is final when it is
     * taken, and the search stops as soon as no remaining node could beat what already reaches the target. Quadratic in the node
     * count at worst.
     */
    public Reading signal(Node seat, Place target, double band) {
        int seatIndex = nodes.length;
        int count = nodes.length + 1;
        double[] best = new double[count];
        int[] hops = new int[count];
        boolean[] taken = new boolean[count];
        Arrays.fill(best, -1);
        best[seatIndex] = 1;

        double bestAtTarget = -1;
        int hopsAtTarget = 0;
        while (true) {
            int from = strongest(best, taken);
            if (from < 0 || best[from] <= bestAtTarget) {
                break;
            }
            taken[from] = true;
            Node node = from == seatIndex ? seat : nodes[from];
            double here = Math.min(best[from], hop(node, target, band));
            if (here > bestAtTarget) {
                bestAtTarget = here;
                hopsAtTarget = hops[from];
            }
            if (from != seatIndex && across[from] >= 0) {
                offer(best, hops, across[from], best[from], hops[from] + 1);
            }
            for (int to = 0; to < nodes.length; to++) {
                if (!taken[to]) {
                    offer(best, hops, to, Math.min(best[from], hop(node, nodes[to].place(), band)), hops[from] + 1);
                }
            }
        }
        return bestAtTarget < 0 ? Reading.NONE : new Reading(true, bestAtTarget, hopsAtTarget);
    }

    /** The strength of one hop, or -1 when it does not reach: a place at the very edge is reached, at no strength. */
    private static double hop(Node from, Place to, double band) {
        if (from.place().dimension() != to.dimension()) {
            return -1;
        }
        double distance = from.place().position().distanceTo(to.position());
        return distance > from.range() ? -1 : SignalMath.strength(distance, from.range(), band);
    }

    /** Takes a chain to {@code to} if it is stronger than the one there. */
    private static void offer(double[] best, int[] hops, int to, double value, int hopCount) {
        if (value > best[to]) {
            best[to] = value;
            hops[to] = hopCount;
        }
    }

    /** The untaken node with the strongest chain, or -1 when no untaken node is reached. */
    private static int strongest(double[] best, boolean[] taken) {
        int pick = -1;
        for (int i = 0; i < best.length; i++) {
            if (!taken[i] && best[i] >= 0 && (pick < 0 || best[i] > best[pick])) {
                pick = i;
            }
        }
        return pick;
    }
}
