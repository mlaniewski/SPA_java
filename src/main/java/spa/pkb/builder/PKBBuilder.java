package spa.pkb.builder;

import spa.common.AST;
import spa.pkb.PKB;
import spa.pkb.PKBImpl;
import spa.tree.ASTNode;
import spa.tree.Node;
import spa.tree.NodeParamType;
import spa.tree.NodeType;

import java.util.*;

public class PKBBuilder {
    private Set<String> variables;
    private Set<String> constants;
    private Map<Integer, Set<Integer>> callers;
    private Map<Integer, Set<Integer>> callersT;
    private Map<Integer, Set<Integer>> calledBy;
    private Map<Integer, Set<Integer>> calledByT;
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

    private List<Integer> tmpList = new ArrayList<>();
    private AST ast;

    public PKBBuilder(AST ast) {
        this.ast = ast;
        this.variables = new HashSet<>();
        this.constants = new HashSet<>();
        this.callers = new HashMap<>();
        this.callersT = new HashMap<>();
        this.calledBy = new HashMap<>();
        this.calledByT = new HashMap<>();
        this.modifies = new HashMap<>();
        this.modified = new HashMap<>();
        this.uses = new HashMap<>();
        this.used = new HashMap<>();
        this.parent = new HashMap<>();
        this.parentT = new HashMap<>();
        this.children = new HashMap<>();
        this.childrenT = new HashMap<>();
        this.nextN = new HashMap<>();
        this.nextT = new HashMap<>();
        this.prevN = new HashMap<>();
        this.prevT = new HashMap<>();
        this.pattern = new HashMap<>();
        this.fullPattern = new HashMap<>();
    }

    public PKB buildPKB() {
        for (Node<ASTNode> varNode : ast.getVarNodes()) {
            variables.add(varNode.getData().getParam(NodeParamType.NAME));
        }
        for (Node<ASTNode> constantNode : ast.getConstantNodes()) {
            constants.add(constantNode.getData().getParam(NodeParamType.NAME));
        }
        buildCallMaps();
        buildParentAndChildrenMaps(ast.getAstTreeAsList().get(0));
        buildModifiesAndUsesMaps();
        for (Node<ASTNode> procedure : ast.getProcedures()) {
            Node<ASTNode> procedureChild = procedure.getChildren().iterator().next();
            buildNextStmtLst(procedureChild, null);
        }
        buildTransientRelation(nextT);
        buildTransientRelation(callersT);
        buildTransientRelation(calledByT);
        buildTransientRelation(childrenT);
        buildInvertedVariableRelation(modifies, modified);
        buildInvertedVariableRelation(uses, used);
        buildTransientRelation(prevT);
        buildPattern(ast.getAssignments());

        return new PKBImpl(callers,
                callersT,
                calledBy,
                calledByT,
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
                variables,
                constants,
                ast);
    }

    private void buildCallMaps() {
        List<Node<ASTNode>> callNodes = ast.getCallNodes();
        for (Node<ASTNode> callNode : callNodes) {
            ASTNode node = callNode.getData();
            ASTNode caller = ast.getProcedureByName(node.getParam(NodeParamType.CALLER)).getData();
            ASTNode calledBy = ast.getProcedureByName(node.getParam(NodeParamType.CALLED_BY)).getData();

            this.callers.computeIfAbsent(caller.getId(), k -> new HashSet<>());
            this.callers.get(caller.getId()).add(calledBy.getId());
            this.calledBy.computeIfAbsent(calledBy.getId(), k -> new HashSet<>());
            this.calledBy.get(calledBy.getId()).add(caller.getId());
            //*
            this.callersT.computeIfAbsent(caller.getId(), k -> new HashSet<>());
            this.callersT.get(caller.getId()).add(calledBy.getId());
            this.calledByT.computeIfAbsent(calledBy.getId(), k -> new HashSet<>());
            this.calledByT.get(calledBy.getId()).add(caller.getId());
        }
    }

