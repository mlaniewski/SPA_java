package spa.common;


import spa.tree.ASTNode;
import spa.tree.Node;
import spa.tree.NodeParamType;
import spa.tree.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AST {
    private List<Node<ASTNode>> tree;
    private List<Node<ASTNode>> procedures;
    private List<Node<ASTNode>> whiles;
    private List<Node<ASTNode>> ifs;
    private List<Node<ASTNode>> assignments;
    private List<Node<ASTNode>> callNodes;
    private List<Node<ASTNode>> varNodes;
    private List<Node<ASTNode>> constantNodes;
    private List<Node<ASTNode>> programLines;

    public AST() {
        ASTNode n = new ASTNode(NodeType.PROGRAM);
        this.tree = Collections.singletonList(new Node<>(n));
        this.procedures = new ArrayList<>();
        this.whiles = new ArrayList<>();
        this.ifs = new ArrayList<>();
        this.assignments = new ArrayList<>();
        this.callNodes = new ArrayList<>();
        this.varNodes = new ArrayList<>();
        this.constantNodes = new ArrayList<>();
        this.programLines = new ArrayList<>();
    }

    public Node<ASTNode> createNode(NodeType nodeType) {
        if (nodeType == NodeType.PROGRAM) {
            return tree.iterator().next();
        }
        ASTNode n = new ASTNode(nodeType);
        Node<ASTNode> node = tree.iterator().next().addChild(new Node<>(n));
        ASTNode.addToNodes(n.getId(), node);

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
                assignments.add(node);
                break;
            case CONSTANT:
                constantNodes.add(node);
                break;
        }
        return node;
    }

    public void addChild(Node<ASTNode> parent, Node<ASTNode> child) {
        if (parent.getData().getNodeType() != NodeType.PROGRAM) {
            parent.addChild(child);
        }
    }

    public void addNodeParameter(Node<ASTNode> node, NodeParamType paramType, String param) {
        node.getData().putParam(paramType, param);
    }

    public Node<ASTNode> getProcedureByName(String procedureName) {
        Optional<Node<ASTNode>> procNode = procedures.stream().filter(node -> node.getData().getParam(NodeParamType.NAME).equals(procedureName)).findFirst();
        return procNode.orElse(null);
    }


    public Node<ASTNode> getTree() {
        return tree.get(0);
    }

    public List<Node<ASTNode>> getAstTreeAsList() {
        List<Node<ASTNode>> list = new ArrayList<>();
        Node<ASTNode> astTree = getTree();
        list.add(astTree);
        list.addAll(astTree.getChildren());
        return list;
    }

    public void addLineNumbers() {
        List<Node<ASTNode>> list = getAstTreeAsList();
        int line = 0;
        for (Node<ASTNode> node : list) {
            ASTNode astNode = node.getData();
            switch (astNode.getNodeType()) {
                case CALL:
                case WHILE:
                case IF:
                case ASSIGN:
                    astNode.setLine(++line);
                    programLines.add(node);
            }
        }
    }

    public List<Node<ASTNode>> getProgramLines() {
        return programLines;
    }

    public List<Node<ASTNode>> getProcedures() {
        return procedures;
    }

    public List<Node<ASTNode>> getWhiles() {
        return whiles;
    }

    public List<Node<ASTNode>> getIfs() {
        return ifs;
    }

    public List<Node<ASTNode>> getAssignments() {
        return assignments;
    }

    public List<Node<ASTNode>> getCallNodes() {
        return callNodes;
    }

    public List<Node<ASTNode>> getVarNodes() {
        return varNodes;
    }

    public List<Node<ASTNode>> getConstantNodes() {
        return constantNodes;
    }


    public void printTree(boolean full) {
        Node<ASTNode> node = getTree();
        System.out.println(node.getData());
        printTree(node.getChildren().get(0), 1, full);
    }

    private void printTree(Node<ASTNode> node, int depth, boolean full) {
        String cut = "";
        for (int i=0; i<depth; i++) cut += " | ";
        if (depth > 0) cut += " └ ";

        if (full) {
            System.out.println(cut + node.getData());
        } else {
            if (node.getData().getLine() != 0) {
                System.out.println(cut + node.getData());
            }
        }
        depth++;

        for (Node<ASTNode> astNode : node.getChildren()) {
            printTree(astNode, depth, full);
        }
    }
}
