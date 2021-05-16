package spa.query.evaluator;

import spa.common.*;
import spa.exception.PKBException;
import spa.pkb.PKB;
import spa.query.parser.QueryTree;
import spa.relation.Next;
import spa.relation.RelationResultEvaluator;
import spa.tree.ASTNode;
import spa.tree.Node;
import spa.tree.NodeParamType;
import spa.tree.NodeType;

import java.util.*;

public class QueryEvaluator {
    private List<String> results;
    private QueryTree queryTree;
    private PKB pkb;

    private boolean boolResult;
    private String[] tmpResult;
    private List<String> valueOfPred;
    private Map<String, Integer> indexOfPred;
    private List<ClosureResult> closureResults;
    private Map<Integer, List<ClosureResult>> dependentClosures;
    private Map<Integer, Set<String>> possibleValues;
    private List<List<String>> resultTable;
    //relations
    private RelationResultEvaluator next;

    public QueryEvaluator(QueryTree queryTree, PKB pkb) {
        this.queryTree = queryTree;
        this.pkb = pkb;
        this.results = new ArrayList<>();
        this.tmpResult = new String[1000];
        this.valueOfPred = new ArrayList<>();
        this.indexOfPred = new HashMap<>();
        this.closureResults = new ArrayList<>();
        this.dependentClosures = new HashMap<>();
        this.possibleValues = new HashMap<>();
        this.resultTable = new LinkedList<>();
        this.next = new Next(pkb);
    }

