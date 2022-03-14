package me.salamander.mallet.util;

import java.util.*;
import java.util.stream.Collectors;

public class Graph<T> {
    private final Map<T, Node> nodes = new HashMap<>();

    public Graph() {

    }

    public Node getNode(T value) {
        return nodes.computeIfAbsent(value, Node::new);
    }

    public void addEdge(T from, T to) {
        Node fromNode = getNode(from);
        Node toNode = getNode(to);

        fromNode.outNeighbors.add(toNode);
        toNode.inNeighbors.add(fromNode);
    }

    public Collection<Node> nodes() {
        return nodes.values();
    }

    public T getRoot() {
        return nodes.values().stream().filter(node -> node.inNeighbors.isEmpty()).findFirst().get().element;
    }

    public Collection<T> getRoots() {
        return nodes.values().stream().filter(node -> node.inNeighbors.isEmpty()).map(Node::getElement).collect(Collectors.toList());
    }

    public class Node {
        private final T element;
        private final Set<Node> inNeighbors = new HashSet<>();
        private final Set<Node> outNeighbors = new HashSet<>();

        protected Node(T value) {
            this.element = value;
        }

        public T getElement() {
            return element;
        }

        public Set<Node> getInNodes() {
            return inNeighbors;
        }

        public Set<Node> getOutNodes() {
            return outNeighbors;
        }

        public Set<T> getIn() {
            return inNeighbors.stream().map(node -> node.element).collect(Collectors.toSet());
        }

        public Set<T> getOut() {
            return outNeighbors.stream().map(node -> node.element).collect(Collectors.toSet());
        }
    }
}
