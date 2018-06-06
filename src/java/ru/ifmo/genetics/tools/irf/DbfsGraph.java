package ru.ifmo.genetics.tools.irf;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaTools;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.dna.kmers.ImmutableBigKmer;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.NumUtils;
import ru.ifmo.genetics.utils.pairs.ImmutablePair;
import ru.ifmo.genetics.utils.pairs.Pair;
import ru.ifmo.genetics.utils.tool.Tool;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DbfsGraph {
    @SuppressWarnings("UnusedDeclaration")
    private Logger logger = Logger.getLogger(DbfsGraph.class);

    public final int k;
    public final ImmutableBigKmer start;
    public final ImmutableBigKmer end;
    public final int maxErrorsPerKmer;
    public boolean isCyclic = false;

//    private final ArrayList<Layer> leftLayers = new ArrayList<Layer>();
//    private final ArrayList<Layer> rightLayers = new ArrayList<Layer>();

    public boolean hasLinks() {
        return hasLinks;
    }

    public boolean hasLinks = false;

    @Nullable
    private HashSet<Node> nodesOnConnectiongPaths;

    @NotNull
    public HashMap<ImmutableBigKmer, Node> getLeftFront() {
        return leftFront;
    }

    public void setLeftFront(@NotNull HashMap<ImmutableBigKmer, Node> leftFront) {
        this.leftFront = leftFront;
    }

    @NotNull
    public HashMap<ImmutableBigKmer, Node> getRightFront() {
        return rightFront;
    }

    public void setRightFront(@NotNull HashMap<ImmutableBigKmer, Node> rightFront) {
        this.rightFront = rightFront;
    }

    @NotNull
    private HashMap<ImmutableBigKmer, Node> leftFront = new HashMap<ImmutableBigKmer, Node>();
    @NotNull
    private HashMap<ImmutableBigKmer, Node> rightFront = new HashMap<ImmutableBigKmer, Node>();
    @NotNull
    final private Long2ObjectOpenHashMap<Node> allNodes = new Long2ObjectOpenHashMap<Node>();

    @NotNull
    public Node getNode(ImmutableBigKmer mark) {
        Node res = allNodes.get(mark.longHashCode());
        if (res == null) {
            res = new Node(mark);
            allNodes.put(mark.longHashCode(), res);
        }
        //noinspection ConstantConditions
        assert res != null;
        return res;
    }

    @NotNull
    public Node startNode() {
        return getNode(start);
    }

    @NotNull
    public Node endNode() {
        return getNode(end);
    }

    public DbfsGraph(int k, ImmutableBigKmer start, ImmutableBigKmer end, int maxErrorsPerKmer) {
        this.k = k;
        this.start = start;
        this.end = end;
        this.maxErrorsPerKmer = maxErrorsPerKmer;


        leftFront.put(start, getNode(start));
        rightFront.put(end, getNode(end));
    }

    public int size() {
        return allNodes.size();
    }


    /*
    public int leftDepth() {
        return leftLayers.size() - 1;
    }

    public int rightDepth() {
        return rightLayers.size() - 1;
    }
    */

    /*
    public Layer addLeftLayer() {
        Layer layer = new Layer(leftLayers.size(), Part.LEFT);
        leftLayers.add(layer);
        return layer;
    }

    public Layer addRightLayer() {
        Layer layer = new Layer(rightLayers.size(), Part.RIGHT);
        rightLayers.add(layer);
        return layer;
    }
    */

    public enum Part {LEFT, RIGHT}


//    public class Layer {
//        public final int level;
//
//        @NotNull
//        public final Part part;
//
//        private final Map<Long, Node> nodes;
//
//        public Layer(int level, @NotNull Part part) {
//            this.level = level;
//            this.part = part;
//            this.nodes = new HashMap<Long, Node>();
//        }
//
//        /**
//         * Creates node with given mark if it doesn't already exist
//         * @param mark mark for node
//         * @return node with this mark (may be old node)
//         */
//        public Node addNode(long mark) {
//            Node oldNode = nodes.get(mark);
//            if (oldNode != null) {
//                return oldNode;
//            }
//
//            Node node = new Node(mark, this);
//            nodes.put(mark, node);
//            return node;
//        }
//
//        public int size() {
//            return nodes.size();
//        }
//
//        public Collection<Node> nodes() {
//            return nodes.values();
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (!(o instanceof Layer)) return false;
//
//            Layer layer = (Layer) o;
//
//            if (level != layer.level) return false;
//            if (part != layer.part) return false;
//
//            return true;
//        }
//
//        @Override
//        public int hashCode() {
//            int result = level;
//            result = 31 * result + part.hashCode();
//            return result;
//        }
//
//        @Override
//        public String toString() {
//            return id() + nodes.entrySet().toString();
//        }
//
//        public String id() {
//            return part.toString().substring(0, 1) + level;
//        }
//
//        @Nullable
//        public Node getNode(long mark) {
//            return nodes.get(mark);
//        }
//    }

    public Edge connect(Node from, Node to, int weight, EdgeType type) {
        int forwardNuc = to.mark.lastNuc();
        int backwardNuc = from.mark.firstNuc();
        Edge res = new Edge(from, to, forwardNuc, backwardNuc, weight, type);
        return connect(res);
    }

    public Edge connect(Edge edge) {
        if (!edge.from.outEdges.contains(edge)) {
            edge.from.outEdges.add(edge);
        }
        if (!edge.to.inEdges.contains(edge)) {
            edge.to.inEdges.add(edge);
        }
        return edge;
    }

    public void unconnect(Edge edge) {
        edge.from.outEdges.remove(edge);
        edge.to.inEdges.remove(edge);
        edge.type = EdgeType.DELETED;
    }

    public void unconnectRecursively(Edge edge) {
        unconnect(edge);
        if (edge.to.inEdges.isEmpty()) {
            ArrayList<Edge> outEdges = new ArrayList<Edge>(edge.to.outEdges);
            for (Edge e: outEdges) {
                unconnectRecursively(e);
            }
        }
    }

    public static enum EdgeType { TRAVERSED, TOUCHING, CONNECTING, DELETED, BASE}

    public class Edge {
        @NotNull
        public final Node from;
        @NotNull
        public final Node to;
        @NotNull
        public final LightDna forwardDna;
        @NotNull
        public final LightDna backwardDna;
        public int weight;


        @NotNull
        public EdgeType type;

        int length() {
            assert forwardDna.length() == backwardDna.length();
            return forwardDna.length();
        }

        private Edge(@NotNull Node from, @NotNull Node to,
                     int forwardNuc, int backwardNuc, int weight,
                     @NotNull EdgeType type) {
            this(from, to,
                    Dna.oneNucDnas[forwardNuc],
                    Dna.oneNucDnas[backwardNuc],
                    weight,
                    type
            );
        }

        private Edge(@NotNull Node from, @NotNull Node to,
                     @NotNull LightDna forwardDna,
                     @NotNull LightDna backwardDna,
                     int weight,
                     @NotNull EdgeType type) {
            this.from = from;
            this.to = to;
            this.forwardDna = forwardDna;
            this.backwardDna = backwardDna;
            this.weight = weight;
            this.type = type;
        }

        public boolean liesOnGoodWeightedPath() {
            return type == EdgeType.BASE;
        }

        @Override
        public String toString() {
            return from.id() + "->-" + forwardDna + "->-" + to.id();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;

            Edge edge = (Edge) o;

            if (!forwardDna.equals(edge.forwardDna)) return false;
            if (!from.equals(edge.from)) return false;
            if (!to.equals(edge.to)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = from.hashCode();
            result = 31 * result + to.hashCode();
            result = 31 * result + forwardDna.hashCode();
            return result;
        }

        public long longHashCode() {
            return from.longHashCode() * 1000000009 + DnaTools.longHashCode(forwardDna);
        }
    }

    public class Node {
        public final ImmutableBigKmer mark;

//        @NotNull public final Layer layer;

        @NotNull
        final public Collection<Edge> outEdges = new ArrayList<Edge>(2);
        @NotNull
        final public Collection<Edge> inEdges = new ArrayList<Edge>(2);

        public boolean isMarkedInLeftBfs = false;
        public boolean isMarkedInRightBfs = false;
        public boolean reachableFromStart = false;
        public boolean reachableFromEnd = false;
        public boolean isArticulationNode = false;
        public boolean isBeingVisited;

        public boolean liesOnGoodWeightedPath() {
            for (Edge e: inEdges) {
                if (e.liesOnGoodWeightedPath()) {
                    return true;
                }
            }
            for (Edge e: outEdges) {
                if (e.liesOnGoodWeightedPath()) {
                    return true;
                }
            }
            return false;
        }

        public boolean reachableFromBoth() {
            return reachableFromStart && reachableFromEnd;
        }

//        public Node lastPossibleBulgeStartInQuestion;
//        public int aliveTokensNumber;
//        public boolean noBulgeStart;


        // links connect nodes in left part with nodes in right part with the same mark
//        @Nullable ArrayList<Node> forwardLinks = null;

        public Node(ImmutableBigKmer mark) {
            this.mark = mark;
//            this.layer = layer;
        }

        /*
        public void linkTo(Node linkEnd) {
            assert mark == linkEnd.mark;
            assert layer.part == Part.LEFT;
            assert linkEnd.layer.part == Part.RIGHT;

            if (forwardLinks == null) {
                forwardLinks = new ArrayList<Node>(1);
            }

            forwardLinks.add(linkEnd);
            hasLinks = true;
        }
        */

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;

            Node node = (Node) o;

            if (!mark.equals(node.mark)) return false;
            // optimistic
//            if (mark.longHashCode() != node.mark.longHashCode()) return false;

            return true;
        }

        @Override
        public int hashCode() {
            @SuppressWarnings("UnnecessaryLocalVariable")
            int result = mark.hashCode();
//            result = 31 * result + layer.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return mark.toString();
        }

        public String id() {
            return toString();
//            + "_" + layer.id();
        }

        public long longHashCode() {
            return mark.longHashCode();
        }

//        public Node getActualLastPossibleBulgeStartInQuestion() {
//            Node res = lastPossibleBulgeStartInQuestion;
//            if (res == null) {
//                return res;
//            }
//
//            if (res.noBulgeStart) {
//                lastPossibleBulgeStartInQuestion = res.getActualLastPossibleBulgeStartInQuestion();
//            }
//            return res;
//        }
    }

    public String id() {
        return start + "_" + end;
    }

    private void toDotStream(Node node, PrintWriter out) {
        String color = node.isArticulationNode ? "red" : "black";
        if (node.mark == start) {
            out.printf("    %s [ label = \"S%s\", color=\"%s\"];\n", node.id(), node.toString(), color);
        } else if (node.mark == end) {
            out.printf("    %s [ label = \"E%s\", color=\"%s\"];\n", node.id(), node.toString(), color);
        } else {
            out.printf("    %s [ label = \"%s\", shape=point, color=\"%s\"];\n", node.id(), node.toString(), color);
        }
    }

    private void toDotStream(Edge edge, PrintWriter out) {
        toDotStream(edge.from, out);
        toDotStream(edge.to, out);
        String color = edge.liesOnGoodWeightedPath() ? "red" : "black";
        String style = edge.type.equals(EdgeType.DELETED) ? "dotted" : "solid";
        out.printf("    %s -> %s [ label = \"%s %d\", color = \"%s\", style = \"%s\" ];\n",
                edge.from.id(),
                edge.to.id(),
                edge.forwardDna,
                edge.weight,
                color,
                style
        );
    }

    public void toDotFile(String fileName) {
        /*
        assert !onlyNodesOnConnectingPaths || nodesOnConnectiongPaths != null;
        */

        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        out.println("digraph " + id() + " {");
        out.println("center=true; rankdir=LR;");

        for (Node node: allNodes.values()) {
            for (Edge outEdge: node.outEdges) {
                toDotStream(outEdge, out);
            }
        }

        /*
        for (int i = 0; i < leftLayers.size(); ++i) {
            layerToDotStream(leftLayers.get(i), out, onlyNodesOnConnectingPaths);
        }

        for (int i = 0; i < rightLayers.size(); ++i) {
            layerToDotStream(rightLayers.get(i), out, onlyNodesOnConnectingPaths);
        }
        */

        out.println("}");
        out.close();
    }

    /*
    private void layerToDotStream(Layer layer, PrintWriter out, boolean onlyNodesOnConnectingPaths) {
        for (Node node: layer.nodes()) {
            out.printf("    %s [ label = \"%s\", shape=point];\n", node.id(), node.toString());
        }

        out.print("    {");
        out.print(" rank=same;");
        for (Node node: layer.nodes()) {
            out.printf(" %s;", node.id());
        }
        out.println(" }");

        for (Node node: layer.nodes()) {
            assert !onlyNodesOnConnectingPaths || nodesOnConnectiongPaths != null;
            if (onlyNodesOnConnectingPaths && !nodesOnConnectiongPaths.contains(node)) {
                continue;
            }
            for (int i = 0; i < 4; ++i) {
                if (node.next[i] == null) {
                    continue;
                }
                out.printf("    %s -> %s [ label = \"%c %d\" ];\n",
                        node.id(),
                        node.next[i].id(),
                        DnaTools.toChar((byte)i),
                        node.nextWeights[i]);
            }

            List<Node> forwardLinks = node.forwardLinks;
            if (forwardLinks != null) {
                for (Node rightNode: forwardLinks) {
                    out.printf("    %s -> %s [ style=dotted, arrowhead=none ];\n",
                            node.id(),
                            rightNode.id());
                }
            }
        }
    }
    */

    public void findNodesOnConnectingPath() {
        markReachableFromStart(getNode(start));
        markReachableFromEnd(getNode(end));
        for (Node node: allNodes.values()) {
            if (!node.reachableFromStart) {
                continue;
            }
            for (Edge outEdge: node.outEdges) {
                if (!outEdge.to.reachableFromEnd) {
                    continue;
                }
                outEdge.type = EdgeType.CONNECTING;
            }
        }
    }

    private void markReachableFromEnd(Node node) {
        if (node == null) {
            return;
        }

        if (node.isBeingVisited) {
            isCyclic = true;
        }

        if (node.reachableFromEnd) {
            return;
        }
        node.reachableFromEnd = true;
        node.isBeingVisited = true;

        for (Edge inEdge: node.inEdges) {
            markReachableFromEnd(inEdge.from);
        }
        node.isBeingVisited = false;
    }

    private void markReachableFromStart(Node node) {
        if (node == null) {
            return;
        }

        if (node.isBeingVisited) {
            isCyclic = true;
        }

        if (node.reachableFromStart) {
            return;
        }
        node.reachableFromStart = true;
        node.isBeingVisited = true;

        for (Edge outEdge: node.outEdges) {
            markReachableFromStart(outEdge.to);
        }
        node.isBeingVisited = false;
    }

//    public void findFlow() {
//        findFlowFromLeftToRight(getNode(end));
//        finaFlowFromRightToLeft(getNode(start));
//    }
//
//    private void finaFlowFromRightToLeft(Node start) {
//    }
//
//    /**
//     * Normalize s.t. calculating weights of next edges depends on prev edges
//     * @param end
//     */
//    private void findFlowFromLeftToRight(Node end) {
//        if (end.flow > 0) {
//            return;
//        }
//
//        double inFlow = 0;
//        int totalInWeight = 0;
//        int inFlowWeight = 0;
//
//        for (int i = 0; i < 4; ++i) {
//            Node prev = end.prev[i];
//            if (prev == null) {
//                continue;
//            }
//
//            if (prev.reachableFromBoth()) {
//                findFlowFromLeftToRight(prev);
//                inFlow += end.prevProperties[i].flow;
//                inFlowWeight += end.prevProperties[i].weight;
//            }
//            totalInWeight += end.prevProperties[i].weight;
//
//        }
//
//        if (end.mark == start) {
//            end.flow = 1;
//        } else {
//            end.flow = inFlow * totalInWeight / inFlowWeight;
//        }
//
//        for (int i = 0; i < 4; ++i) {
//            Node prev = end.prev[i];
//            if (prev == null) {
//                continue;
//            }
//
//            if (!prev.reachableFromBoth()) {
//                end.prevProperties[i].flow = end.flow * end.prevProperties[i].weight / totalInWeight;
//            }
//        }
//
//        int totalOutWeight = 0;
//        for (int i = 0; i < 4; ++i) {
//            Node next = end.next[i];
//            if (next == null) {
//                continue;
//            }
//
//            totalOutWeight += end.nextProperties[i].weight;
//        }
//
//        for (int i = 0; i < 4; ++i) {
//            Node next = end.next[i];
//            if (next == null) {
//                continue;
//            }
//
//            end.nextProperties[i].flow = end.flow * end.nextProperties[i].weight / totalOutWeight;
//        }
//    }

//    public void findBulges() {
//        findBulges(getNode(start));
//    }
//
//    /**
//     * NOT WORKING!
//     */
//    private void findBulges(Node node) {
//        if (true) {
//            throw new UnsupportedOperationException("this function doesn't works right");
//        }
//        int numberOfInEdges = 0;
//        for (int i = 0; i < 4; ++i) {
//            Node prev = node.prev[i];
//            if (prev == null || !prev.reachableFromBoth()) {
//                continue;
//            }
//            ++numberOfInEdges;
//            if (!prev.isVisited) {
//                return;
//            }
//        }
//
//        Tool.debug(logger, true, "visiting " + node);
//        if (node.toString().equals("GCAAGCC")) {
//            int z = 0;
//        }
//
//        assert !node.isVisited;
//        node.isVisited = true;
//        node.noBulgeStart = false;
//
//        HashSet<Node> inTokens = new HashSet<Node>();
//
//        for (int i = 0; i < 4; ++i) {
//            Node prev = node.prev[i];
//            if (prev == null || !prev.reachableFromBoth()) {
//                continue;
//            }
//            Node curToken = prev;
//            inTokens.add(curToken);
//            while (true) {
//                curToken.aliveTokensNumber--;
//                Tool.debug(logger, true, "decreasing token " + curToken + ": " + curToken.aliveTokensNumber);
//                if (curToken.aliveTokensNumber != 0) {
//                    break;
//                }
//                Tool.info(logger, true, "bulge: " + curToken + " -> " + node);
//                inTokens.remove(curToken);
//                curToken = curToken.getActualLastPossibleBulgeStartInQuestion();
//                if (curToken == null) {
//                    break;
//                }
//                inTokens.add(curToken);
//            }
//        }
//
//        Tool.debug(logger, true, "inTokens: " + inTokens);
//        node.lastPossibleBulgeStartInQuestion = updateTokensToLastPredesessor(inTokens);
//        if (node.lastPossibleBulgeStartInQuestion != null) {
//            node.lastPossibleBulgeStartInQuestion.aliveTokensNumber++;
//        }
//        Tool.debug(logger, true, "last predesessor: " + node.lastPossibleBulgeStartInQuestion);
//
//        int numberOfOutEdges = 0;
//        for (int i = 0; i < 4; ++i) {
//            Node next = node.next[i];
//            if (next == null || !next.reachableFromBoth()) {
//                continue;
//            }
//
//            numberOfOutEdges++;
//        }
//
//        node.aliveTokensNumber = numberOfOutEdges;
//        for (int i = 0; i < 4; ++i) {
//            Node next = node.next[i];
//            if (next == null || !next.reachableFromBoth()) {
//                continue;
//            }
//
//            Tool.debug(logger, true, "going " + node + " -> " + next);
//            findBulges(next);
//        }
//
//
//    }
//
//    private Node updateTokensToLastPredesessor(HashSet<Node> tokens) {
//        if (tokens.size() == 0) {
//            return null;
//        }
//
//        Node lastPredesessor = getLastPredesessor(tokens);
//        for (Node node: tokens) {
//            while (node != lastPredesessor) {
//                assert !node.equals(lastPredesessor);
//                Node oldPrev = node.getActualLastPossibleBulgeStartInQuestion();
//                node.noBulgeStart = true;
//                node.lastPossibleBulgeStartInQuestion = lastPredesessor;
//                lastPredesessor.aliveTokensNumber += node.aliveTokensNumber - 1;
//                node.aliveTokensNumber = 0;
//                node = oldPrev;
//            }
//        }
//        return lastPredesessor;
//    }
//
//    private Node getLastPredesessor(Collection<Node> tokens) {
//        assert tokens.size() > 0;
//        int originalTokensNumber = tokens.size();
//        HashMap<Node, MutableInt> visits = new HashMap<Node, MutableInt>();
//        while (true) {
//
//            for (Node node: tokens) {
//                MutableInt curVisits = visits.get(node);
//                if (curVisits == null) {
//                    curVisits = new MutableInt();
//                    visits.put(node, curVisits);
//                }
//                curVisits.increment();
//                if (curVisits.intValue() == originalTokensNumber) {
//                    return node;
//                }
//            }
//
//            ArrayList<Node> newTokens = new ArrayList<Node>();
//            for (Node node: tokens) {
//                if (node.lastPossibleBulgeStartInQuestion != null) {
//                    newTokens.add(node.getActualLastPossibleBulgeStartInQuestion());
//                }
//            }
//            tokens = newTokens;
//        }
//    }

//    private int findArticulationNodesDfs(Node node, int numberOfTokens) {
//        int numberOfInEdges = 0;
//        for (int i = 0; i < 4; ++i) {
//            Node prev = node.prev[i];
//            if (prev == null || !prev.reachableFromBoth()) {
//                continue;
//            }
//            ++numberOfInEdges;
//            if (!prev.isVisited) {
//                return numberOfTokens;
//            }
//        }
//
//
//        numberOfTokens -= numberOfInEdges - 1;
//        assert !node.isVisited;
//        node.isVisited = true;
//
//        if (numberOfTokens == 1) {
//            node.isArticulationNode = true;
//        }
//
//
//        int numberOfOutEdges = 0;
//        for (int i = 0; i < 4; ++i) {
//            Node next = node.next[i];
//            if (next == null || !next.reachableFromBoth()) {
//                continue;
//            }
//
//            numberOfOutEdges++;
//        }
//
//        numberOfTokens += numberOfOutEdges - 1;
//        for (int i = 0; i < 4; ++i) {
//            Node next = node.next[i];
//            if (next == null || !next.reachableFromBoth()) {
//                continue;
//            }
//
//            numberOfTokens = findArticulationNodesDfs(next, numberOfTokens);
//        }
//        return numberOfTokens;
//    }
//
//    public void findArticulationNodes() {
//        Tool.debug(logger, true, "Finding articulation nodes");
//        Node startNode = getNode(start);
//
//        for (int i = 0; i < 4; ++i) {
//            Node prev = startNode.prev[i];
//            if (prev == null) {
//                continue;
//            }
//            assert !prev.reachableFromBoth();
//        }
//
//        findArticulationNodesDfs(startNode, 0);
//    }

    private boolean analysed = false;

    public void analyse() {
        assert !analysed;
        analysed = true;
        findNodesOnConnectingPath();
        removeNotConnectingEdges();
        makeConsensus();
        compressEdges();
//        findBulges();
//        findFlow();
//        findArticulationNodes();

    }

    private void tryCompressEdgesAroundNode(Node node) {
        if (!(node.inEdges.size() == 1 && node.outEdges.size() == 1)) {
            return;
        }

        Edge inEdge = node.inEdges.iterator().next();
        Edge outEdge = node.outEdges.iterator().next();

        if (!inEdge.type.equals(outEdge.type)) {
            return;
        }

        Edge compressedEdge = new Edge(
                inEdge.from, outEdge.to,
                new Dna(inEdge.forwardDna, outEdge.forwardDna),
                new Dna(outEdge.backwardDna, inEdge.backwardDna),
                (int) (((long)inEdge.weight * inEdge.length() + (long)outEdge.weight * outEdge.length()) / (inEdge.length() + outEdge.length())),
                inEdge.type
        );
        unconnect(inEdge);
        unconnect(outEdge);
        connect(compressedEdge);
    }

    public void compressEdges() {
        for (Node node: allNodes.values()) {
            tryCompressEdgesAroundNode(node);
        }

    }

    private void removeNotConnectingEdges() {
        for (Node node: allNodes.values()) {
            Iterator<Edge> edgeIterator = node.outEdges.iterator();
            while (edgeIterator.hasNext()) {
                Edge outEdge = edgeIterator.next();
                if (outEdge.type.equals(EdgeType.CONNECTING)) {
                    continue;
                }

                Node to = outEdge.to;
                edgeIterator.remove();
                to.inEdges.remove(outEdge);
            }
        }
    }

    private void makeConsensus() {
        // assert all connecting edges removed
        ArrayList<Edge> allEdges = new ArrayList<Edge>();

        for (Node node: allNodes.values()) {
            for (Edge outEdge: node.outEdges) {
                allEdges.add(outEdge);
            }
        }
        Collections.sort(allEdges, new Comparator<Edge>() {
            @Override
            public int compare(Edge e1, Edge e2) {
                // heavier edges go first
                return NumUtils.compare(e2.weight, e1.weight);
            }
        });

        int goodPaths = 0;
        for (Edge anchorEdge: allEdges) {
            if (anchorEdge.liesOnGoodWeightedPath()) {
                continue;
            }
            if (anchorEdge.type != EdgeType.CONNECTING) {
                continue;
            }

            Collection<Edge> updatedEdges = new HashSet<Edge>();
            markEdgesAsGood(anchorEdge, updatedEdges);

            if (updatedEdges.isEmpty()) {
                break;
            }

            goodPaths++;
            if (goodPaths > 3) {
                break;
            }

            removeBadSimilarEdges();
//            break;
        }
    }

    private final static int MAX_LEVENSHTEIN_DISTANCE = 10;

    private void removeBadSimilarEdges() {
        Errors noErrors = new Errors(k);
        cache1.clear();
        cache2.clear();

        distCache.clear();
        distCache2.clear();

        for (Node node: allNodes.values()) {
            boolean hasGoodOutEdge = false;
            boolean hasBadOutEdge = false;
            for (Edge edge: node.outEdges) {
                if (edge.liesOnGoodWeightedPath()) {
                    hasGoodOutEdge = true;
                } else {
                    hasBadOutEdge = true;
                }
            }

            ArrayList<Edge> toRemove = new ArrayList<Edge>();
            if (hasGoodOutEdge && hasBadOutEdge) {
                for (Iterator<Edge> it = node.outEdges.iterator(); it.hasNext(); ) {
                    Edge edge = it.next();
                    if (edge.liesOnGoodWeightedPath()) {
                        continue;
                    }

//                    Tool.debug(logger, true, "Trying to remove edge " + edge);
//                    int d = maxDistFromPathXToMostSimilarGoodPathFromY(edge, node);
//                    Tool.debug(logger, true, "Max distance is " + d);
//                    if (d <= MAX_LEVENSHTEIN_DISTANCE) {
                    if (anyPathFromXHaveSimilarGoodPathFromY(edge, node, noErrors)) {
//                        Tool.debug(logger, true, "Removed edge " + edge);
                        toRemove.add(edge);
                    }
                }
            }
            for (Edge edge: toRemove) {
                unconnectRecursively(edge);
            }
        }
    }

    private static int numberOfErrors = 0;
    private static ConcurrentHashMap<Errors, Errors> errorsCache = new ConcurrentHashMap<Errors, Errors>();

    public static class Errors {
        public static final int MAX_ERRORS_PER_KMER = 3;
        private final int k;
        private final int[] positions;

        private Errors withError = null;
        private Errors withGood = null;
        private final static Logger logger = Logger.getLogger(Errors.class);

        public static Errors noErrors(int k) {
            return get(k, new int[0]);
        }

        private static Errors get(int k, int[] positions) {
            Errors res = new Errors(k, positions);
            if (!errorsCache.containsKey(res)) {
                errorsCache.putIfAbsent(res, res);
            }
            return errorsCache.get(res);
        }

        private Errors(int k) {
            this(k, new int[0]);
        }

        private Errors(int k, int[] positions) {
            this.k = k;
            this.positions = positions;
            numberOfErrors++;
            if ((numberOfErrors & (numberOfErrors - 1)) == 0) {
                Tool.debug(logger, "number of Errors objects: " + numberOfErrors);
            }
        }

        public Errors appendIndel() {
            return append(true);
        }

        public Errors appendMismatch(boolean mismatch) {
            return append(mismatch);
        }

        private Errors append(boolean error) {
            if (error) {
                if (withError == null) {
                    withError = forceAppend(error);
                }
                return withError;
            }
            if (withGood == null) {
                withGood = forceAppend(error);
            }
            return withGood;

        }
        private Errors forceAppend(boolean error) {
            int rightShift = error ? 1 : 0;
            int leftShift = (positions.length > 0 && positions[0] == k) ? 1 : 0;
            int[] newPositions = new int[positions.length - leftShift + rightShift];
            System.arraycopy(positions, leftShift, newPositions, 0, positions.length - leftShift);
            for (int i = 0; i < newPositions.length; ++i) {
                newPositions[i]++;
            }
            return get(k, newPositions);
        }

        public int errorsNumber() {
            return positions.length;
        }
        public boolean tooMany() {
            return positions.length > MAX_ERRORS_PER_KMER;
        }

        @Override
        public String toString() {
            return "Errors{" +
                    "positions=" + Arrays.toString(positions) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Errors)) return false;

            Errors errors = (Errors) o;

            if (k != errors.k) return false;
            if (!Arrays.equals(positions, errors.positions)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = k;
            result = 31 * result + Arrays.hashCode(positions);
            return result;
        }

        public long longHashCode() {
            long result = k;
            result = 31 * result + Misc.longHashCode(positions);
            return result;
        }
    }

    public boolean tooMany(Errors errors) {
        return errors.errorsNumber() > maxErrorsPerKmer;
    }
    private Long2BooleanMap cache1 = new Long2BooleanOpenHashMap(allNodes.size());
    private Long2BooleanMap cache2 = new Long2BooleanOpenHashMap(allNodes.size());

