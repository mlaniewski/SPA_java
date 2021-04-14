package pl.edu.pb.wi.spa.tree;

import java.util.HashMap;
import java.util.Map;

public class ASTNode {
    private static int nextId;
    private static Map<Integer, Node<ASTNode>> nodes = new HashMap<>();
    private Node<ASTNode> nodeIter;
    private HashMap<NodeParamType, String> params = new HashMap<>();
    //public?
    private NodeType nodeType;
    private int lineNumber;
    private int id;


    public ASTNode(NodeType nodeType) {
        this.nodeType = nodeType;
        this.id = nextId++;
    }

    public void setParam(NodeParamType type, String val) {
        this.params.put(type, val);
    }

    public String getParam(NodeParamType type) {
        return params.get(type);
    }

    public Node<ASTNode> getTreeIterator() {
        return nodeIter;
    }

    public void setTreeIterator(Node<ASTNode> node) {
        this.nodeIter = node;
        this.nodes.put(id, node);
    }

    public Node<ASTNode> getNodeById(int id) {
        return this.nodes.get(id);
    }

    public int getId() {
        return id;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "ASTNode{" +
                "params=" + params +
                ", nodeType=" + nodeType +
                ", id=" + id +
                '}';
    }
}
