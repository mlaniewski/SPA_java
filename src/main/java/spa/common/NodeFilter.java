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
            boolean e = type2.isEmpty();
            if (e ? m1 : (m1 || m2)) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    private boolean matchType(NodeType nodeType, String predType) {
        if (predType.equals("procedure")) {
            return nodeType == NodeType.PROCEDURE;
        } else if (predType.equals("statement")) {
            return nodeType == NodeType.CALL
                    || nodeType == NodeType.WHILE
                    || nodeType == NodeType.IF
                    || nodeType == NodeType.ASSIGN;
        } else if (predType.equals("assign")) {
            return nodeType == NodeType.ASSIGN;
        }
        else if (predType.equals("while")) {
            return nodeType == NodeType.WHILE;
        }
        else if (predType.equals("if")) {
            return nodeType == NodeType.IF;
        } else if (predType.equals("var")) {
            return nodeType == NodeType.VARIABLE;
        } else if (predType.equals("call")) {
            return nodeType == NodeType.CALL;
        } else if (predType.equals("const")) {
            return nodeType == NodeType.CONSTANT;
        }
        return false;
    }
}
