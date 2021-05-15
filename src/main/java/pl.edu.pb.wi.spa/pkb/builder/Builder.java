package pl.edu.pb.wi.spa.pkb.builder;

import pl.edu.pb.wi.spa.common.AST;
import pl.edu.pb.wi.spa.pkb.PKB;
import pl.edu.pb.wi.spa.pkb.PKBImpl;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;
import pl.edu.pb.wi.spa.tree.NodeType;

import java.util.*;

public class Builder {
    private Set<String> variables;
    private Set<String> constants;
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

    private List<Integer> tempUiVector = new ArrayList<>();
    private AST ast;

    public Builder(AST ast) {
        this.ast = ast;
        variables = new HashSet<>();
        constants = new HashSet<>();
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
    }

    public PKB buildPKB() {
        initializeCallMaps();
        initializeParentMap(ast.getAstTreeAsList().get(0));
        initializeModifiesAndUsesMaps();
        for (Node<ASTNode> procedure : ast.getProcedures()) {
            Node<ASTNode> procedureChild = procedure.getChildren().iterator().next();
            initializeNextStmtLst(procedureChild, null);
        }
        initializeTransientRelation(nextT);
        initializeTransientRelation(callersT);
        initializeTransientRelation(calleesT);
        initializeTransientRelation(childrenT);
        initializeInvertedVariableRelation(modifies, modified);
        initializeInvertedVariableRelation(uses, used);
        initializeTransientRelation(prevT);
        initializePattern(ast.getAssignments());

        for (Node<ASTNode> varNode : ast.getVarNodes()) {
            variables.add(varNode.getData().getParam(NodeParamType.NAME));
        }
        for (Node<ASTNode> constantNode : ast.getConstantNodes()) {
            constants.add(constantNode.getData().getParam(NodeParamType.NAME));
        }

        return new PKBImpl(callers,
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
                variables,
                constants,
                ast);
    }

    private void initializeCallMaps() {
        List<Node<ASTNode>> callNodes = ast.getCallNodes();
        for (Node<ASTNode> callNode : callNodes) {
            ASTNode node = callNode.getData();
            ASTNode caller = ast.getProcedureByName(node.getParam(NodeParamType.CALLER)).getData();
            ASTNode callee = ast.getProcedureByName(node.getParam(NodeParamType.CALLEE)).getData();

            callers.computeIfAbsent(caller.getId(), k -> new HashSet<>());
            callers.get(caller.getId()).add(callee.getId());
            callees.computeIfAbsent(callee.getId(), k -> new HashSet<>());
            callees.get(callee.getId()).add(caller.getId());
            //*
            callersT.computeIfAbsent(caller.getId(), k -> new HashSet<>());
            callersT.get(caller.getId()).add(callee.getId());
            calleesT.computeIfAbsent(callee.getId(), k -> new HashSet<>());
            calleesT.get(callee.getId()).add(caller.getId());
        }
    }

    private void initializeParentMap(Node<ASTNode> node) {
        NodeType nodeType = node.getData().getNodeType();
        boolean isLeafNode = nodeType == NodeType.CALL || nodeType == NodeType.ASSIGN;
        boolean isContainerNode = nodeType == NodeType.WHILE || nodeType == NodeType.IF;
        boolean isStmt = isLeafNode || isContainerNode;

        int nodeId = node.getData().getId();
        if (isStmt && !tempUiVector.isEmpty()) {
            parent.put(nodeId, tempUiVector.get(tempUiVector.size()-1)); //(*parent)[(*node)->id] = tempUiVector.back();
            parentT.computeIfAbsent(nodeId, k -> new HashSet<>());
            for (Integer id : tempUiVector) {
                parentT.get(nodeId).add(id); //(*parentT)[(*node)->id].insert(tempUiVector.begin(), tempUiVector.end());
            }
            children.computeIfAbsent(parent.get(nodeId), k -> new HashSet<>());
            children.get(parent.get(nodeId)).add(nodeId); //(*children)[(*parent)[(*node)->id]].insert((*node)->id);
            childrenT.computeIfAbsent(parent.get(nodeId), k -> new HashSet<>());
            childrenT.get(parent.get(nodeId)).add(nodeId); //(*childrenT)[(*parent)[(*node)->id]].insert((*node)->id);
        }
        if (!isLeafNode) {
            if (isContainerNode) {
                tempUiVector.add(nodeId);
            }
            for (Node<ASTNode> child : node.getChildren()) {
                initializeParentMap(child);
            }
            if (isContainerNode) {
                tempUiVector.remove(tempUiVector.get(tempUiVector.size() - 1)); // tempUiVector.pop_back();
            }
        }
    }

