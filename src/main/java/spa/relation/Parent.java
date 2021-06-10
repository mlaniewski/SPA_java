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

public class Parent implements ClosureResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Parent(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        prog_line n2;
        Select BOOLEAN such that Parent(8, 9)
     */
    @Override
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws SPAException {
        int leftParamLineNum = 0, rightParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(closure.getLeftParam());
            rightParamLineNum = Integer.valueOf(closure.getRightParam());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (leftParamLineNum != 0 && rightParamLineNum != 0) {
            closureResult.setBoolResult(pkb.isParent(pkb.getStmtByLine(leftParamLineNum), pkb.getStmtByLine(rightParamLineNum), _transient));
        } else if (leftParamLineNum != 0) {
            closureResult.setBoolResult(!pkb.getChildren(pkb.getStmtByLine(leftParamLineNum), _transient).isEmpty());
        } else if (rightParamLineNum != 0) {
            closureResult.setBoolResult(!pkb.getParent(pkb.getStmtByLine(rightParamLineNum), _transient).isEmpty());
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
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Parent(s, 9)
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws SPAException {
        int rightParamLineNum = 0;
        try {
            rightParamLineNum = Integer.valueOf(closure.getRightParam());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (rightParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getParent(pkb.getStmtByLine(rightParamLineNum), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getParent(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Parent(9, s)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(closure.getLeftParam());
        } catch (NumberFormatException e) { }

        ClosureResult closureResult = new ClosureResult();
        if (leftParamLineNum != 0) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getChildren(pkb.getStmtByLine(leftParamLineNum), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = filter.filterNodesByType(pkb.getAllValues("statement"), "if", "while");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getChildren(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        while w; assign a;
        Select a pattern a("a", _) such that Parent(w, a)
    */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(filter.filterNodesByType(pkb.getAllValues("statement"), "if", "while"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getChildren(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("statement"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>>  qResults = filter.filterNodesByType(pkb.getParent(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
