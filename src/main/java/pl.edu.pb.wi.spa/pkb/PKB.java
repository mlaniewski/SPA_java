package pl.edu.pb.wi.spa.pkb;

import pl.edu.pb.wi.spa.exception.PKBException;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

import java.util.List;
import java.util.Set;

public interface PKB {
    Node<ASTNode> getProcedureByName(String procName);
    Node<ASTNode> getStmtByLineNumber(int lineNumber) throws PKBException;

    boolean checkFollows(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient);
    List<Node<ASTNode>> getFollowing(Node<ASTNode> s1, boolean _transient);
    List<Node<ASTNode>> getFollowed(Node<ASTNode> s2, boolean _transient);
    boolean checkParent(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient);
    List<Node<ASTNode>> getParent(Node<ASTNode> s1, boolean _transient);
    List<Node<ASTNode>> getChildren(Node<ASTNode> s2, boolean _transient);
    boolean checkUses(Node<ASTNode> n, String var);
    List<String> getUsed(Node<ASTNode> n);
    List<Node<ASTNode>> getUsing(String var);
    boolean checkModifies(Node<ASTNode> n, String var);
    List<String> getModified(Node<ASTNode> n);
    List<Node<ASTNode>> getModifying(String var);
    boolean checkCalls(Node<ASTNode> p1, Node<ASTNode> p2, boolean _transient);
    List<Node<ASTNode>> getCallees(Node<ASTNode> p1, boolean _transient);
    List<Node<ASTNode>> getCallers(Node<ASTNode> p2, boolean _transient);
    boolean checkNext(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient);
    List<Node<ASTNode>> getNext(Node<ASTNode> s1, boolean _transient);
    List<Node<ASTNode>> getPrev(Node<ASTNode> s2, boolean _transient);
    List<Node<ASTNode>> getPattern(String var, String expr);

    Set<String> getAllPropertyValues(String propName);

    Set<String> getConstants();

    List<Node<ASTNode>> getAllValues(String predType);

    List<String> getAllVariables();
}
