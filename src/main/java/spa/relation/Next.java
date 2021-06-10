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

public class Next implements RelationResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Next(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        prog_line n2;
        Select BOOLEAN such that Next(1, 2)
     */
    @Override
    public RelationResult getResultWhenNoPredicate(Relation relation, boolean _transient) throws SPAException {
        int leftParamLineNum = 0, rightParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(relation.getLeftParam());
            rightParamLineNum = Integer.valueOf(relation.getRightParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (leftParamLineNum != 0 && rightParamLineNum != 0) {
            relationResult.setBoolResult(pkb.isNext(pkb.getStmtByLine(leftParamLineNum), pkb.getStmtByLine(rightParamLineNum), _transient));
        } else if (leftParamLineNum != 0) {
            relationResult.setBoolResult(!pkb.getNext(pkb.getStmtByLine(leftParamLineNum), _transient).isEmpty());
        } else if (rightParamLineNum != 0) {
            relationResult.setBoolResult(!pkb.getPrevious(pkb.getStmtByLine(rightParamLineNum), _transient).isEmpty());
        } else {
            List<Node<ASTNode>> nodes = pkb.getAllValues("statement");
            relationResult.setBoolResult(false);
            for (Node<ASTNode> node : nodes) {
                if (!pkb.getNext(node, _transient).isEmpty()) {
                    relationResult.setBoolResult(true);
                    break;
                }
            }
        }
        return relationResult;
    }

    /*
        prog_line n2;
        Select n2 such that Next*(n2, 5)
     */
    @Override
    public RelationResult getResultWhenLeftPredicate(Relation relation, Predicate p1, boolean _transient) throws SPAException {
        int rightParamLineNum = 0;
        try {
            rightParamLineNum = Integer.valueOf(relation.getRightParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (rightParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getPrevious(pkb.getStmtByLine(rightParamLineNum), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getPrevious(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        prog_line n2;
        Select n2 such that Next*(9, n2)
     */
    @Override
    public RelationResult getResultWhenRightPredicate(Relation relation, Predicate p2, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(relation.getLeftParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (leftParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getNext(pkb.getStmtByLine(leftParamLineNum), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getNext(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }
    /*
        while w; assign a;
        Select a pattern a("a", _) such that Next(w, a)
     */
    @Override
    public RelationResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        RelationResult relationResult = new RelationResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("statement"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getNext(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                relationResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("statement"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getPrevious(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                relationResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return relationResult;
    }
}
