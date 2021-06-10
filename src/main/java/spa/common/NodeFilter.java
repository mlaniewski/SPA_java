package spa.common;

import spa.tree.ASTNode;
import spa.tree.Node;
import spa.tree.NodeType;

import java.util.ArrayList;
import java.util.List;

public class NodeFilter {

    public List<Node<ASTNode>> filterNodesByType(List<Node<ASTNode>> nodes, String type) {
        return filterNodesByType(nodes, type, "");
    }

    public List<Node<ASTNode>> filterNodesByType(List<Node<ASTNode>> nodes, String type, String type2) {
        List<Node<ASTNode>> filteredNodes = new ArrayList<>();
        for (Node<ASTNode> node : nodes) {
            boolean m1 = matchType(node.getData().getNodeType(), type);
            boolean m2 = matchType(node.getData().getNodeType(), type2);
            boolean isEmpty = type2.isEmpty();
            if (isEmpty ? m1 : (m1 || m2)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    private boolean matchType(NodeType nodeType, String predType) {
        switch (predType) {
            case "procedure":
                return nodeType == NodeType.PROCEDURE;
            case "statement":
                return nodeType == NodeType.CALL
                        || nodeType == NodeType.WHILE
                        || nodeType == NodeType.IF
                        || nodeType == NodeType.ASSIGN;
            case "assign":
                return nodeType == NodeType.ASSIGN;
            case "while":
                return nodeType == NodeType.WHILE;
            case "if":
                return nodeType == NodeType.IF;
            case "var":
                return nodeType == NodeType.VARIABLE;
            case "call":
                return nodeType == NodeType.CALL;
            case "const":
                return nodeType == NodeType.CONSTANT;
        }
        return false;
    }
}
