package spa.relation;

import spa.common.Relation;
import spa.common.RelationResult;
import spa.common.NodeFilter;
import spa.common.Predicate;
import spa.exception.SPAException;
import spa.pkb.PKB;
import spa.tree.ASTNode;
import spa.tree.Node;

import java.util.List;

public class Calls implements RelationResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Calls(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        prog_line n;
        Select BOOLEAN such that Calls("Circle", "Rectangle")
     */
    @Override
    public RelationResult getResultWhenNoPredicate(Relation relation, boolean _transient) throws SPAException {
        String leftParamName = "", rightParamName = "";
        if (relation.getLeftParam().startsWith("\"")) {
            leftParamName = relation.getLeftParam().substring(1, relation.getLeftParam().length() - 1);
        }
        if (relation.getRightParam().startsWith("\"")) {
            rightParamName = relation.getRightParam().substring(1, relation.getRightParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if (!leftParamName.isEmpty() && !rightParamName.isEmpty()) {
            relationResult.setBoolResult(pkb.isCalls(pkb.getProcedureByName(leftParamName), pkb.getProcedureByName(rightParamName), _transient));
        } else if (!leftParamName.isEmpty()) {
            relationResult.setBoolResult(!pkb.getCalledBy(pkb.getProcedureByName(leftParamName), _transient).isEmpty());
        } else if (!rightParamName.isEmpty()) {
            relationResult.setBoolResult(!pkb.getCalling(pkb.getProcedureByName(rightParamName), _transient).isEmpty());
        } else {
            List<Node<ASTNode>> nodes = pkb.getAllValues("procedure");
            relationResult.setBoolResult(false);
            for (Node<ASTNode> node : nodes) {
                if (!pkb.getCalledBy(node, _transient).isEmpty()) {
                    relationResult.setBoolResult(true);
                    break;
                }
            }
        }
        return relationResult;
    }

    /*
        procedure p;
        Select p such that Calls(p, "Third")
     */
    @Override
    public RelationResult getResultWhenLeftPredicate(Relation relation, Predicate p1, boolean _transient) throws SPAException {
        String rightParamName = "";
        if (relation.getRightParam().startsWith("\"")) {
            rightParamName = relation.getRightParam().substring(1, relation.getRightParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if (!rightParamName.isEmpty()) { // nazwa procedury
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCalling(pkb.getProcedureByName(rightParamName), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCalling(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        procedure p;
        Select p such that Calls("First", p)
     */
    @Override
    public RelationResult getResultWhenRightPredicate(Relation relation, Predicate p2, boolean _transient) throws SPAException {
        String leftParamName = "";
        if (relation.getLeftParam().startsWith("\"")) {
            leftParamName = relation.getLeftParam().substring(1, relation.getLeftParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if (!leftParamName.isEmpty()) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCalledBy(pkb.getProcedureByName(leftParamName), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCalledBy(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        procedure p1, p2; prog_line n;
        Select n such that Calls(p1, p2)
     */
    @Override
    public RelationResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        RelationResult relationResult = new RelationResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("procedure"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getCalledBy(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                relationResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("procedure"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getCalling(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                relationResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return relationResult;
    }
}
