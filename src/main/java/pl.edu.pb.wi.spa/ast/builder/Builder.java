package pl.edu.pb.wi.spa.ast.builder;

import pl.edu.pb.wi.spa.ast.AST;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;
import pl.edu.pb.wi.spa.tree.NodeType;

import java.util.*;

public class Builder {

    private AST ast;
    private List<Node<ASTNode>> astTree;
    private List<Node<ASTNode>> procedures;
    private List<Node<ASTNode>> whiles;
    private List<Node<ASTNode>> ifs;
    private List<Node<ASTNode>> assigments;
    private Set<String> variables;
    private Set<String> constants;
    private List<Node<ASTNode>> callNodes;
    private List<Node<ASTNode>> programLines;
    private Map<Integer, Set<Integer>> callers;
    private Map<Integer, Set<Integer>> callersT;
    private Map<Integer, Set<Integer>> callees;
    private Map<Integer, Set<Integer>> calleesT;
    private Map<Integer, Set<String>> modifies;
    private Map<String, Set<Integer>> modified;
    private Map<Integer, Set<String>> uses;
    private Map<String, Set<Integer>> used;
    private Map<Integer, Integer> parent;
    private Map<Integer, Set<Integer>> parentT;
    private Map<Integer, Set<Integer>> children;
    private Map<Integer, Set<Integer>> childrenT;
    private Map<Integer, Set<Integer>> nextN;
    private Map<Integer, Set<Integer>> nextT;
    private Map<Integer, Set<Integer>> prevN;
    private Map<Integer, Set<Integer>> prevT;
    private Map<Integer, Set<String>> pattern;
    private Map<Integer, String> fullPattern;
    private Map<Integer, Set<Integer>> affecting;
    private Map<Integer, Set<Integer>> affectingT;
    private Map<Integer, Set<Integer>> affected;
    private Map<Integer, Set<Integer>> affectedT;

    private List<Node<ASTNode>> varNodes;
    private List<Node<ASTNode>> constantNodes;


    public Builder() {
        ASTNode n = new ASTNode(NodeType.PROGRAM);
        astTree = Collections.singletonList(new Node<>(n));
        procedures = new ArrayList<>();
        whiles = new ArrayList<>();
        ifs = new ArrayList<>();
        assigments = new ArrayList<>();
        variables = new HashSet<>();
        constants = new HashSet<>();
        programLines = new ArrayList<>();
        callers = new HashMap<>();
        callersT = new HashMap<>();
        callees = new HashMap<>();
        calleesT = new HashMap<>();
        modifies = new HashMap<>();
        modified = new HashMap<>();
        uses = new HashMap<>();
        used = new HashMap<>();
        parent = new HashMap<>();
        parentT = new HashMap<>();
        children = new HashMap<>();
        childrenT = new HashMap<>();
        nextN = new HashMap<>();
        nextT = new HashMap<>();
        prevN = new HashMap<>();
        prevT = new HashMap<>();
        pattern = new HashMap<>();
        fullPattern = new HashMap<>();
        callNodes = new ArrayList<>();
        affecting = new HashMap<>();
        affectingT = new HashMap<>();
        affected = new HashMap<>();
        affectedT = new HashMap<>();
        ast = new AST(
                callers,
                callersT,
                callees,
                calleesT,
                modifies,
                modified,
                uses,
                used,
                parent,
                parentT,
                children,
                childrenT,
                nextN,
                nextT,
                prevN,
                prevT,
                pattern,
                fullPattern,
                affecting,
                affectingT,
                affected,
                affectedT,
                procedures,
                whiles,
                ifs,
                assigments,
                variables,
                constants,
                callNodes,
                programLines);
        varNodes = new ArrayList<>();
        constantNodes = new ArrayList<>();
    }

    public Node<ASTNode> createNode(NodeType nodeType) {
        if (nodeType == NodeType.PROGRAM) {
            return astTree.iterator().next();
        }
        ASTNode n = new ASTNode(nodeType);
        Node<ASTNode> node = astTree.iterator().next().addChild(new Node<>(n));
        n.setTreeIterator(node);

        switch (nodeType) {
            case PROCEDURE:
                procedures.add(node);
                break;
            case VARIABLE:
                varNodes.add(node);
                break;
            case CALL:
                callNodes.add(node);
                break;
            case WHILE:
                whiles.add(node);
                break;
            case IF:
                ifs.add(node);
                break;
            case ASSIGN:
                assigments.add(node);
                break;
            case CONSTANT:
                constantNodes.add(node);
                break;
            default:
                break;
        }
        return node;
    }

    public void addChild(Node<ASTNode> parent, Node<ASTNode> child) {
        if (parent.getData().getNodeType() != NodeType.PROGRAM) {
            //astTree.iterator().next().
            //astTree->move_ontop(astTree->append_child(parent), child);
            parent.addChild(child);
        }
    }

    public void addNodeParameter(Node<ASTNode> node, NodeParamType paramType, String value) {
        node.getData().setParam(paramType, value);
    }

    public List<Node<ASTNode>> getAstTree() {
        return astTree;
    }
}
