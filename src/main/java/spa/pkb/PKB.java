package spa.pkb;

import spa.exception.SPAException;
import spa.tree.ASTNode;
import spa.tree.Node;

import java.util.List;
import java.util.Set;

public interface PKB {
    Node<ASTNode> getProcedureByName(String procedureName);
    Node<ASTNode> getStmtByLine(int line) throws SPAException;

    boolean isFollows(Node<ASTNode> node1, Node<ASTNode> node2, boolean _transient);
    List<Node<ASTNode>> getFollowing(Node<ASTNode> node1, boolean _transient);
    List<Node<ASTNode>> getFollowedBy(Node<ASTNode> node2, boolean _transient);
    boolean isParent(Node<ASTNode> node1, Node<ASTNode> node2, boolean _transient);
    List<Node<ASTNode>> getParent(Node<ASTNode> node1, boolean _transient);
    List<Node<ASTNode>> getChildren(Node<ASTNode> node2, boolean _transient);
    boolean isUses(Node<ASTNode> node, String var);
    List<String> getUsedBy(Node<ASTNode> node);
    List<Node<ASTNode>> getUsing(String var);
    boolean isModifies(Node<ASTNode> node, String var);
    List<String> getModifiedBy(Node<ASTNode> node);
    List<Node<ASTNode>> getModifying(String var);
    boolean isCalls(Node<ASTNode> proc1, Node<ASTNode> proc2, boolean _transient);
    List<Node<ASTNode>> getCalledBy(Node<ASTNode> proc1, boolean _transient);
    List<Node<ASTNode>> getCalling(Node<ASTNode> proc2, boolean _transient);
    boolean isNext(Node<ASTNode> node1, Node<ASTNode> node2, boolean _transient);
    List<Node<ASTNode>> getNext(Node<ASTNode> node1, boolean _transient);
    List<Node<ASTNode>> getPrevious(Node<ASTNode> node2, boolean _transient);
    List<Node<ASTNode>> getPattern(String var, String expr);

    Set<String> getAllPropertyValues(String propName);

    Set<String> getConstants();

    List<Node<ASTNode>> getAllValues(String predType);

    List<String> getAllVariables();
}