    private void buildParentAndChildrenMaps(Node<ASTNode> node) {
        NodeType nodeType = node.getData().getNodeType();
        boolean isLeaf = nodeType == NodeType.CALL || nodeType == NodeType.ASSIGN;
        boolean isContainer = nodeType == NodeType.WHILE || nodeType == NodeType.IF;
        boolean isStmt = isLeaf || isContainer;

        int nodeId = node.getData().getId();
        if (isStmt && !tmpList.isEmpty()) {
            parent.put(nodeId, tmpList.get(tmpList.size()-1));
            parentT.computeIfAbsent(nodeId, k -> new HashSet<>());
            for (Integer id : tmpList) {
                parentT.get(nodeId).add(id);
            }
            children.computeIfAbsent(parent.get(nodeId), k -> new HashSet<>());
            children.get(parent.get(nodeId)).add(nodeId);
            childrenT.computeIfAbsent(parent.get(nodeId), k -> new HashSet<>());
            childrenT.get(parent.get(nodeId)).add(nodeId);
        }
        if (!isLeaf) {
            if (isContainer) {
                tmpList.add(nodeId);
            }
            for (Node<ASTNode> child : node.getChildren()) {
                buildParentAndChildrenMaps(child);
            }
            if (isContainer) {
                tmpList.remove(tmpList.get(tmpList.size() - 1));
            }
        }
    }

    private void buildModifiesAndUsesMaps() {
        List<Node<ASTNode>> assignments = ast.getAssignments();
        List<Node<ASTNode>> procedures = ast.getProcedures();
        List<Node<ASTNode>> callNodes = ast.getCallNodes();

        for (Node<ASTNode> assignIt : assignments) {
            Node<ASTNode> assignNodeIt = assignIt.getChildren().iterator().next();
            String variable = assignNodeIt.getData().getParam(NodeParamType.NAME);
            int nodeId = assignIt.getData().getId();
            modifies.computeIfAbsent(nodeId, k -> new HashSet<>());
            modifies.get(nodeId).add(variable);
            buildUsesAssignment(assignIt, assignIt);
        }

        for (Node<ASTNode> procedureNode : procedures) {
            buildModifiesAndUsesContainers(procedureNode);
        }

        Set<Integer> tmpCalls = new HashSet<>();
        for (Node<ASTNode> callNode : callNodes) {
            tmpCalls.add(callNode.getData().getId());
        }
        while (!tmpCalls.isEmpty()) {
            buildModifiesAndUsesCalls(ASTNode.getNodeById(tmpCalls.iterator().next()), tmpCalls);
        }
    }

    private void buildUsesAssignment(Node<ASTNode> assignNode, Node<ASTNode> child) {
        if (child.getData().getNodeType() == NodeType.ASSIGN) {
            Iterator<Node<ASTNode>> it = child.getChildren().iterator();
            child = it.next();
            if (it.hasNext()) {
                child = it.next();
            }
        }
        for (Node<ASTNode> node : child.getChildren()) {
            if (node.getData().getNodeType() == NodeType.VARIABLE) {
                uses.computeIfAbsent(assignNode.getData().getId(), k -> new HashSet<>());
                uses.get(assignNode.getData().getId()).add(node.getData().getParam(NodeParamType.NAME));
            } else {
                buildUsesAssignment(assignNode, node);
            }
        }
    }

    private void buildModifiesAndUsesContainers(Node<ASTNode> container) {
        for (Node<ASTNode> stmtLstNode : container.getChildren()) {
            for (Node<ASTNode> stmtNode : stmtLstNode.getChildren()) {
                switch (stmtNode.getData().getNodeType()) {
                    case WHILE:
                    case IF:
                        uses.computeIfAbsent(stmtNode.getData().getId(), k -> new HashSet<>());
                        uses.get(stmtNode.getData().getId()).add(stmtNode.getData().getParam(NodeParamType.COND));
                    case PROCEDURE:
                        buildModifiesAndUsesContainers(stmtNode);
                    case ASSIGN:
                        modifies.computeIfAbsent(container.getData().getId(), k -> new HashSet<>());
                        if (modifies.get(stmtNode.getData().getId()) != null) {
                            for (String s : modifies.get(stmtNode.getData().getId())) {
                                modifies.get(container.getData().getId()).add(s);
                            }
                        }
                        uses.computeIfAbsent(container.getData().getId(), k -> new HashSet<>());
                        if (uses.get(stmtNode.getData().getId()) != null) {
                            for (String s : uses.get(stmtNode.getData().getId())) {
                                uses.get(container.getData().getId()).add(s);
                            }
                        }
                }
            }
        }
    }

