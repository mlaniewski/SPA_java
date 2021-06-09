package spa.relation;

import spa.common.Closure;
import spa.common.ClosureResult;
import spa.common.NodeFilter;
import spa.common.Predicate;
import spa.exception.SPAException;
import spa.pkb.PKB;
import spa.tree.ASTNode;
import spa.tree.Node;

import java.util.List;

public class Next implements ClosureResultEvaluator {
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
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws SPAException {
        // sprawdzam czy lhs i rhs sa numerami linii
        int lhsLineNum = 0, rhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
            rhsLineNum = Integer.valueOf(closure.getRhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (lhsLineNum != 0 && rhsLineNum != 0) {
            closureResult.setBoolResult(pkb.checkNext(pkb.getStmtByLineNumber(lhsLineNum), pkb.getStmtByLineNumber(rhsLineNum), _transient));
        } else if (lhsLineNum != 0) {
            closureResult.setBoolResult(!pkb.getNext(pkb.getStmtByLineNumber(lhsLineNum), _transient).isEmpty());
        } else if (rhsLineNum != 0) {
            closureResult.setBoolResult(!pkb.getPrev(pkb.getStmtByLineNumber(rhsLineNum), _transient).isEmpty());
        } else {
            List<Node<ASTNode>> nodes = pkb.getAllValues("statement");
            closureResult.setBoolResult(false);
            for (Node<ASTNode> node : nodes) {
                if (!pkb.getNext(node, _transient).isEmpty()) {
                    closureResult.setBoolResult(true);
                    break;
                }
            }
        }
        return closureResult;
    }

    /*
        prog_line n2;
        Select n2 such that Next*(n2, 5)
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws SPAException {
        // sprawdzam czy rhs sa numerami linii
        int rhsLineNum = 0;
        try {
            rhsLineNum = Integer.valueOf(closure.getRhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (rhsLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getPrev(pkb.getStmtByLineNumber(rhsLineNum), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getPrev(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        prog_line n2;
        Select n2 such that Next*(9, n2)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws SPAException {
        // sprawdzam czy lhs sa numerami linii
        int lhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (lhsLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getNext(pkb.getStmtByLineNumber(lhsLineNum), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getNext(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }
    /*
        while w; assign a;
        Select a pattern a("a", _) such that Next(w, a)
     */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("statement"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getNext(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("statement"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getPrev(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
