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

public class Uses implements ClosureResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Uses(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        stmt s;
        Select s such that Modifies(10, "c")
     */
    @Override
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws PKBException {
        // sprawdzam czy lhs sa numerami linii
        int lhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
        } catch (NumberFormatException e) { }
        // sprawdzam czy lhs i rhs sa nazwami
        String lhsName = "", rhsName = "";
        if (closure.getLhs().startsWith("\"")) {
            lhsName = closure.getLhs().substring(1, closure.getLhs().length() - 1);
        }
        if (closure.getRhs().startsWith("\"")) {
            rhsName = closure.getRhs().substring(1, closure.getRhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if ((lhsLineNum != 0 || !lhsName.isEmpty()) && !rhsName.isEmpty()) {
            Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
            closureResult.setBoolResult(pkb.checkUses(n, rhsName));
        } else if (lhsLineNum != 0 || !lhsName.isEmpty()) {
            Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
            closureResult.setBoolResult(!pkb.getUsed(n).isEmpty());
        } else if (!rhsName.isEmpty()) {
            closureResult.setBoolResult(!pkb.getUsing(rhsName).isEmpty());
        } else {
            List<String> vars = pkb.getAllVariables();
            closureResult.setBoolResult(false);
            for (String var : vars) {
                if (!pkb.getUsing(var).isEmpty()) {
                    closureResult.setBoolResult(true);
                    break;
                }
            }
        }
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Uses(s, "c")
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws PKBException {
        String rhsName = "";
        if (closure.getRhs().startsWith("\"")) {
            rhsName = closure.getRhs().substring(1, closure.getRhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!rhsName.isEmpty()) { // zmienna
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getUsing(rhsName), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<String> allVars = pkb.getAllVariables();
            for (String var : allVars) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getUsing(var), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        variable v;
        Select v such that Uses("Circle", v)

        variable v;
        Select v such that Uses(23, v)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws PKBException {
        // sprawdzam czy lhs sa numerami linii
        int lhsLineNum = 0;
        try {
            lhsLineNum = Integer.valueOf(closure.getLhs());
        } catch (NumberFormatException e) { }
        // sprawdzam czy lhs i rhs sa nazwami
        String lhsName = "", rhsName = "";
        if (closure.getLhs().startsWith("\"")) {
            lhsName = closure.getLhs().substring(1, closure.getLhs().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (lhsLineNum != 0 || !lhsName.isEmpty()) { // stmt lub proc
            Node<ASTNode> n = lhsLineNum != 0 ? pkb.getStmtByLineNumber(lhsLineNum) : pkb.getProcedureByName(lhsName);
            List<String> results = pkb.getUsed(n);
            for (String res : results) {
                closureResult.addValue(res);
            }
        } else { // _
            List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            allVals.addAll(procNodes);
            for (Node<ASTNode> val : allVals) {
                List<String> results = pkb.getUsed(val);
                for (String res : results) {
                    closureResult.addValue(res);
                }
            }
        }
        return closureResult;
    }

    /*
        assign a; variable v;
        Select v such that Uses(a, v)
     */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
        List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
        allVals.addAll(procNodes);

        List<Node<ASTNode>> allPVals = filter.filterNodesByType(allVals, p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<String> pResults = pkb.getUsed(val);
            for (String r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r);
            }
        }
        List<String> allQVals = pkb.getAllVariables();
        for (String val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getUsing(val), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val, r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
