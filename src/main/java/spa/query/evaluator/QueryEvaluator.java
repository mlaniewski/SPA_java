package spa.query.evaluator;

import spa.common.*;
import spa.exception.PKBException;
import spa.pkb.PKB;
import spa.query.parser.QueryTree;
import spa.relation.*;
import spa.tree.ASTNode;
import spa.tree.Node;

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
    private ClosureResultEvaluator next;
    private ClosureResultEvaluator follows;
    private ClosureResultEvaluator parent;
    private ClosureResultEvaluator calls;
    private ClosureResultEvaluator uses;
    private ClosureResultEvaluator modifies;

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
        this.follows = new Follows(pkb);
        this.parent = new Parent(pkb);
        this.calls = new Calls(pkb);
        this.uses = new Uses(pkb);
        this.modifies = new Modifies(pkb);
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
                    possibleValues.get(i).add(val.getData().nodeToString());
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

        if (p1.getType().isEmpty() && p2.getType().isEmpty()) { // 0 predykatow
            if (relation.equals("next")) {
                closureResult = next.getResultWhenNoPredicate(closure, _transient);
            }
            if (relation.equals("follows")) {
                closureResult = follows.getResultWhenNoPredicate(closure, _transient);
            }
            if (relation.equals("parent")) {
                closureResult = parent.getResultWhenNoPredicate(closure, _transient);
            }
            if (relation.equals("calls")) {
                closureResult = calls.getResultWhenNoPredicate(closure, _transient);
            }
            if (relation.equals("uses")) {
                closureResult = uses.getResultWhenNoPredicate(closure, _transient);
            }
            if (relation.equals("modifies")) {
                closureResult = modifies.getResultWhenNoPredicate(closure, _transient);
            }
            closureResult.setResultType("BOOL");
        }
        else if (p2.getType().isEmpty()) { // predykat z lewej
            if (relation.equals("next")) {
                closureResult = next.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            if (relation.equals("follows")) {
                closureResult = follows.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            if (relation.equals("parent")) {
                closureResult = parent.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            if (relation.equals("calls")) {
                closureResult = calls.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            if (relation.equals("uses")) {
                closureResult = uses.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            if (relation.equals("modifies")) {
                closureResult = modifies.getResultWhenLeftPredicate(closure, p1, _transient);
            }
            closureResult.setResultType("SET");
            closureResult.setP(p1.getValue());
        }
        else if (p1.getType().isEmpty()) { // predykat z prawej
            if (relation.equals("next")) {
                closureResult = next.getResultWhenRightPredicate(closure, p2, _transient);
            }
            if (relation.equals("follows")) {
                closureResult = follows.getResultWhenRightPredicate(closure, p2, _transient);
            }
            if (relation.equals("parent")) {
                closureResult = parent.getResultWhenRightPredicate(closure, p2, _transient);
            }
            if (relation.equals("calls")) {
                closureResult = calls.getResultWhenRightPredicate(closure, p2, _transient);
            }
            if (relation.equals("uses")) {
                closureResult = uses.getResultWhenRightPredicate(closure, p2, _transient);
            }
            if (relation.equals("modifies")) {
                closureResult = modifies.getResultWhenRightPredicate(closure, p2, _transient);
            }
            closureResult.setResultType("SET");
            closureResult.setP(p2.getValue());
        }
        else { // 2 predykaty
            if (relation.equals("next")) {
                closureResult = next.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("follows")) {
                closureResult = follows.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("parent")) {
                closureResult = parent.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("calls")) {
                closureResult = calls.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("uses")) {
                closureResult = uses.getResultWhenBothPredicates(p1, p2, _transient);
            }
            if (relation.equals("modifies")) {
                closureResult = modifies.getResultWhenBothPredicates(p1, p2, _transient);
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

    public List<String> getResults() {
        return results;
    }
}
