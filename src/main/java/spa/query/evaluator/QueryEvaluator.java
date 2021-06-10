package spa.query.evaluator;

import spa.common.*;
import spa.exception.SPAException;
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
    private List<String> valueOfPredicate;
    private Map<String, Integer> indexOfPredicate;
    private List<RelationResult> relationResults;
    private Map<Integer, List<RelationResult>> dependentRelations;
    private Map<Integer, Set<String>> possibleValues;
    private List<List<String>> resultTable;
    //relations
    private RelationResultEvaluator next;
    private RelationResultEvaluator follows;
    private RelationResultEvaluator parent;
    private RelationResultEvaluator calls;
    private RelationResultEvaluator uses;
    private RelationResultEvaluator modifies;

    public QueryEvaluator(QueryTree queryTree, PKB pkb) {
        this.queryTree = queryTree;
        this.pkb = pkb;
        this.results = new ArrayList<>();
        this.tmpResult = new String[1000];
        this.valueOfPredicate = new ArrayList<>();
        this.indexOfPredicate = new HashMap<>();
        this.relationResults = new ArrayList<>();
        this.dependentRelations = new HashMap<>();
        this.possibleValues = new HashMap<>();
        this.resultTable = new LinkedList<>();
        this.next = new Next(pkb);
        this.follows = new Follows(pkb);
        this.parent = new Parent(pkb);
        this.calls = new Calls(pkb);
        this.uses = new Uses(pkb);
        this.modifies = new Modifies(pkb);
    }

    public void prepareResults() throws SPAException {
        int i = 0;
        for (Predicate predicate : queryTree.getPredicateList()) {
            valueOfPredicate.add(predicate.getValue());
            indexOfPredicate.put(predicate.getValue(), i++);
        }
        for (Relation relation : queryTree.getRelationList()) {
            RelationResult result = getRelationResult(relation);
            if (result.getResultType().equals("MAP") && result.getPq().isEmpty()) {
                continue;
            }
            relationResults.add(result);
        }
        for (Pattern pattern : queryTree.getPatternList()) {
            RelationResult result = getPatternResult(pattern);
            relationResults.add(result);
        }
        for (With with : queryTree.getWithList()) {
            RelationResult result = getWithResult(with);
            relationResults.add(result);
        }
        for (RelationResult cr : relationResults) {
            if (cr.getResultType().equals("MAP")) {
                int idx = Math.max(indexOfPredicate.get(cr.getP()), indexOfPredicate.get(cr.getQ()));
                dependentRelations.computeIfAbsent(idx, k -> new LinkedList<>());
                dependentRelations.get(idx).add(cr);
            }
        }
    }

    public void performEvaluation() {
        boolean foundFalseResult = false;
        boolean foundSetOrMap = false;
        for (RelationResult cr : relationResults) {
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
                res.add(it.get(indexOfPredicate.get(queryTree.getSelector().getVariables().get(0))));
            }
            results.addAll(res);
        }
    }

    private void findResult(int pred) {
        if (possibleValues.get(pred) == null) {
            return;
        }
        for (String val : possibleValues.get(pred)) {
            boolean found = true;
            if (dependentRelations.get(pred) != null) {
                for (RelationResult cr : dependentRelations.get(pred)) {
                    String depPred = !cr.getP().equals(valueOfPredicate.get(pred)) ? cr.getP() : cr.getQ();
                    Map<String, Set<String>> map = !cr.getP().equals(valueOfPredicate.get(pred)) ? cr.getQp() : cr.getPq();
                    if (map.get(val) != null) {
                        if (!map.get(val).contains(String.valueOf(tmpResult[indexOfPredicate.get(depPred)]))) {
                            found = false;
                            break;
                        }
                    }
                }
            }
            if (found) {
                tmpResult[pred] = val;
                if (pred < queryTree.getPredicateList().size() - 1) {
                    findResult(pred + 1);
                } else {
                    List<String> result = new ArrayList<>();
                    result.addAll(Arrays.asList(tmpResult).subList(0, queryTree.getPredicateList().size()));
                    resultTable.add(result);
                }
            }
        }
    }

    private void findPossibleValues() {
        Set<Integer> updatedPreds = new HashSet<>();

        if (relationResults.isEmpty()) {
            int i = 0;
            for (Predicate pred : queryTree.getPredicateList()) {
                List<Node<ASTNode>> allVals = pkb.getAllValues(pred.getType());
                for (Node<ASTNode> val : allVals) {
                    possibleValues.computeIfAbsent(i, k -> new HashSet<>());
                    possibleValues.get(i).add(val.getData().nodeToString());
                }
                i++;
            }
        }

        for (RelationResult cr : relationResults) {
            int pred;
            if (cr.getResultType().equals("SET")) {
                pred = indexOfPredicate.get(cr.getP());
                updatePossibleValues(pred, cr.getVals(), updatedPreds.contains(pred));
                updatedPreds.add(pred);
            } else if (cr.getResultType().equals("MAP")) {
                for (int i = 0; i < 2; i++) {
                    Set<String> tmpValues = new HashSet<>();
                    pred = indexOfPredicate.get(i == 0 ? cr.getP() : cr.getQ());
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
            Set<String> commonPart = new HashSet<>();
            for (String el : vals) {
                if (possibleValues.get(pred).contains(el)) {
                    commonPart.add(el);
                }
            }
            possibleValues.remove(pred);
            possibleValues.put(pred, commonPart);
        }
        else {
            possibleValues.computeIfAbsent(pred, k -> new HashSet<>());
            possibleValues.get(pred).addAll(vals);
        }
    }

    private RelationResult getRelationResult(Relation relation) throws SPAException {
        RelationResult relationResult = new RelationResult();
        boolean _transient = relation.getType().endsWith("*");
        String relationName = _transient ? relation.getType().substring(0, relation.getType().length() - 1) : relation.getType();

        Predicate p1 = new Predicate();
        Predicate p2 = new Predicate();
        for (Predicate predicate : queryTree.getPredicateList()) {
            if (relation.getLeftParam().equals(predicate.getValue())) {
                p1 = predicate;
            } else if (relation.getRightParam().equals(predicate.getValue())) {
                p2 = predicate;
            }
        }

        if (p1.getType().isEmpty() && p2.getType().isEmpty()) { // 0 predykatow
            switch (relationName) {
                case "next":
                    relationResult = next.getResultWhenNoPredicate(relation, _transient);
                    break;
                case "follows":
                    relationResult = follows.getResultWhenNoPredicate(relation, _transient);
                    break;
                case "parent":
                    relationResult = parent.getResultWhenNoPredicate(relation, _transient);
                    break;
                case "calls":
                    relationResult = calls.getResultWhenNoPredicate(relation, _transient);
                    break;
                case "uses":
                    relationResult = uses.getResultWhenNoPredicate(relation, _transient);
                    break;
                case "modifies":
                    relationResult = modifies.getResultWhenNoPredicate(relation, _transient);
                    break;
            }
            relationResult.setResultType("BOOL");
        }
        else if (p2.getType().isEmpty()) { // predykat z lewej
            switch (relationName) {
                case "next":
                    relationResult = next.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
                case "follows":
                    relationResult = follows.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
                case "parent":
                    relationResult = parent.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
                case "calls":
                    relationResult = calls.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
                case "uses":
                    relationResult = uses.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
                case "modifies":
                    relationResult = modifies.getResultWhenLeftPredicate(relation, p1, _transient);
                    break;
            }
            relationResult.setResultType("SET");
            relationResult.setP(p1.getValue());
        }
        else if (p1.getType().isEmpty()) { // predykat z prawej
            switch (relationName) {
                case "next":
                    relationResult = next.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
                case "follows":
                    relationResult = follows.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
                case "parent":
                    relationResult = parent.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
                case "calls":
                    relationResult = calls.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
                case "uses":
                    relationResult = uses.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
                case "modifies":
                    relationResult = modifies.getResultWhenRightPredicate(relation, p2, _transient);
                    break;
            }
            relationResult.setResultType("SET");
            relationResult.setP(p2.getValue());
        }
        else { // 2 predykaty
            switch (relationName) {
                case "next":
                    relationResult = next.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
                case "follows":
                    relationResult = follows.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
                case "parent":
                    relationResult = parent.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
                case "calls":
                    relationResult = calls.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
                case "uses":
                    relationResult = uses.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
                case "modifies":
                    relationResult = modifies.getResultWhenBothPredicates(p1, p2, _transient);
                    break;
            }
            relationResult.setResultType("MAP");
            relationResult.setP(p1.getValue());
            relationResult.setQ(p2.getValue());
        }

        return relationResult;
    }

    private RelationResult getPatternResult(Pattern pattern) {
        RelationResult result = new RelationResult();
        List<Node<ASTNode>> nodes = pkb.getPattern(pattern.getLeftParam(), pattern.getRightParam());
        if (nodes.isEmpty()) {
            result.setResultType("BOOL");
            result.setBoolResult(false);
        } else {
            result.setResultType("SET");
            result.setP(pattern.getVarName());
            for (Node<ASTNode> node : nodes) {
                result.addValue(String.valueOf(node.getData().getLine()));
            }
        }

        return result;
    }

    private RelationResult getWithResult(With with) {
        RelationResult relationResult = new RelationResult();
        if (with.getLeftParamPropertyName().equals(with.getRightParamPropertyName())) {
            relationResult.setResultType("MAP");
            relationResult.setP(with.getLeftParamVarName());
            relationResult.setQ(with.getRightParamVarName());
            Set<String> allVals = pkb.getAllPropertyValues(with.getLeftParamPropertyName());
            for (String val : allVals) {
                relationResult.addPq(val, val);
                relationResult.addQp(val, val);
            }
        } else if ((with.getLeftParamPropertyName().equals("value") && with.getRightParamPropertyName().equals("stmt")) ||
                (with.getLeftParamPropertyName().equals("stmt") && with.getRightParamPropertyName().equals("value"))) {
            relationResult.setResultType("MAP");
            relationResult.setP(with.getLeftParamVarName());
            relationResult.setQ(with.getRightParamVarName());
            Set<String> allVals = new HashSet<>();
            Set<String> leftParamVals = pkb.getAllPropertyValues(with.getLeftParamPropertyName());
            Set<String> rightParamVals = pkb.getAllPropertyValues(with.getRightParamPropertyName());
            for (String el : leftParamVals) {
                if (rightParamVals.contains(el)) {
                    allVals.add(el);
                }
            }
            for (String val : allVals) {
                relationResult.addPq(val, val);
                relationResult.addQp(val, val);
            }
        } else if (with.getLeftParamPropertyName().equals("value")) {
            relationResult.setResultType("BOOLEAN");
            relationResult.setBoolResult(false);
            Set<String> constants = pkb.getConstants();
            for (String c : constants) {
                if (c.equals(with.getRightParamVarName())) {
                    relationResult.setBoolResult(true);
                    break;
                }
            }
        } else {
            relationResult.setResultType("SET");
            relationResult.setP(with.getLeftParamVarName());
            if (with.getRightParamVarName().startsWith("\"")) {
                relationResult.addValue(with.getRightParamVarName().substring(1, with.getRightParamVarName().length() - 1));
            }
            else {
                relationResult.addValue(with.getRightParamVarName());
            }
        }

        return relationResult;
    }

    public List<String> getResults() {
        return results;
    }
}
