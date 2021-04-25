package pl.edu.pb.wi.spa.solver;

import pl.edu.pb.wi.spa.ast.AST;
import pl.edu.pb.wi.spa.common.Predicate;
import pl.edu.pb.wi.spa.common.With;
import pl.edu.pb.wi.spa.common.Closure;
import pl.edu.pb.wi.spa.common.Selector;
import pl.edu.pb.wi.spa.common.Pattern;
import pl.edu.pb.wi.spa.common.ClosureResult;
import pl.edu.pb.wi.spa.exception.PKBException;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;
import pl.edu.pb.wi.spa.tree.NodeType;

import java.util.*;

public class Solver {
    private List<String> results;
    private Selector selector;
    private List<Closure> closureTable;
    private List<Pattern> patternTable;
    private List<Predicate> predTable;
    private List<With> withTable;
    private AST ast;

    private boolean boolResult;
    private String[] tmpResult = new String[1000]; //TODO to jest podejrzane
    private List<String> valueOfPred = new ArrayList<>();
    private Map<String, Integer> indexOfPred = new HashMap<>();
    private List<ClosureResult> closureResults = new ArrayList<>();
    private Map<Integer, List<ClosureResult>> dependentClosures = new HashMap<>();
    private Map<Integer, Set<String>> possibleValues = new HashMap<>();
    private List<List<String>> resultTable = new LinkedList<>();

    public Solver(List<String> results, Selector selector, List<Closure> closureTable, List<Pattern> patternTable, List<Predicate> predTable, List<With> withTable, AST ast) throws PKBException {
        this.results = results;
        this.selector = selector;
        this.closureTable = closureTable;
        this.patternTable = patternTable;
        this.predTable = predTable;
        this.withTable = withTable;
        this.ast = ast;

        int i = 0;
        for (Predicate predicate : predTable) {
            valueOfPred.add(predicate.getValue());
            indexOfPred.put(predicate.getValue(), i++);
        }
        for (Closure closure : closureTable) {
            ClosureResult result = getClosureResult(closure);
            if (result.getResultType().equals("MAP") && result.getPq().isEmpty()) {
                continue;
            }
            closureResults.add(result);
        }
        for (Pattern pattern : patternTable) {
            closureResults.add(getPatternResult(pattern));
        }
        for (With with : withTable) {
            closureResults.add(getWithResult(with));
        }
        for (ClosureResult cr : closureResults) {
            if (cr.getResultType().equals("MAP")) {
                int idx = Math.max(indexOfPred.get(cr.getP()), indexOfPred.get(cr.getQ()));
                dependentClosures.computeIfAbsent(idx, k -> new LinkedList<>());
                dependentClosures.get(idx).add(cr);
            }
        }
    }

    public void evaluate() {
        boolean foundFalseResult = false;
        boolean foundSetOrMap = false;
        for (ClosureResult cr : closureResults) {
            if (cr.getResultType().equals("BOOL") && !cr.isBoolResult()) {
                foundFalseResult = true;
                break;
            }
            if (!cr.getResultType().equals("BOOL")) {
                foundSetOrMap = true;
            }
        }
        if (!foundFalseResult) {
            findPossibleValues();
            if (foundSetOrMap) {
                findResult(0);
            }
            else {
                boolResult = true;
            }
        }

        if (selector.getType().equals("boolean")) {
            if (resultTable.isEmpty() && !boolResult) {
                results.add("false");
            } else {
                results.add("true");
            }
        } else if (selector.getType().equals("variable")) {
            Set<String> res = new HashSet<>();
            for (List<String> it : resultTable) {
                res.add(it.get(indexOfPred.get(selector.getVariables().get(0))));
            }
            results.addAll(res);
        } else if (selector.getType().equals("tuple")) { //TODO robimy tuple czy nie?

        }
    }