    private void buildModifiesAndUsesCalls(Node<ASTNode> callNode, Set<Integer> tmpCalls) {
        tmpCalls.remove(callNode.getData().getId());
        if (tmpCalls.contains(callNode.getData().getId()) &&
                !callNode.getData().getParam(NodeParamType.CALLER).equals(callNode.getData().getParam(NodeParamType.CALLED_BY))) {
            buildModifiesAndUsesCalls(callNode, tmpCalls);
        }
        int caleeProcId = ast.getProcedureByName(callNode.getData().getParam(NodeParamType.CALLED_BY)).getData().getId();
        modifies.computeIfAbsent(callNode.getData().getId(), k -> new HashSet<>());
        for (String s : modifies.get(caleeProcId)) {
            modifies.get(callNode.getData().getId()).add(s);
        }
        uses.computeIfAbsent(callNode.getData().getId(), k -> new HashSet<>());
        for (String s : uses.get(caleeProcId)) {
            uses.get(callNode.getData().getId()).add(s);
        }

        Node<ASTNode> node = callNode;
        while (node.getData().getNodeType() != NodeType.PROCEDURE) {
            do {
                node = node.getParent();
            } while (node.getData().getNodeType() != NodeType.IF &&
                        node.getData().getNodeType() != NodeType.WHILE &&
                        node.getData().getNodeType() != NodeType.PROCEDURE);

            modifies.computeIfAbsent(node.getData().getId(), k -> new HashSet<>());
            for (String s : modifies.get(callNode.getData().getId())) {
                modifies.get(node.getData().getId()).add(s);
            }
            uses.computeIfAbsent(node.getData().getId(), k -> new HashSet<>());
            for (String s : uses.get(callNode.getData().getId())) {
                uses.get(node.getData().getId()).add(s);
            }
        }
    }

    private void buildNextStmtLst(Node<ASTNode> stmtLst, Node<ASTNode> next) {
        Iterator<Node<ASTNode>> it = stmtLst.getChildren().iterator();
        Node<ASTNode> prev = it.next();
        Node<ASTNode> node;
        while (it.hasNext()) {
            node = it.next();
            buildNext(prev, node);
            prev = node;
        }
        buildNext(prev, next);
    }

