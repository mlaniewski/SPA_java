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

public class Follows implements RelationResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Follows(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        prog_line n2;
        Select BOOLEAN such that Follows(8, 9)
     */
    @Override
    public RelationResult getResultWhenNoPredicate(Relation relation, boolean _transient) throws SPAException {
        int leftParmLineNum = 0, rightParmLineNum = 0;
        try {
            leftParmLineNum = Integer.valueOf(relation.getLeftParam());
            rightParmLineNum = Integer.valueOf(relation.getRightParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (leftParmLineNum != 0 && rightParmLineNum != 0) {
            relationResult.setBoolResult(pkb.isFollows(pkb.getStmtByLine(leftParmLineNum), pkb.getStmtByLine(rightParmLineNum), _transient));
        } else if (leftParmLineNum != 0) {
            relationResult.setBoolResult(!pkb.getFollowing(pkb.getStmtByLine(leftParmLineNum), _transient).isEmpty());
        } else if (rightParmLineNum != 0) {
            relationResult.setBoolResult(!pkb.getFollowedBy(pkb.getStmtByLine(rightParmLineNum), _transient).isEmpty());
        } else {
            List<Node<ASTNode>> nodes = pkb.getAllValues("statement");
            relationResult.setBoolResult(false);
            for (Node<ASTNode> node : nodes) {
                if (!pkb.getFollowing(node, _transient).isEmpty()) {
                    relationResult.setBoolResult(true);
                    break;
                }
            }
        }
        return relationResult;
    }

    /*
        stmt s;
        Select s such that Follows(s, 8)
     */
    @Override
    public RelationResult getResultWhenLeftPredicate(Relation relation, Predicate p1, boolean _transient) throws SPAException {
        int rightParamLineNum = 0;
        try {
            rightParamLineNum = Integer.valueOf(relation.getRightParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (rightParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowedBy(pkb.getStmtByLine(rightParamLineNum), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowedBy(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        stmt s;
        Select s such that Follows(9, s)
     */
    @Override
    public RelationResult getResultWhenRightPredicate(Relation relation, Predicate p2, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(relation.getLeftParam());
        } catch (NumberFormatException e) { }

        RelationResult relationResult = new RelationResult();
        if (leftParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowing(pkb.getStmtByLine(leftParamLineNum), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        while w; assign a;
        Select a pattern a("a", _) such that Follows(w, a)
     */
    @Override
    public RelationResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        RelationResult relationResult = new RelationResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("statement"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                relationResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("statement"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getFollowedBy(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                relationResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return relationResult;
    }
}