    public void prepareResults() throws PKBException {
        int i = 0;
        for (Predicate predicate : queryTree.getPredTable()) {
            valueOfPred.add(predicate.getValue());
            indexOfPred.put(predicate.getValue(), i++);
        }
        for (Closure closure : queryTree.getClosureTable()) {
            ClosureResult result = getClosureResult(closure);
            if (result.getResultType().equals("MAP") && result.getPq().isEmpty()) {
                continue;
            }
            closureResults.add(result);
        }
        for (Pattern pattern : queryTree.getPatternTable()) {
            closureResults.add(getPatternResult(pattern));
        }
        for (With with : queryTree.getWithTable()) {
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

        if (queryTree.getSelector().getType().equals("boolean")) {
            if (resultTable.isEmpty() && !boolResult) {
                results.add("false");
            } else {
                results.add("true");
            }
        } else if (queryTree.getSelector().getType().equals("variable")) {
            Set<String> res = new HashSet<>();
            for (List<String> it : resultTable) {
                res.add(it.get(indexOfPred.get(queryTree.getSelector().getVariables().get(0))));
            }
            results.addAll(res);
        }
    }

    private void findResult(int pred) {
        if (pred >= possibleValues.size()) {
            return;
        }
        if (possibleValues.get(pred) == null) {
            return;
        }
        for (String val : possibleValues.get(pred)) {
            boolean foundValue = true;
            if (dependentClosures.get(pred) != null) {
                for (ClosureResult cr : dependentClosures.get(pred)) {
                    String depPred = !cr.getP().equals(valueOfPred.get(pred)) ? cr.getP() : cr.getQ();
                    Map<String, Set<String>> map = !cr.getP().equals(valueOfPred.get(pred)) ? cr.getQp() : cr.getPq();
                    if (map.get(val) != null) {
                        if (!map.get(val).contains(String.valueOf(tmpResult[indexOfPred.get(depPred)]))) {
                            foundValue = false;
                            break;
                        }
                    }
                }
            }
            if (foundValue) {
                tmpResult[pred] = val;
                if (pred < queryTree.getPredTable().size() - 1) {
                    findResult(pred + 1);
                } else {
                    List<String> result = new ArrayList<>();
                    for (int i = 0; i < queryTree.getPredTable().size(); i++) {
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
            for (Predicate pred : queryTree.getPredTable()) {
                List<Node<ASTNode>> allVals = pkb.getAllValues(pred.getType());
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
            possibleValues.remove(pred);
            possibleValues.put(pred, intersection);
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
        for (Predicate predicate : queryTree.getPredTable()) {
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
            if (relation.equals("next")) {
                closureResult = next.getResultWhenNoPredicate(lhsLineNum, rhsLineNum, _transient);
            }
            if (relation.equals("follows")) {
                if (lhsLineNum != 0 && rhsLineNum != 0) {
                    closureResult.setBoolResult(pkb.checkFollows(pkb.getStmtByLineNumber(lhsLineNum), pkb.getStmtByLineNumber(rhsLineNum), _transient));
                } else if (lhsLineNum != 0) {
                    closureResult.setBoolResult(!pkb.getFollowing(pkb.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
                } else if (rhsLineNum != 0) {
                    closureResult.setBoolResult(!pkb.getFollowed(pkb.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = pkb.getAllValues("statement");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!pkb.getFollowing(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (lhsLineNum != 0 && rhsLineNum != 0) {
                    closureResult.setBoolResult(pkb.checkParent(pkb.getStmtByLineNumber(lhsLineNum), pkb.getStmtByLineNumber(rhsLineNum), _transient));
                } else if (lhsLineNum != 0) {
                    closureResult.setBoolResult(!pkb.getChildren(pkb.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
                } else if (rhsLineNum != 0) {
                    closureResult.setBoolResult(!pkb.getParent(pkb.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = pkb.getAllValues("statement");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!pkb.getParent(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!lhsName.isEmpty() && !rhsName.isEmpty()) {
                    closureResult.setBoolResult(pkb.checkCalls(pkb.getProcedureByName(lhsName), pkb.getProcedureByName(rhsName), _transient));
                } else if (!lhsName.isEmpty()) {
                    closureResult.setBoolResult(!pkb.getCallees(pkb.getProcedureByName(lhsName), _transient).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!pkb.getCallers(pkb.getProcedureByName(rhsName), _transient).isEmpty());
                } else {
                    List<Node<ASTNode>> nodes = pkb.getAllValues("procedure");
                    closureResult.setBoolResult(false);
                    for (Node<ASTNode> node : nodes) {
                        if (!pkb.getCallees(node, _transient).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if ((lhsLineNum != 0 || !lhsName.isEmpty()) && !rhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    closureResult.setBoolResult(pkb.checkUses(n, rhsName));
                } else if (lhsLineNum != 0 || !lhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    closureResult.setBoolResult(!pkb.getUsed(n).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!pkb.getUsing(rhsName).isEmpty());
                } else {
                    List<String> vars = pkb.getAllVariables();
                    closureResult.setBoolResult(false);
                    for (String var : vars) {
                        if (!pkb.getUsing(var).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if ((lhsLineNum != 0 || !lhsName.isEmpty()) && !rhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    closureResult.setBoolResult(pkb.checkModifies(n, rhsName));
                } else if (lhsLineNum != 0 || !lhsName.isEmpty()) {
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    closureResult.setBoolResult(!pkb.getModified(n).isEmpty());
                } else if (!rhsName.isEmpty()) {
                    closureResult.setBoolResult(!pkb.getModifying(rhsName).isEmpty());
                } else {
                    List<String> vars = pkb.getAllVariables();
                    closureResult.setBoolResult(false);
                    for (String var : vars) {
                        if (!pkb.getModifying(var).isEmpty()) {
                            closureResult.setBoolResult(true);
                            break;
                        }
                    }
                }
            }
            closureResult.setResultType("BOOL");
        }
        else if (p2.getType().isEmpty()) { // predykat z lewej
            if (relation.equals("next")) {
                closureResult = next.getResultWhenLeftPredicate(p1, rhsLineNum, _transient);
            }
            if (relation.equals("follows")) {
                if (rhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getFollowed(pkb.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getFollowed(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (rhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getParent(pkb.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getParent(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!rhsName.isEmpty()) { // nazwa procedury
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getCallers(pkb.getProcedureByName(rhsName), _transient), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getCallers(val, _transient), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if (!rhsName.isEmpty()) { // zmienna
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getUsing(rhsName), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<String> allVars = pkb.getAllVariables();
                    for (String var : allVars) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getUsing(var), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if (!rhsName.isEmpty()) { // zmienna
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getModifying(rhsName), p1.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<String> allVars = pkb.getAllVariables();
                    for (String var : allVars) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getModifying(var), p1.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }

                }
            }
            closureResult.setResultType("SET");
            closureResult.setP(p1.getValue());
        }
        else if (p1.getType().isEmpty()) { // predykat z prawej
            if (relation.equals("next")) {
                closureResult = next.getResultWhenRightPredicate(p2, lhsLineNum, _transient);
            }
            if (relation.equals("follows")) {
                if (lhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getFollowing(pkb.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("parent")) {
                if (lhsLineNum != 0) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getChildren(pkb.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = filterNodesByType(pkb.getAllValues("statement"), "if", "while");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getChildren(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("calls")) {
                if (!lhsName.isEmpty()) { // numer linii
                    List<Node<ASTNode>> results = filterNodesByType(pkb.getCallees(pkb.getProcedureByName(lhsName), _transient), p2.getType());
                    for (Node<ASTNode> res : results) {
                        closureResult.addValue(nodeToString(res));
                    }
                } else { // _
                    List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
                    for (Node<ASTNode> val : allVals) {
                        List<Node<ASTNode>> results = filterNodesByType(pkb.getCallees(val, _transient), p2.getType());
                        for (Node<ASTNode> res : results) {
                            closureResult.addValue(nodeToString(res));
                        }
                    }
                }
            }
            if (relation.equals("uses")) {
                if (lhsLineNum != 0 || !lhsName.isEmpty()) { // stmt lub proc
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    List<String> results = pkb.getUsed(n);
                    for (String res : results) {
                        closureResult.addValue(res);
                    }
                } else { // _
                    List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
                    List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                    allVals.addAll(procNodes);
                    for (Node<ASTNode> val : allVals) {
                        List<String> results = pkb.getUsed(val);
                        for (String res : results) {
                            closureResult.addValue(res);
                        }
                    }
                }
            }
            if (relation.equals("modifies")) {
                if (lhsLineNum != 0 || !lhsName.isEmpty()) { // stmt lub proc
                    Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
                    List<String> results = pkb.getModified(n);
                    for (String res : results) {
                        closureResult.addValue(res);
                    }
                } else { // _
                    List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
                    List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                    allVals.addAll(procNodes);
                    for (Node<ASTNode> val : allVals) {
                        List<String> results = pkb.getModified(val);
                        for (String res : results) {
                            closureResult.addValue(res);
                        }
                    }
                }
            }
            closureResult.setResultType("SET");
            closureResult.setP(p2.getValue());
        }
        else { // 2 predykaty
            if (relation.equals("next")) {
                closureResult = next.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("follows")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(pkb.getAllValues("statement"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(pkb.getAllValues("statement"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(pkb.getFollowed(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("parent")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(filterNodesByType(pkb.getAllValues("statement"), "if", "while"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(pkb.getChildren(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(pkb.getAllValues("statement"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>>  qResults = filterNodesByType(pkb.getParent(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("calls")) {
                List<Node<ASTNode>> allPVals = filterNodesByType(pkb.getAllValues("procedure"), p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<Node<ASTNode>> pResults = filterNodesByType(pkb.getCallees(val, _transient), p2.getType());
                    for (Node<ASTNode> r : pResults) {
                        closureResult.addPq(nodeToString(val), nodeToString(r));
                    }
                }
                List<Node<ASTNode>> allQVals = filterNodesByType(pkb.getAllValues("procedure"), p2.getType());
                for (Node<ASTNode> val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(pkb.getCallers(val, _transient), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(nodeToString(val), nodeToString(r));
                    }
                }
            }
            if (relation.equals("uses")) {
                List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
                List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                allVals.addAll(procNodes);

                List<Node<ASTNode>> allPVals = filterNodesByType(allVals, p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<String> pResults = pkb.getUsed(val);
                    for (String r : pResults) {
                        closureResult.addPq(nodeToString(val), r);
                    }
                }
                List<String> allQVals = pkb.getAllVariables();
                for (String val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(pkb.getUsing(val), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(val, nodeToString(r));
                    }
                }
            }
            if (relation.equals("modifies")) {
                List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
                List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
                allVals.addAll(procNodes);

                List<Node<ASTNode>> allPVals = filterNodesByType(allVals, p1.getType());
                for (Node<ASTNode> val : allPVals) {
                    List<String> pResults = pkb.getModified(val);
                    for (String r : pResults) {
                        closureResult.addPq(nodeToString(val), r);
                    }
                }
                List<String> allQVals = pkb.getAllVariables();
                for (String val : allQVals) {
                    List<Node<ASTNode>> qResults = filterNodesByType(pkb.getModifying(val), p1.getType());
                    for (Node<ASTNode> r : qResults) {
                        closureResult.addQp(val, nodeToString(r));
                    }
                }
            }
            closureResult.setResultType("MAP");
            closureResult.setP(p1.getValue());
            closureResult.setQ(p2.getValue());
        }

        return closureResult;
    }

    private ClosureResult getPatternResult(Pattern pattern) {
        ClosureResult result = new ClosureResult();
        List<Node<ASTNode>> nodes = pkb.getPattern(pattern.getLhs(), pattern.getRhs());
        if (nodes.isEmpty()) {
            result.setResultType("BOOL");
            result.setBoolResult(false);
        } else {
            result.setResultType("SET");
            result.setP(pattern.getVarName());
            for (Node<ASTNode> node : nodes) {
                result.addValue(String.valueOf(node.getData().getLineNumber()));
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
            Set<String> allVals = pkb.getAllPropertyValues(with.getLhsPropertyName());
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
            Set<String> lhsVals = pkb.getAllPropertyValues(with.getLhsPropertyName());
            Set<String> rhsVals = pkb.getAllPropertyValues(with.getRhsPropertyName());
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
            Set<String> constants = pkb.getConstants();
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

    @Deprecated
    private String nodeToString(Node<ASTNode> node) {
        switch (node.getData().getNodeType()) {
            case PROCEDURE:
            case VARIABLE:
            case CONSTANT:
                return node.getData().getParam(NodeParamType.NAME);
            default:
                return String.valueOf(node.getData().getLineNumber());
        }
    }

    @Deprecated
    private List<Node<ASTNode>> filterNodesByType(List<Node<ASTNode>> nodes, String type) {
        return filterNodesByType(nodes, type, "");
    }

    @Deprecated
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

    @Deprecated
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

    public List<String> getResults() {
        return results;
    }
}