    //TODO to moze byc nie do konca poprawne
    private void findResult(int pred) {
        if (pred >= possibleValues.size()) { // to jest do wywalenia jak beda juz ClosureResult zrobione
            return;
        }
        if (possibleValues.get(pred) != null) {
            return;
        }
        for (String val : possibleValues.get(pred)) {
            boolean foundValue = true;
            if (dependentClosures.get(pred) != null) {
                for (ClosureResult cr : dependentClosures.get(pred)) {
                    String depPred = cr.getP().equals(valueOfPred.get(pred)) ? cr.getP() : cr.getQ();
                    Map<String, Set<String>> map = cr.getP().equals(valueOfPred.get(pred)) ? cr.getQp() : cr.getPq();
                    if (map.get(val) != null) {
                        if (map.get(val).contains(String.valueOf(tmpResult[indexOfPred.get(depPred)]))) {
                            foundValue = false;
                            break;
                        }
                    }
                }
            }
            if (foundValue) {
                tmpResult[pred] = val;
                if (pred < predTable.size() - 1) {
                    findResult(pred + 1);
                } else {
                    List<String> result = new ArrayList<>();
                    for (int i = 0; i < predTable.size(); i++) {
                        result.add(tmpResult[i]);
                    }
                    resultTable.add(result);
                }
            }
        }
    }

    private void findPossibleValues() {
        Set<String> tmpValues = new HashSet<>();
        Set<Integer> updatedPreds = new HashSet<>();

        if (closureResults.isEmpty()) {
            int i = 0;
            for (Predicate pred : predTable) {
                List<Node<ASTNode>> allVals = getAllValues(pred.getType());
                for (Node<ASTNode> val : allVals) {
                    possibleValues.computeIfAbsent(i, k -> new HashSet<>());
                    possibleValues.get(i).add(nodeToString(val));
                }
                i++;
            }
        }

        for (ClosureResult cr : closureResults) {
            int pred;
            if (cr.getResultType().equals("SET")) {
                pred = indexOfPred.get(cr.getP());
                updatePossibleValues(pred, cr.getVals(), updatedPreds.contains(pred));
                updatedPreds.add(pred);
            } else if (cr.getResultType().equals("MAP")) {
                for (int i = 0; i < 2; i++) {
                    tmpValues.clear();
                    pred = indexOfPred.get(i == 0 ? cr.getP() : cr.getQ());
                    Map<String, Set<String>> m = i == 0 ? cr.getPq() : cr.getQp();
                    m.forEach((k, v) -> {
                        tmpValues.add(k);
                    });
                    updatePossibleValues(pred, tmpValues, updatedPreds.contains(pred));
                    updatedPreds.add(pred);
                }
            }
        }

    }

    private void updatePossibleValues(int pred, Set<String> vals, boolean updated) {
        if (updated) {
            Set<String> intersection = new HashSet<>();
            for (String el : vals) {
                if (possibleValues.get(pred).contains(el)) {
                    intersection.add(el);
                }
            }
        }
        else {
            possibleValues.computeIfAbsent(pred, k -> new HashSet<>());
            possibleValues.get(pred).addAll(vals);
        }
    }

    private ClosureResult getClosureResult(Closure closure) throws PKBException {
        ClosureResult closureResult = new ClosureResult();
        boolean _transient = closure.getType().endsWith("*");
        String relation = _transient ? closure.getType().substring(0, closure.getType().length() - 1) : closure.getType();

        Predicate p1 = new Predicate();
        Predicate p2 = new Predicate();
        for (Predicate predicate : predTable) {
            if (closure.getLhs().equals(predicate.getValue())) {
                p1 = predicate;
            } else if (closure.getRhs().equals(predicate.getValue())) {
                p2 = predicate;
            }
        }

        // sprawdzam czy lhs i rhs sa numerami linii
        int lhsLineNum = 0, rhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
        } catch (NumberFormatException e) { }
        try {
            rhsLineNum = Integer.valueOf(closure.getRhs());
        } catch (NumberFormatException e) { }

        // sprawdzam czy lhs i rhs sa nazwami
        String lhsName = "", rhsName = "";
        if (closure.getLhs().startsWith("\"")) {
            lhsName = closure.getLhs().substring(1, closure.getLhs().length() - 1);
        }
        if (closure.getRhs().startsWith("\"")) {
            rhsName = closure.getRhs().substring(1, closure.getRhs().length() - 1);
        }

