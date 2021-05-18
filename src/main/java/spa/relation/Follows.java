package spa.relation;

import spa.common.Closure;
import spa.common.ClosureResult;
import spa.common.NodeFilter;
import spa.common.Predicate;
import spa.exception.PKBException;
import spa.pkb.PKB;
import spa.tree.ASTNode;
import spa.tree.Node;

import java.util.List;

public class Follows implements ClosureResultEvaluator {
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
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws PKBException {
        // sprawdzam czy lhs i rhs sa numerami linii
        int lhsLineNum = 0, rhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
            rhsLineNum = Integer.valueOf(closure.getRhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
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
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Follows(s, 8)
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws PKBException {
        // sprawdzam czy rhs sa numerami linii
        int rhsLineNum = 0;
        try {
            rhsLineNum = Integer.valueOf(closure.getRhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (rhsLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowed(pkb.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowed(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Follows(9, s)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws PKBException {
        // sprawdzam czy lhs sa numerami linii
        int lhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (lhsLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowing(pkb.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        while w; assign a;
        Select a pattern a("a", _) such that Follows(w, a)
     */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("statement"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getFollowing(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("statement"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getFollowed(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
