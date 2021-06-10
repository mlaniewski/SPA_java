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

public class Calls implements ClosureResultEvaluator {
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
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws SPAException {
        String leftParamName = "", rightParamName = "";
        if (closure.getLeftParam().startsWith("\"")) {
            leftParamName = closure.getLeftParam().substring(1, closure.getLeftParam().length() - 1);
        }
        if (closure.getRightParam().startsWith("\"")) {
            rightParamName = closure.getRightParam().substring(1, closure.getRightParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!leftParamName.isEmpty() && !rightParamName.isEmpty()) {
            closureResult.setBoolResult(pkb.checkCalls(pkb.getProcedureByName(leftParamName), pkb.getProcedureByName(rightParamName), _transient));
        } else if (!leftParamName.isEmpty()) {
            closureResult.setBoolResult(!pkb.getCallees(pkb.getProcedureByName(leftParamName), _transient).isEmpty());
        } else if (!rightParamName.isEmpty()) {
            closureResult.setBoolResult(!pkb.getCallers(pkb.getProcedureByName(rightParamName), _transient).isEmpty());
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
        return closureResult;
    }

    /*
        procedure p;
        Select p such that Calls(p, "Third")
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws SPAException {
        String rightParamName = "";
        if (closure.getRightParam().startsWith("\"")) {
            rightParamName = closure.getRightParam().substring(1, closure.getRightParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!rightParamName.isEmpty()) { // nazwa procedury
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallers(pkb.getProcedureByName(rightParamName), _transient), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallers(val, _transient), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        procedure p;
        Select p such that Calls("First", p)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws SPAException {
        String leftParamName = "";
        if (closure.getLeftParam().startsWith("\"")) {
            leftParamName = closure.getLeftParam().substring(1, closure.getLeftParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!leftParamName.isEmpty()) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallees(pkb.getProcedureByName(leftParamName), _transient), p2.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<Node<ASTNode>> allVals = pkb.getAllValues("procedure");
            for (Node<ASTNode> val : allVals) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallees(val, _transient), p2.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        procedure p1, p2; prog_line n;
        Select n such that Calls(p1, p2)
     */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> allPVals = filter.filterNodesByType(pkb.getAllValues("procedure"), p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<Node<ASTNode>> pResults = filter.filterNodesByType(pkb.getCallees(val, _transient), p2.getType());
            for (Node<ASTNode> r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        List<Node<ASTNode>> allQVals = filter.filterNodesByType(pkb.getAllValues("procedure"), p2.getType());
        for (Node<ASTNode> val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getCallers(val, _transient), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val.getData().nodeToString(), r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