        if (p1.getType().isEmpty() && p2.getType().isEmpty()) { // 0 predykatow
            closureResult.setResultType("BOOL");
            if (relation.equals("next")) {
                if (lhsLineNum != 0 && rhsLineNum != 0) {
                    closureResult.setBoolResult(ast.checkNext(ast.getStmtByLineNumber(lhsLineNum), ast.getStmtByLineNumber(rhsLineNum), _transient));
                } else if (lhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getNext(ast.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
                } else if (rhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getPrev(ast.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = getAllValues("statement");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!ast.getNext(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("follows")) {
                if (lhsLineNum != 0 && rhsLineNum != 0) {
                    closureResult.setBoolResult(ast.checkFollows(ast.getStmtByLineNumber(lhsLineNum), ast.getStmtByLineNumber(rhsLineNum), _transient));
                } else if (lhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getFollowing(ast.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
                } else if (rhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getFollowed(ast.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = getAllValues("statement");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!ast.getFollowing(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (lhsLineNum != 0 && rhsLineNum != 0) {
                    closureResult.setBoolResult(ast.checkParent(ast.getStmtByLineNumber(lhsLineNum), ast.getStmtByLineNumber(rhsLineNum), _transient));
                } else if (lhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getChildren(ast.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
                } else if (rhsLineNum != 0) {
                    closureResult.setBoolResult(!ast.getParent(ast.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = getAllValues("statement");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!ast.getParent(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!lhsName.isEmpty() && !rhsName.isEmpty()) {
                    closureResult.setBoolResult(ast.checkCalls(ast.getProcedureByName(lhsName), ast.getProcedureByName(rhsName), _transient));
                } else if (!lhsName.isEmpty()) {
                    closureResult.setBoolResult(!ast.getCallees(ast.getProcedureByName(lhsName), _transient).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!ast.getCallers(ast.getProcedureByName(rhsName), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = getAllValues("procedure");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!ast.getCallees(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if ((lhsLineNum != 0 || !lhsName.isEmpty()) && !rhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    closureResult.setBoolResult(ast.checkUses(n, rhsName));
                } else if (lhsLineNum != 0 || !lhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    closureResult.setBoolResult(!ast.getUsed(n).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!ast.getUsing(rhsName).isEmpty());
                } else {
                    List<String> vars = getAllVariables();
                    closureResult.setBoolResult(false);
                    for (String var : vars) {
                        if (!ast.getUsing(var).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if ((lhsLineNum != 0 || !lhsName.isEmpty()) && !rhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    closureResult.setBoolResult(ast.checkModifies(n, rhsName));
                } else if (lhsLineNum != 0 || !lhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    closureResult.setBoolResult(!ast.getModified(n).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!ast.getModifying(rhsName).isEmpty());
                } else {
                    List<String> vars = getAllVariables();
                    closureResult.setBoolResult(false);
                    for (String var : vars) {
                        if (!ast.getModifying(var).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
        }
        else if (p2.getType().isEmpty()) { // predykat z lewej
            closureResult.setResultType("SET");
            closureResult.setP(p1.getValue());
            if (relation.equals("next")) {
                if (rhsLineNum != 0) { // numer linii
                    //469
                    List<Node<ASTNode>> results = filterNodesByType(ast.getPrev(ast.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getPrev(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("follows")) {
                if (rhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getFollowed(ast.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getFollowed(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (rhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getParent(ast.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getParent(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!rhsName.isEmpty()) { // nazwa procedury
                    List<Node<ASTNode>> results = filterNodesByType(ast.getCallers(ast.getProcedureByName(rhsName), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("procedure");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getCallers(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if (!rhsName.isEmpty()) { // zmienna
                    List<Node<ASTNode>> results = filterNodesByType(ast.getUsing(rhsName), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<String> allVars = getAllVariables();
                    for (String var : allVars) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getUsing(var), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if (!rhsName.isEmpty()) { // zmienna
                    List<Node<ASTNode>> results = filterNodesByType(ast.getModifying(rhsName), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<String> allVars = getAllVariables();
                    for (String var : allVars) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getModifying(var), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }

                }
            }
        }
        else if (p1.getType().isEmpty()) { // predykat z prawej
            closureResult.setResultType("SET");
            closureResult.setP(p2.getValue());
            if (relation.equals("next")) {
                if (lhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getNext(ast.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getNext(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("follows")) {
                if (lhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getFollowing(ast.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getFollowing(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (lhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getChildren(ast.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = filterNodesByType(getAllValues("statement"), "if", "while");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getChildren(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!lhsName.isEmpty()) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(ast.getCallees(ast.getProcedureByName(lhsName), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = getAllValues("procedure");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(ast.getCallees(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if (lhsLineNum != 0 || !lhsName.isEmpty()) { // stmt lub proc
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    List<String> results = ast.getUsed(n);
                    for (String res : results) {
                        closureResult.addValue(res);
                    }
                } else { // _
                    List<Node<ASTNode>> procNodes = getAllValues("procedure");
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    allVals.addAll(procNodes);
                    for (Node<ASTNode> val : allVals) {
                        List<String> results = ast.getUsed(val);
                        for (String res : results) {
                            closureResult.addValue(res);
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if (lhsLineNum != 0 || !lhsName.isEmpty()) { // stmt lub proc
                    Node<ASTNode> n = lhsLineNum != 0 ? ast.getStmtByLineNumber(lhsLineNum) : ast.getProcedureByName(lhsName);
                    List<String> results = ast.getModified(n);
                    for (String res : results) {
                        closureResult.addValue(res);
                    }
                } else { // _
                    List<Node<ASTNode>> procNodes = getAllValues("procedure");
                    List<Node<ASTNode>> allVals = getAllValues("statement");
                    allVals.addAll(procNodes);
                    for (Node<ASTNode> val : allVals) {
                        List<String> results = ast.getModified(val);
                        for (String res : results) {
                            closureResult.addValue(res);
                        }
                    }
                }
            }
        }
        else { // 2 predykaty
            closureResult.setResultType("MAP");
            closureResult.setP(p1.getValue());
            closureResult.setQ(p2.getValue());
            if (relation.equals("next")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(getAllValues("statement"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(ast.getNext(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(getAllValues("statement"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(ast.getPrev(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("follows")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(getAllValues("statement"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(ast.getFollowing(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(getAllValues("statement"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(ast.getFollowed(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("parent")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(filterNodesByType(getAllValues("statement"), "if", "while"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(ast.getChildren(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(getAllValues("statement"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>>  qResults = filterNodesByType(ast.getParent(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("calls")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(getAllValues("procedure"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(ast.getCallees(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(getAllValues("procedure"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(ast.getCallers(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("uses")) {
                List<Node<ASTNode>> procNodes = getAllValues("procedure");
                List<Node<ASTNode>> allVals = getAllValues("statement");
                allVals.addAll(procNodes);

                List<Node<ASTNode>> allPVals = filterNodesByType(allVals, p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<String> pResults = ast.getUsed(val);
                    for (String r : pResults) {
                        closureResult.addPq(nodeToString(val), r);
                    }
                }
                List<String> allQVals = getAllVariables();
                for (String val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(ast.getUsing(val), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(val, nodeToString(r));
                    }
                }
            }
            if (relation.equals("modifies")) {
                List<Node<ASTNode>> procNodes = getAllValues("procedure");
                List<Node<ASTNode>> allVals = getAllValues("statement");
                allVals.addAll(procNodes);

                List<Node<ASTNode>> allPVals = filterNodesByType(allVals, p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<String> pResults = ast.getModified(val);
                    for (String r : pResults) {
                        closureResult.addPq(nodeToString(val), r);
                    }
                }
                List<String> allQVals = getAllVariables();
                for (String val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(ast.getModifying(val), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(val, nodeToString(r));
                    }
                }
            }
        }

        return closureResult;
    }

    private ClosureResult getPatternResult(Pattern pattern) {
        ClosureResult result = new ClosureResult();
        List<Node<ASTNode>> nodes = ast.getPattern(pattern.getLhs(), pattern.getRhs());
        if (nodes.isEmpty()) {
            result.setResultType("BOOL");
            result.setBoolResult(false);
        } else {
            result.setResultType("SET");
            result.setP(pattern.getVarName());
            for (Node<ASTNode> node : nodes) {
                result.addValue(String.valueOf(ast.getLineNumber(node)));
            }
        }

        return result;
    }

    private ClosureResult getWithResult(With with) {
        ClosureResult closureResult = new ClosureResult();
        if (with.getLhsPropertyName().equals(with.getRhsPropertyName())) {
            closureResult.setResultType("MAP");
            closureResult.setP(with.getLhsVarName());
            closureResult.setQ(with.getRhsVarName());
            Set<String> allVals = getAllPropertyValues(with.getLhsPropertyName());
            for (String val : allVals) {
                closureResult.addPq(val, val);
                closureResult.addQp(val, val);
            }
        } else if ((with.getLhsPropertyName().equals("value") && with.getRhsPropertyName().equals("stmt")) ||
                (with.getLhsPropertyName().equals("stmt") && with.getRhsPropertyName().equals("value"))) {
            closureResult.setResultType("MAP");
            closureResult.setP(with.getLhsVarName());
            closureResult.setQ(with.getRhsVarName());
            Set<String> allVals = new HashSet<>();
            Set<String> lhsVals = getAllPropertyValues(with.getLhsPropertyName());
            Set<String> rhsVals = getAllPropertyValues(with.getRhsPropertyName());
            for (String el : lhsVals) {
                if (rhsVals.contains(el)) {
                    allVals.add(el);
                }
            }
            for (String val : allVals) {
                closureResult.addPq(val, val);
                closureResult.addQp(val, val);
            }
        } else if (with.getLhsPropertyName().equals("value")) {
            closureResult.setResultType("BOOLEAN");
            closureResult.setBoolResult(false);
            Set<String> constants = ast.getConstants();
            for (String c : constants) {
                if (c.equals(with.getRhsVarName())) {
                    closureResult.setBoolResult(true);
                    break;
                }
            }
        } else {
            closureResult.setResultType("SET");
            closureResult.setP(with.getLhsVarName());
            if (with.getRhsVarName().startsWith("\"")) {
                closureResult.addValue(with.getRhsVarName().substring(1, with.getRhsVarName().length() - 2));
            }
            else {
                closureResult.addValue(with.getRhsVarName());
            }
        }

        return closureResult;
    }

    private Set<String> getAllPropertyValues(String propName) {
        Set<String> result = new HashSet<>();
        if (propName.equals("stmt")) {
            int lines = ast.getProgramLines().size();
            for (int i = 1; i <= lines; i++) {
                result.add(String.valueOf(i));
            }
        } else if (propName.equals("value")) {
            result.addAll(ast.getConstants());
        } else if (propName.equals("procName")) {
            List<Node<ASTNode>> procedures = ast.getProcedures();
            for (Node<ASTNode> p : procedures) {
                result.add(p.getData().getParam(NodeParamType.NAME));
            }
        } else if (propName.equals("varName")) {
            result.addAll(ast.getVariables());
        }

        return result;
    }

    private String nodeToString(Node<ASTNode> node) {
        switch (node.getData().getNodeType()) {
            case PROCEDURE:
            case VARIABLE:
            case CONSTANT:
                return node.getData().getParam(NodeParamType.NAME);
            default:
                return String.valueOf(ast.getLineNumber(node));
        }
    }
    private List<Node<ASTNode>> filterNodesByType(List<Node<ASTNode>> nodes, String type) {
        return filterNodesByType(nodes, type, "");
    }

    private List<Node<ASTNode>> filterNodesByType(List<Node<ASTNode>> nodes, String type, String type2) {
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

    private List<String> getAllVariables() {
        return new ArrayList<>(ast.getVariables());
    }

    private List<Node<ASTNode>> getAllValues(String predType) {
        List<Node<ASTNode>> result = new ArrayList<>();
        List<List<Node<ASTNode>>> nodeTypes = new ArrayList<>();
        if (predType.equals("procedure")) {
            nodeTypes.add(ast.getProcedures());
        } else if (predType.equals("statement")) {
            nodeTypes.add(ast.getWhiles());
            nodeTypes.add(ast.getIfs());
            nodeTypes.add(ast.getAssigments());
            nodeTypes.add(ast.getCallNodes());
        } else if (predType.equals("while")) {
            nodeTypes.add(ast.getWhiles());
        } else if (predType.equals("if")) {
            nodeTypes.add(ast.getIfs());
        }
        else if (predType.equals("call")) {
            nodeTypes.add(ast.getCallNodes());
        } else if (predType.equals("assign")) {
            nodeTypes.add(ast.getAssigments());
        }

        for (List<Node<ASTNode>> nodeType : nodeTypes) {
            result.addAll(nodeType);
        }
        return result;
    }
}
