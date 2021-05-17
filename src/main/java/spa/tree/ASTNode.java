package spa.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ASTNode {
    private static int nextId;
    private static Map<Integer, Node<ASTNode>> nodes = new HashMap<>();
    private HashMap<NodeParamType, String> params = new HashMap<>();
    private NodeType nodeType;
    private int lineNumber;
    private Integer id;


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

    public void setTreeIterator(Node<ASTNode> node) {
        nodes.put(id, node);
    }

    public static Node<ASTNode> getNodeById(Integer id) {
        return nodes.get(id);
    }

    public Integer getId() {
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

    public String nodeToString() {
        switch (nodeType) {
            case PROCEDURE:
            case VARIABLE:
            case CONSTANT:
                return getParam(NodeParamType.NAME);
            default:
                return String.valueOf(lineNumber);
        }
    }

    @Override
    public String toString() {
        return "ASTNode{" +
                "params=" + params +
                ", nodeType=" + nodeType +
                ", id=" + id +
                ", line number=" + lineNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ASTNode astNode = (ASTNode) o;
        return nodeType == astNode.nodeType &&
                Objects.equals(id, astNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeType, id);
    }
}