    private void initializeModifiesAndUsesMaps() {
        List<Node<ASTNode>> assignments = ast.getAssignments();
        List<Node<ASTNode>> procedures = ast.getProcedures();
        List<Node<ASTNode>> callNodes = ast.getCallNodes();

        for (Node<ASTNode> assignIt : assignments) {
            Node<ASTNode> assignNodeIt = assignIt.getChildren().iterator().next();
            String variable = assignNodeIt.getData().getParam(NodeParamType.NAME);
            int nodeId = assignIt.getData().getId();
            modifies.computeIfAbsent(nodeId, k -> new HashSet<>());
            modifies.get(nodeId).add(variable);// (*modifies)[(**asgnIt)->id].insert(variable);
            initializeUsesAssignment(assignIt, assignIt);
        }

        Set<Integer> callsToInit = new HashSet<>();
        for (Node<ASTNode> procedureNode : procedures) {
            initializeModifiesAndUsesContainers(procedureNode);
        }
        for (Node<ASTNode> callNode : callNodes) {
            callsToInit.add(callNode.getData().getId());
        }
        while (!callsToInit.isEmpty())
        {
            initializeModifiesAndUsesCalls(ASTNode.getNodeById(callsToInit.iterator().next()), callsToInit);
        }
    }

    private void initializeUsesAssignment(Node<ASTNode> assignNode, Node<ASTNode> child) {
        if (child.getData().getNodeType() == NodeType.ASSIGN) {
            Iterator<Node<ASTNode>> it = child.getChildren().iterator();
            child = it.next();
            if (it.hasNext()) {
                child = it.next();
            }
        }
        for (Node<ASTNode> node : child.getChildren()) {
            switch (node.getData().getNodeType()) {
                case VARIABLE:
                    uses.computeIfAbsent(assignNode.getData().getId(), k -> new HashSet<>());
                    uses.get(assignNode.getData().getId()).add(node.getData().getParam(NodeParamType.NAME)); //(*uses)[(*assignment)->id].insert((*it)->getParam(Name));
                    break;
                default:
                    initializeUsesAssignment(assignNode, node);
                    break;
            }
        }
    }

    private void initializeModifiesAndUsesContainers(Node<ASTNode> container) {
        for (Node<ASTNode> stmtLstNode : container.getChildren()) {
            for (Node<ASTNode> stmtNode : stmtLstNode.getChildren()) {
                switch (stmtNode.getData().getNodeType()) {
                    case WHILE:
                    case IF:
                        uses.computeIfAbsent(stmtNode.getData().getId(), k -> new HashSet<>());
                        uses.get(stmtNode.getData().getId()).add(stmtNode.getData().getParam(NodeParamType.COND)); //(*uses)[(*stmtIt)->id].insert((*stmtIt)->getParam(Cond));
                    case PROCEDURE:
                        initializeModifiesAndUsesContainers(stmtNode);
                    case ASSIGN:
                        modifies.computeIfAbsent(container.getData().getId(), k -> new HashSet<>());
                        if (modifies.get(stmtNode.getData().getId()) != null) { //TODO dorobić więcej ifów tego typu
                            for (String s : modifies.get(stmtNode.getData().getId())) {
                                modifies.get(container.getData().getId()).add(s); //(*modifies)[(*container)->id].insert((*modifies)[(*stmtIt)->id].begin(), (*modifies)[(*stmtIt)->id].end());
                            }
                        }
                        uses.computeIfAbsent(container.getData().getId(), k -> new HashSet<>());
                        if (uses.get(stmtNode.getData().getId()) != null) { //TODO dorobić więcej ifów tego typu
                            for (String s : uses.get(stmtNode.getData().getId())) {
                                uses.get(container.getData().getId()).add(s); //(*uses)[(*container)->id].insert((*uses)[(*stmtIt)->id].begin(), (*uses)[(*stmtIt)->id].end());
                            }
                        }
                }
            }
        }
    }

