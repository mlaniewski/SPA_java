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
        // sprawdzam czy lhs i rhs sa nazwami, czyli są w formacie "%s"
        String lhsName = "", rhsName = "";
        if (closure.getLhs().startsWith("\"")) {
            lhsName = closure.getLhs().substring(1, closure.getLhs().length() - 1);
        }
        if (closure.getRhs().startsWith("\"")) {
            rhsName = closure.getRhs().substring(1, closure.getRhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
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
        return closureResult;
    }

    /*
        procedure p;
        Select p such that Calls(p, "Third")
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws SPAException {
        // sprawdzam czy rhs sa nazwami, czyli są w formacie "%s"
        String rhsName = "";
        if (closure.getRhs().startsWith("\"")) {
            rhsName = closure.getRhs().substring(1, closure.getRhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!rhsName.isEmpty()) { // nazwa procedury
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallers(pkb.getProcedureByName(rhsName), _transient), p1.getType());
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
        // sprawdzam czy lhs i rhs sa nazwami, czyli są w formacie "%s"
        String lhsName = "";
        if (closure.getLhs().startsWith("\"")) {
            lhsName = closure.getLhs().substring(1, closure.getLhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!lhsName.isEmpty()) { // numer linii
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getCallees(pkb.getProcedureByName(lhsName), _transient), p2.getType());
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
