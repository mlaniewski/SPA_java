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
    private List<Node<ASTNode>> assignments;
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
    private List<Integer> tempUiVector;

    public Builder() {
        ASTNode n = new ASTNode(NodeType.PROGRAM);
        astTree = Collections.singletonList(new Node<>(n));
        procedures = new ArrayList<>();
        whiles = new ArrayList<>();
        ifs = new ArrayList<>();
        assignments = new ArrayList<>();
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
                assignments,
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
                assignments.add(node);
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

    public Node<ASTNode> getAstTree() {
        return astTree.get(0);
    }

    //TODO to tak naprawde budowa PKB
    public AST getAST() {
        List<Node<ASTNode>> list = getAstTreeAsList();

        addLineNumbers(list);
        initializeCallMaps();
        tempUiVector = new ArrayList<>();
        initializeParentMap(list.get(0));
        initializeModifiesAndUsesMaps();
        for (Node<ASTNode> procedure : procedures) {
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

        System.out.println();
        return null;
    }

    private void addLineNumbers(List<Node<ASTNode>> list) {
        int lineNumber = 0;
        for (Node<ASTNode> node : list) {
            ASTNode astNode = node.getData();
            switch (astNode.getNodeType()) {
                case CALL:
                case WHILE:
                case IF:
                case ASSIGN:
                    astNode.setLineNumber(++lineNumber);
                    programLines.add(node);
            }
        }
    }

    private void initializeCallMaps() {
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

    private List<Node<ASTNode>> getAstTreeAsList() {
        List<Node<ASTNode>> list = new ArrayList<>();
        Node<ASTNode> astTree = getAstTree();
        list.add(astTree);
        getNextNode(astTree.getChildren().get(0), list);
        return list;
    }

    private void getNextNode(Node<ASTNode> node, List<Node<ASTNode>> list) {
        list.add(node);
        for (Node<ASTNode> astNode : node.getChildren()) {
            getNextNode(astNode, list);
        }
    }
}