    private void initializeModifiesAndUsesCalls(Node<ASTNode> call, Set<Integer> callsToInit) {
        callsToInit.remove(call.getData().getId());
        if (callsToInit.contains(call.getData().getId()) &&
                !call.getData().getParam(NodeParamType.CALLER).equals(call.getData().getParam(NodeParamType.CALLEE))) {
            initializeModifiesAndUsesCalls(call, callsToInit);
        }
        int caleeProcId = ast.getProcedureByName(call.getData().getParam(NodeParamType.CALLEE)).getData().getId();
        modifies.computeIfAbsent(call.getData().getId(), k -> new HashSet<>());
        for (String s : modifies.get(caleeProcId)) {
            modifies.get(call.getData().getId()).add(s); //(*modifies)[(*call)->id].insert((*modifies)[caleeProcId].begin(), (*modifies)[caleeProcId].end());
        }
        uses.computeIfAbsent(call.getData().getId(), k -> new HashSet<>());
        for (String s : uses.get(caleeProcId)) {
            uses.get(call.getData().getId()).add(s); //(*uses)[(*call)->id].insert((*uses)[caleeProcId].begin(), (*uses)[caleeProcId].end());
        }

        Node<ASTNode> node = call;
        while (node.getData().getNodeType() != NodeType.PROCEDURE) {
            do {
                node = node.getParent();
            } while (node.getData().getNodeType() != NodeType.IF &&
                        node.getData().getNodeType() != NodeType.WHILE &&
                        node.getData().getNodeType() != NodeType.PROCEDURE);

            modifies.computeIfAbsent(node.getData().getId(), k -> new HashSet<>());
            for (String s : modifies.get(call.getData().getId())) {
                modifies.get(node.getData().getId()).add(s); //(*modifies)[(*node)->id].insert((*modifies)[(*call)->id].begin(), (*modifies)[(*call)->id].end());
            }
            uses.computeIfAbsent(node.getData().getId(), k -> new HashSet<>());
            for (String s : uses.get(call.getData().getId())) {
                uses.get(node.getData().getId()).add(s); //(*uses)[(*node)->id].insert((*uses)[(*call)->id].begin(), (*uses)[(*call)->id].end());
            }

        }
    }

    private void initializeNextStmtLst(Node<ASTNode> stmtLst, Node<ASTNode> next) {
        Iterator<Node<ASTNode>> it = stmtLst.getChildren().iterator();
        Node<ASTNode> prev = it.next();
        Node<ASTNode> n;
        while (it.hasNext()) {
            n = it.next();
            initializeNext(prev, n);
            prev = n;
        }
        initializeNext(prev, next);
    }

    private void initializeNext(Node<ASTNode> prev, Node<ASTNode> next) {
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

                initializeNextStmtLst(childNode, next);
                initializeNextStmtLst(prevNextChildNode, next);
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

                initializeNextStmtLst(childNode, prev);
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

    private void initializePattern(List<Node<ASTNode>> assignments ) {
        for (Node<ASTNode> assignment : assignments) {
            Iterator<Node<ASTNode>> it = assignment.getChildren().iterator();
            it.next(); //variable name
            Node<ASTNode> exp = it.next();
            Node<ASTNode> expDetails = exp.getChildren().iterator().next();
            int assignmentId = assignment.getData().getId();
            fullPattern.put(assignmentId, initializePatternNode(expDetails, assignmentId));
        }
    }

    private String initializePatternNode(Node<ASTNode> exp, int id) {
        ASTNode expData = exp.getData();
        String val = expData.getParam(NodeParamType.NAME);
        String result = "";

        if (expData.getNodeType() == NodeType.VARIABLE || expData.getNodeType() == NodeType.CONSTANT) {
            result = val;
        } else {
            Iterator<Node<ASTNode>> it = exp.getChildren().iterator();
            for (int i = 0; i < 3; ++i) {
                if (i == 1) {
                    result += val;
                    continue;
                }
                Node<ASTNode> elem = it.next();
                String c = initializePatternNode(elem, id);
                if (val.compareTo("*") == 0 && elem.getData().getParam(NodeParamType.NAME).compareTo("+") == 0) {
                    c = "(" + c + ")";
                }
                result += c;
            }
        }

        pattern.computeIfAbsent(id, s -> new HashSet<>());
        pattern.get(id).add(result);
        return result;
    }

    private void initializeTransientRelation(Map<Integer, Set<Integer>> relation) {
        boolean finished = false;
        while (!finished) {
            finished = true;
            Iterator<Integer> aIt = relation.keySet().iterator();
            while (aIt.hasNext()) {
                int a = aIt.next();
                Iterator<Integer> bIt = relation.get(a).iterator();
                Set<Integer> set = new HashSet<>(relation.get(a));
                while (bIt.hasNext()) {
                    int b = bIt.next();
                    if (relation.get(b) != null) {
                        Iterator<Integer> cIt = relation.get(b).iterator();
                        while (cIt.hasNext()) {
                            int c = cIt.next();
                            if (set.add(c)) {
                                finished = false;
                            }
                        }
                    }
                }
                relation.put(a, set);
            }
        }
    }

    private void initializeInvertedVariableRelation(Map<Integer, Set<String>> rel,
                                                    Map<String, Set<Integer>> inv) {
        rel.forEach((id, set) -> {
            set.forEach((val -> {
                inv.computeIfAbsent(val, s -> new HashSet<>());
                inv.get(val).add(id);
            }));
        });
    }
}
