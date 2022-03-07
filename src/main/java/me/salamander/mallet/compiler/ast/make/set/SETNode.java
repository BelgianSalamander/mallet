package me.salamander.mallet.compiler.ast.make.set;

import me.salamander.mallet.compiler.ast.node.ASTNode;
import me.salamander.mallet.compiler.ast.make.cfg.CFGNode;
import me.salamander.mallet.compiler.ast.make.cfg.ControlFlowGraph;
import me.salamander.mallet.util.Graph;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SETNode {
    protected @Nullable SETNode parent;
    protected final Set<CFGNode> body;
    protected final Set<Set<CFGNode>> subBodies;
    protected final Set<SETNode> children = new HashSet<>();

    protected final StructureEncapsulationTree tree;
    protected final Map<Set<CFGNode>, List<SETNode>> topologicalSort = new HashMap<>(); // Sub-group to sort

    public SETNode(StructureEncapsulationTree tree, Set<CFGNode> body, Set<Set<CFGNode>> subBodies) {
        this.body = body;
        this.subBodies = subBodies;
        this.tree = tree;

        checkSubBodies();
    }

    public boolean canAdd(SETNode node) {
        return this.body.containsAll(node.body);
    }

    public void add(SETNode node) {
        if(node == this) {
            System.err.println("WTF");
        }

        //Check if can add to children
        for(SETNode child : children) {
            if(child.canAdd(node)) {
                child.add(node);
                return;
            }
        }

        //Otherwise, add to self but make sure it only intersects a single sub-body
        for(Set<CFGNode> subBody : subBodies) {
            if(subBody.containsAll(node.body)) {
                children.add(node);
                node.parent = this;

                for(CFGNode member: node.body) {
                    member.SETParent = node;
                }

                //Check if any of this node's children can be added to node
                Set<SETNode> loweredChildren = new HashSet<>();
                for(SETNode child : this.children) {
                    if(child != node && node.canAdd(child)) {
                        node.add(child);
                        loweredChildren.add(child);
                    }
                }

                this.children.removeAll(loweredChildren);
                List<SETNode> sort = this.topologicalSort.get(subBody);

                if (sort != null) {
                    int i = 0;
                    for (; i < sort.size(); i++) {
                        if (loweredChildren.contains(sort.get(i))) {
                            break;
                        }
                    }

                    sort.removeAll(loweredChildren);
                    sort.add(i, node);
                }

                return;
            }
        }

        throw new RuntimeException("Could not find location for SET node");
    }

    public abstract CFGNode getEntryPoint();

    private void checkSubBodies() {
        //Make sure every node is in a sub-body and that it only appears once
        int totalLength = 0;
        Set<CFGNode> union = new HashSet<>();

        for(Set<CFGNode> subBody : subBodies) {
            if(subBody.size() == 0) {
                throw new RuntimeException("Sub-body cannot be empty");
            }

            totalLength += subBody.size();
            union.addAll(subBody);
        }

        if(!body.equals(union)) {
            throw new RuntimeException("Sub-bodies must be disjoint");
        }

        if(totalLength != body.size()) {
            throw new RuntimeException("Sub-bodies must be disjoint");
        }
    }

    public Map<Set<CFGNode>, List<SETNode>> getTopologicalSort() {
        return topologicalSort;
    }

    public void makeLabelledBlocks() {
        for (Set<CFGNode> subBody : subBodies) {
            Set<SETNode> siblings = new HashSet<>();

            for (SETNode child : children) {
                if (subBody.containsAll(child.body)) {
                    siblings.add(child);
                }
            }

            makeLabelledBlocks(siblings, subBody);
        }
    }

    private void makeLabelledBlocks(Set<SETNode> siblings, Set<CFGNode> subBody) {
        //Dava Paper Algorithm 3 (EnhancedTopologicalSort)
        Graph<SETNode> graph = new Graph<>();

        for (SETNode sibling : siblings) {
            for (CFGNode cfgNode : sibling.body) {
                for (CFGNode successor : cfgNode.getSuccessors()) {
                    if (!sibling.body.contains(successor) && subBody.contains(successor)) {
                        SETNode toNode = null;

                        for(SETNode other : siblings) {
                            if(other.body.contains(successor)) {
                                toNode = other;
                            }
                        }

                        if(toNode == null) {
                            throw new RuntimeException("Could not find successor");
                        }

                        if(!excludeEdgeFromTopologicalSort(sibling, cfgNode, toNode, successor)) {
                            graph.addEdge(sibling, toNode);
                        }
                    }
                }
            }
        }

        Set<SETNode> rootSet = new HashSet<>();

        for(SETNode child : siblings) {
            if(graph.getNode(child).getInNodes().size() != 1) {
                rootSet.add(child);
            }
        }

        for (SETNode child : siblings) {
            Set<SETNode> successors = graph.getNode(child).getOutNodes().stream()
                    .map(Graph.Node::getElement)
                    .filter(n -> !rootSet.contains(n))
                    .collect(Collectors.toSet());

            if(successors.size() == 0) continue;

            Iterator<SETNode> it = successors.iterator();
            SETNode successor = it.next();

            while (it.hasNext()) {
                rootSet.add(successor);
            }
        }

        Map<SETNode, List<SETNode>> linkedLists = new HashMap<>();
        Graph<SETNode> rootGraph = new Graph<>();
        rootSet.forEach(rootGraph::getNode);

        for (SETNode setNode : rootSet) {
            //Find linked list
            List<SETNode> linkedList = new ArrayList<>();

            SETNode curr = setNode;

            while (true) {
                linkedList.add(curr);

                Set<SETNode> successors = graph.getNode(curr).getOutNodes().stream()
                        .map(Graph.Node::getElement)
                        .filter(n -> !rootSet.contains(n))
                        .collect(Collectors.toSet());

                if(successors.size() == 0) {
                    break;
                } else if(successors.size() > 1) {
                    throw new RuntimeException("Multiple successors");
                }

                curr = successors.iterator().next();
            }

            linkedLists.put(setNode, linkedList);

            Set<SETNode> successors = new HashSet<>();

            for (SETNode node : linkedList) {
                successors.addAll(graph.getNode(node).getOutNodes().stream().map(Graph.Node::getElement).toList());
            }

            successors.removeIf(linkedList::contains);

            for (SETNode successor : successors) {
                rootGraph.addEdge(setNode, successor);
            }
        }

        List<SETNode> sorted = new ArrayList<>();

        Stack<SETNode> stack = new Stack<>();
        Set<SETNode> visited = new HashSet<>();

        stack.push(rootGraph.getRoot());

        while(!stack.isEmpty()) {
            SETNode node = stack.pop();

            if(!visited.add(node)) continue;

            sorted.addAll(linkedLists.get(node));

            for (Graph<SETNode>.Node n : rootGraph.getNode(node).getOutNodes()) {
                stack.push(n.getElement());
            }
        }

        this.topologicalSort.put(subBody, sorted);
        List<SETNode> labelledBlocks = new ArrayList<>();

        //Make labelled blocks
        for (int i = 1; i < sorted.size(); i++) {
            SETNode node = sorted.get(i);
            SETNode prev = sorted.get(i - 1);

            Set<SETNode> in = graph.getNode(node).getIn();

            if(in.size() > 1 || in.iterator().next() != prev) {
                //Make labelled block
                SETNode blockStart = null;
                int j;
                for(j = 0; j < i; j++) {
                    if(in.contains(sorted.get(j))) {
                        blockStart = sorted.get(j);
                        break;
                    }
                }

                if(blockStart == null) {
                    throw new RuntimeException("Could not find block start");
                }

                //Block is [j, i - 1]
                Set<CFGNode> body = new HashSet<>();

                for (int k = j; k < i; k++) {
                    body.addAll(sorted.get(k).body);
                }

                LabelledBlockSetNode labelledBlock = new LabelledBlockSetNode(tree, body, sorted, node);
                labelledBlocks.add(labelledBlock);
            }
        }

        labelledBlocks.forEach(this.tree::addNode);
    }

    protected boolean excludeEdgeFromTopologicalSort(SETNode fromSETNode, CFGNode fromCFGNode, SETNode toSETNode, CFGNode toCFGNode) {
        return false;
    }

    public void print(PrintStream out, String indent) {
        out.println(indent +  "SETNode " + this + " (" + this.tree.node2id.getInt(this) + ")");

        indent += "  ";

        out.println(indent + "Sub-bodies:");
        indent += "  ";
        int i = 0;
        for(Set<CFGNode> subBody : subBodies) {
            String startIndent = indent;
            out.println(indent + "Sub-body: " + i);
            indent += "  ";
            out.print(indent + "Nodes: [ ");
            for(CFGNode node : subBody) {
                out.print(node.getId() + " ");
            }
            out.println("]");
            out.println(indent + "Children:");
            indent += "  ";
            for(SETNode child : children) {
                if(subBody.containsAll(child.body)) {
                    child.print(out, indent);
                }
            }
            i++;
            indent = startIndent;
        }
    }

    public Set<CFGNode> getBody() {
        return body;
    }

    public Set<CFGNode> getBodyFor(CFGNode node) {
        for(Set<CFGNode> subBody : subBodies) {
            if(subBody.contains(node)) {
                return subBody;
            }
        }

        throw new RuntimeException("Could not find sub-body for node");
    }

    public Set<Set<CFGNode>> getSubBodies() {
        return subBodies;
    }

    public Set<SETNode> getChildren() {
        return children;
    }

    public String getLabel() {
        return "L_" + this.tree.node2id.getInt(this);
    }

    public CFGNode getSortSuccessor() {
        SETNode curr = this.parent;
        SETNode searchFor = this;

        while (curr != null) {
            for (List<SETNode> sort : curr.topologicalSort.values()) {
                for (int i = 0; i < sort.size() - 1; i++) {
                    if (sort.get(i) == searchFor) {
                        return sort.get(i + 1).getEntryPoint();
                    }
                }
            }

            searchFor = curr;
            curr = curr.parent;
        }

        return null;
    }

    public abstract List<ASTNode> makeAST(ControlFlowGraph cfg);
}