    private void buildNext(Node<ASTNode> prev, Node<ASTNode> next) {
        Node<ASTNode> childNode, childChildNode, prevNextChildNode, prevNextChildChildNode;
        int prevId = prev.getData().getId();
        switch (prev.getData().getNodeType()) {
            case IF:
                Iterator<Node<ASTNode>> prevIt = prev.getChildren().iterator();
                childNode = prevIt.next();
                childChildNode = childNode.getChildren().iterator().next();

                nextN.computeIfAbsent(prevId, k -> new HashSet<>());
                nextN.get(prevId).add(childChildNode.getData().getId());
                nextT.computeIfAbsent(prevId, k -> new HashSet<>());
                nextT.get(prevId).add(childChildNode.getData().getId());

                prevN.computeIfAbsent(childChildNode.getData().getId(), k -> new HashSet<>());
                prevN.get(childChildNode.getData().getId()).add(prevId);
                prevT.computeIfAbsent(childChildNode.getData().getId(), k -> new HashSet<>());
                prevT.get(childChildNode.getData().getId()).add(prevId);

                prevNextChildNode = prevIt.next();
                prevNextChildChildNode = prevNextChildNode.getChildren().iterator().next();
                nextN.computeIfAbsent(prevId, k -> new HashSet<>());
                nextN.get(prevId).add(prevNextChildChildNode.getData().getId());
                nextT.computeIfAbsent(prevId, k -> new HashSet<>());
                nextT.get(prevId).add(prevNextChildChildNode.getData().getId());

                prevN.computeIfAbsent(prevNextChildChildNode.getData().getId(), k -> new HashSet<>());
                prevN.get(prevNextChildChildNode.getData().getId()).add(prevId);
                prevT.computeIfAbsent(prevNextChildChildNode.getData().getId(), k -> new HashSet<>());
                prevT.get(prevNextChildChildNode.getData().getId()).add(prevId);

                buildNextStmtLst(childNode, next);
                buildNextStmtLst(prevNextChildNode, next);
                break;
            case WHILE:
                childNode = prev.getChildren().iterator().next();
                childChildNode = childNode.getChildren().iterator().next();

                nextN.computeIfAbsent(prevId, k -> new HashSet<>());
                nextN.get(prevId).add(childChildNode.getData().getId());
                nextT.computeIfAbsent(prevId, k -> new HashSet<>());
                nextT.get(prevId).add(childChildNode.getData().getId());

                prevN.computeIfAbsent(childChildNode.getData().getId(), k -> new HashSet<>());
                prevN.get(childChildNode.getData().getId()).add(prevId);
                prevT.computeIfAbsent(childChildNode.getData().getId(), k -> new HashSet<>());
                prevT.get(childChildNode.getData().getId()).add(prevId);

                buildNextStmtLst(childNode, prev);
            case ASSIGN:
            case CALL:
                if (next != null) {
                    int nextId = next.getData().getId();
                    nextN.computeIfAbsent(prevId, k -> new HashSet<>());
                    nextN.get(prevId).add(nextId);
                    nextT.computeIfAbsent(prevId, k -> new HashSet<>());
                    nextT.get(prevId).add(nextId);

                    prevN.computeIfAbsent(nextId, k -> new HashSet<>());
                    prevN.get(nextId).add(prevId);
                    prevT.computeIfAbsent(nextId, k -> new HashSet<>());
                    prevT.get(nextId).add(prevId);
                }
        }
    }

    private void buildPattern(List<Node<ASTNode>> assignments ) {
        for (Node<ASTNode> assignment : assignments) {
            Iterator<Node<ASTNode>> it = assignment.getChildren().iterator();
            it.next(); //variable name
            Node<ASTNode> exp = it.next();
            Node<ASTNode> expDetails = exp.getChildren().iterator().next();
            int assignmentId = assignment.getData().getId();
            fullPattern.put(assignmentId, buildPatternNode(expDetails, assignmentId));
        }
    }

    private String buildPatternNode(Node<ASTNode> exp, int id) {
        ASTNode expData = exp.getData();
        String val = expData.getParam(NodeParamType.NAME);
        String result = "";

        if (expData.getNodeType() == NodeType.VARIABLE || expData.getNodeType() == NodeType.CONSTANT) {
            result = val;
        } else {
            Iterator<Node<ASTNode>> it = exp.getChildren().iterator();
            for (int i = 0; i < 3; i++) {
                if (i == 1) {
                    result += val;
                    continue;
                }
                Node<ASTNode> elem = it.next();
                String x = buildPatternNode(elem, id);
                if (val.equals("*") && elem.getData().getParam(NodeParamType.NAME).equals("+")) {
                    x = "(" + x + ")";
                }
                result = result + x;
            }
        }

        pattern.computeIfAbsent(id, s -> new HashSet<>());
        pattern.get(id).add(result);
        return result;
    }

    private void buildTransientRelation(Map<Integer, Set<Integer>> relation) {
        boolean end = false;
        while (!end) {
            end = true;
            for (int a : relation.keySet()) {
                Iterator<Integer> bIt = relation.get(a).iterator();
                Set<Integer> set = new HashSet<>(relation.get(a));
                while (bIt.hasNext()) {
                    int b = bIt.next();
                    if (relation.get(b) != null) {
                        for (int c : relation.get(b)) {
                            if (set.add(c)) {
                                end = false;
                            }
                        }
                    }
                }
                relation.put(a, set);
            }
        }
    }

    private void buildInvertedVariableRelation(Map<Integer, Set<String>> rel, Map<String, Set<Integer>> inv) {
        rel.forEach((id, set) -> {
            set.forEach((val -> {
                inv.computeIfAbsent(val, s -> new HashSet<>());
                inv.get(val).add(id);
            }));
        });
    }
}