//    private HashMap<Tuple3<Node, Node, Errors>, Boolean> cache1 = new HashMap<Tuple3<Node, Node, Errors>, Boolean>();
//    private HashMap<Tuple3<Edge, Node, Errors>, Boolean> cache2 = new HashMap<Tuple3<Edge, Node, Errors>, Boolean>();
    private int xxx = 0;
    /**
     * Have false-negatives
     * @param nodeX
     * @param nodeY
     * @param doneErrors
     * @return
     */
    private boolean anyPathFromXHaveSimilarGoodPathFromY(Node nodeX, Node nodeY, Errors doneErrors) {
        if (doneErrors.tooMany()) {
            return false;
        }
//        Tuple3<Node, Node, Errors> key = Tuple3.create(nodeX, nodeY, doneErrors);
        long key = nodeX.longHashCode();
        key *= 1000000009;
        key += nodeY.longHashCode();
        key *= 1000000009;
        key += doneErrors.longHashCode();

        if (!cache1.containsKey(key)) {
            xxx++;
            if ((xxx & (xxx - 1)) == 0) {
//                System.err.println("Number of calls: " + xxx);
            }
//            System.err.println(nodeX + " " + nodeY + " " + doneErrors);
            cache1.put(key, anyPathFromXHaveSimilarGoodPathFromYImpl(nodeX, nodeY, doneErrors));
        }
        return cache1.get(key);
    }

    private boolean anyPathFromXHaveSimilarGoodPathFromY(Edge edgeX, Node nodeY, Errors doneErrors) {
        if (doneErrors.tooMany()) {
            return false;
        }
//        Tuple3<Edge, Node, Errors> key = Tuple3.create(edgeX, nodeY, doneErrors);
        long key = edgeX.longHashCode();
        key *= 1000000009;
        key += nodeY.longHashCode();
        key *= 1000000009;
        key += doneErrors.longHashCode();
        if (!cache2.containsKey(key)) {
            xxx++;
            if ((xxx & (xxx - 1)) == 0) {
//                System.err.println("Number of calls: " + xxx);
            }
//            System.err.println(edgeX + " " + nodeY + " " + doneErrors);
            cache2.put(key, anyPathFromXHaveSimilarGoodPathFromYImpl(edgeX, nodeY, doneErrors));
        }
        return cache2.get(key);
    }

    private boolean anyPathFromXHaveSimilarGoodPathFromYImpl(Node nodeX, Node nodeY, Errors doneErrors) {
        boolean xIsDeadEnd = nodeX.outEdges.isEmpty();
        boolean yIsDeadEnd = nodeY.outEdges.isEmpty();
        if (xIsDeadEnd && yIsDeadEnd) {
            return nodeX.equals(nodeY);
        }

        if (nodeX.equals(nodeY)) {
            return true;
        }

        if (xIsDeadEnd) {
            for (Edge edgeY: nodeY.outEdges) {
                if (!edgeY.liesOnGoodWeightedPath()) {
                    continue;
                }
                if (anyPathFromXHaveSimilarGoodPathFromY(nodeX, edgeY.to, doneErrors.appendIndel())) {
                    return true;
                }
            }
            return false;
        }

        if (yIsDeadEnd) {
            for (Edge edgeX: nodeX.outEdges) {
                if (!anyPathFromXHaveSimilarGoodPathFromY(edgeX.to, nodeY, doneErrors.appendIndel())) {
                    return false;
                }
            }
            return true;
        }

        for (Edge edgeX: nodeX.outEdges) {
            if (!anyPathFromXHaveSimilarGoodPathFromY(edgeX, nodeY, doneErrors)) {
                return false;
            }
        }
        return true;
    }

    private boolean anyPathFromXHaveSimilarGoodPathFromYImpl(Edge edgeX, Node nodeY, Errors doneErrors) {
        boolean yIsDeadEnd = nodeY.outEdges.isEmpty();

        if (yIsDeadEnd) {
            return !anyPathFromXHaveSimilarGoodPathFromY(edgeX.to, nodeY, doneErrors.appendIndel());
        }

        boolean haveAny = false;
        for (Edge edgeY: nodeY.outEdges) {
            if (!edgeY.liesOnGoodWeightedPath()) {
                continue;
            }

            // Can't work with not singular edges
            assert edgeX.length() == 1 : edgeX.length();
            assert edgeY.length() == 1 : edgeY.length();

            if (anyPathFromXHaveSimilarGoodPathFromY(edgeX.to, edgeY.to, doneErrors.appendMismatch(edgeX.forwardDna.nucAt(0) != edgeY.forwardDna.nucAt(0))) ||
                    anyPathFromXHaveSimilarGoodPathFromY(edgeX, edgeY.to, doneErrors.appendIndel()) ||
                    anyPathFromXHaveSimilarGoodPathFromY(edgeX.to, nodeY, doneErrors.appendIndel())) {
                haveAny = true;
                break;
            }
        }
        return haveAny;
    }

    private HashMap<Pair<Node, Node>, Integer> distCache = new HashMap<Pair<Node, Node>, Integer>();
    private HashMap<Pair<Edge, Edge>, Integer> distCache2 = new HashMap<Pair<Edge, Edge>, Integer>();


    private int maxDistFromPathXToMostSimilarGoodPathFromY(Node nodeX, Node nodeY) {
        if (nodeX == nodeY) {
            return 0;
        }

        Pair<Node, Node> key = new ImmutablePair<Node, Node>(nodeX, nodeY);

        if (distCache.containsKey(key)) {
            return distCache.get(key);
        }

//        System.err.println(nodeX + " ###### " + nodeY);

        if (nodeX.outEdges.isEmpty()) {
            int res = Integer.MAX_VALUE / 2;
            for (Edge edgeY: nodeY.outEdges) {
                res = Math.min(res,
                        maxDistFromPathXToMostSimilarGoodPathFromY(nodeX, edgeY.to) +
                                levenshteinDistance(Dna.emptyDna, edgeY.forwardDna));
            }
            distCache.put(key, res);
            return res;
        }

        int res = -1;
        for (Edge edgeX: nodeX.outEdges) {
            res = Math.max(res, maxDistFromPathXToMostSimilarGoodPathFromY(edgeX, nodeY));
        }
        assert res != -1;
        distCache.put(key, res);
        return res;
    }

    private int maxDistFromPathXToMostSimilarGoodPathFromY(Node nodeX, Edge edgeY) {
//        System.err.println(nodeX + " ###### " + edgeY);
        if (nodeX.outEdges.isEmpty()) {
            return levenshteinDistance(Dna.emptyDna, edgeY.forwardDna) +
                    maxDistFromPathXToMostSimilarGoodPathFromY(nodeX, edgeY.to);
        }

        int res = -1;
        for (Edge edgeX: nodeX.outEdges) {
            int d = maxDistFromPathXToMostSimilarGoodPathFromY(edgeX, edgeY);
            res = Math.max(res, d);
        }
        assert res != -1;
        return res;
    }

    private int maxDistFromPathXToMostSimilarGoodPathFromY(Edge edgeX, Node nodeY) {
//        System.err.println(edgeX + " ###### " + nodeY);
        if (nodeY.outEdges.isEmpty()) {
            return levenshteinDistance(edgeX.forwardDna, Dna.emptyDna) +
                    maxDistFromPathXToMostSimilarGoodPathFromY(edgeX.to, nodeY);
        }

        int res = Integer.MAX_VALUE / 2;
        for (Edge edgeY: nodeY.outEdges) {
            if (!edgeY.liesOnGoodWeightedPath()) {
                continue;
            }
            res = Math.min(res, maxDistFromPathXToMostSimilarGoodPathFromY(edgeX, edgeY));

        }
        assert res != Integer.MAX_VALUE / 2;
        return res;
    }

    private int maxDistFromPathXToMostSimilarGoodPathFromY(Edge edgeX, Edge edgeY) {
        assert edgeY.liesOnGoodWeightedPath();

        Pair<Edge, Edge> key = new ImmutablePair<Edge, Edge>(edgeX, edgeY);

        if (distCache2.containsKey(key)) {
            return distCache2.get(key);
        }
//        System.err.println(edgeX + " ###### " + edgeY);

        int res;

        {
            int d = maxDistFromPathXToMostSimilarGoodPathFromY(edgeX.to, edgeY.to);
            d += levenshteinDistance(edgeX.forwardDna, edgeY.forwardDna);
            res = d;
        }

        {
            int d = maxDistFromPathXToMostSimilarGoodPathFromY(edgeX, edgeY.to);
            d += levenshteinDistance(Dna.emptyDna, edgeY.forwardDna);
            res = Math.min(res, d);
        }

        {
            int d = maxDistFromPathXToMostSimilarGoodPathFromY(edgeX.to, edgeY);
            d += levenshteinDistance(edgeX.forwardDna, Dna.emptyDna);
            res = Math.min(res, d);
        }
        distCache2.put(key, res);
        return res;
    }

    private final static int INDEL_PENALTY = 1;
    private final static int MISMATCH_PENALTY = 1;

    private int levenshteinDistance(LightDna a, LightDna b) {
        int n = a.length();
        int m = b.length();
        if (n == 0 || m == 0) {
            return INDEL_PENALTY * (n + m);
        }

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; ++i) {
            for (int j = 0; j <= m; ++j) {
                d[i][j] = Integer.MAX_VALUE;
            }
        }

        d[0][0] = 0;

        for (int i = 0; i <= n; ++i) {
            for (int j = 0; j <= m; ++j) {
                if (i > 0) {
                    d[i][j] = Math.min(d[i][j], d[i - 1][j] + INDEL_PENALTY);
                }
                if (j > 0) {
                    d[i][j] = Math.min(d[i][j], d[i][j - 1] + INDEL_PENALTY);
                }

                if (i > 0 && j > 0) {
                    d[i][j] = Math.min(d[i][j], d[i - 1][j - 1] + (a.nucAt(i - 1) != b.nucAt(j - 1) ? MISMATCH_PENALTY : 0));
                }
            }
        }
        return d[n][m];
    }


    private void markEdgesAsGood(Edge edge, Collection<Edge> updatedEdges) {
        assert !edge.liesOnGoodWeightedPath();

        markEdgesAsGoodForward(edge, updatedEdges);
        markEdgesAsGoodBackward(edge.from, updatedEdges);

    }

    private void markEdgesAsGoodForward(Node node, Collection<Edge> updatedEdges) {
        int maxOutWeight = -1;
        Edge maxOutWeightEdge = null;

        for (Edge outEdge: node.outEdges) {
            if (outEdge.weight > maxOutWeight) {
                maxOutWeight = outEdge.weight;
                maxOutWeightEdge = outEdge;
            }
        }
        if (maxOutWeightEdge != null) {
            markEdgesAsGoodForward(maxOutWeightEdge, updatedEdges);
        }
    }

    private void markEdgesAsGoodForward(Edge edge, Collection<Edge> updatedEdges) {
        if (edge.liesOnGoodWeightedPath()) {
            return;
        }

        edge.type = EdgeType.BASE;
        updatedEdges.add(edge);

        markEdgesAsGoodForward(edge.to, updatedEdges);
    }

    private void markEdgesAsGoodBackward(Node node, Collection<Edge> updatedEdges) {
        int maxInWeight = -1;
        Edge maxInWeightEdge = null;

        for (Edge inEdge: node.inEdges) {
            if (inEdge.weight > maxInWeight) {
                maxInWeight = inEdge.weight;
                maxInWeightEdge = inEdge;
            }
        }

        if (maxInWeightEdge != null) {
            markEdgesAsGoodBackward(maxInWeightEdge, updatedEdges);
        }

    }

    private void markEdgesAsGoodBackward(Edge edge, Collection<Edge> updatedEdges) {
        if (edge.liesOnGoodWeightedPath()) {
            return;
        }

        edge.type = EdgeType.BASE;
        updatedEdges.add(edge);

        markEdgesAsGoodBackward(edge.from, updatedEdges);
    }
}
