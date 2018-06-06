package ru.ifmo.genetics.tools.olc.layouter;

import ru.ifmo.genetics.tools.olc.overlaps.Overlaps;
import ru.ifmo.genetics.tools.olc.overlaps.OverlapsList;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * DFS algorithm without recursive calls.
 * Calculates components number and top5 sizes. <br/>
 *
 * Algorithm is:
 * void run() {
 *      for (int i = 0; i < overlaps.readsNumber; i++) {
 *          if (!overlaps.isReadRemoved(i) && !visited.contains(i)) {
 *              walk(i);
 *          }
 *      }
 * }
 * void walk(from) {
 *      visited.add(from);
 *      for (Edge e : from.edges) {
 *          if (!visited.contains(e.to)) {
 *              walk(to);
 *          }
 *      }
 * }
 */
public class DfsAlgo {

    final Overlaps overlaps;

    public DfsAlgo(Overlaps overlaps) {
        this.overlaps = overlaps;
    }



    /**
     * top5 biggest component sizes
     */
    public int[] compSizes = new int[5];
    public int compNumber = 0;
    final HashSet<Integer> visited = new HashSet<Integer>();

    public void run() {
        for (int i = 0; i < overlaps.readsNumber; i++) {
            if (!overlaps.isReadRemoved(i) && !visited.contains(i)) {
                int curVisited = visited.size();

                boolean res = checkAndWalk(i);
                if (res) {
                    compNumber++;
                    int size = visited.size() - curVisited;
                    // updating compSizes
                    int j = compSizes.length - 1;
                    while (j >= 0 && size > compSizes[j]) {
                        if (j + 1 < compSizes.length)
                            compSizes[j + 1] = compSizes[j];
                        compSizes[j] = size;
                        j--;
                    }
                }
            }
        }
    }

    protected boolean checkAndWalk(int i) {
//        System.err.println("going from " + i);
        walk(i);
        return true;
    }


    public static class VInfo {
        int v;
        int ovListIndex = 0;
    }
    final LinkedList<VInfo> stack = new LinkedList<VInfo>();

    /**
     * DFS, but without calling walk function any more
     */
    private void walk(int from) {
        // initializing...
        addToStack(from);

        // going...
        VInfo vi;
        while (!stack.isEmpty()) {
            vi = stack.getLast();

            OverlapsList forward = overlaps.getList(vi.v);
            OverlapsList backward = overlaps.getList(vi.v ^ 1);
            boolean foundNext = false;
            while (!foundNext) {    // for (Edge e : edges) {
                int to = getTo(forward, backward, vi.ovListIndex);
                if (to == -1) {
                    break;
                }
                vi.ovListIndex++;

                boolean res = checkGoForward(vi, to);
                if (res && !visited.contains(to)) {
                    addToStack(to);
                    foundNext = true;
                }
            }

            if (!foundNext) {
                stack.removeLast();
                updateGoingBackward(vi, stack.peekLast());
            }
        }
    }

    protected boolean checkGoForward(VInfo vi, int to) {
        return true;
    }

    protected void updateGoingBackward(VInfo vi, VInfo prev) {
//        if (prev != null) {
//            System.err.println("returns to " + prev.v);
//        }
    }

    protected void addToStack(int to) {
        VInfo newVi = new VInfo();
        newVi.v = to;
        stack.addLast(newVi);
        visited.add(to);
//        System.err.println("going to " + to);
    }

    protected int getTo(OverlapsList forward, OverlapsList backward, int index) {
        if (index < forward.size()) {
            return forward.getTo(index);
        } else if (index - forward.size() < backward.size()) {
            return backward.getTo(index - forward.size()) ^ 1;
        }
        return -1;
    }

}
