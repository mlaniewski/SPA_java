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

public class Modifies implements ClosureResultEvaluator {
    private PKB pkb;
    private NodeFilter filter;

    public Modifies(PKB pkb) {
        this.pkb = pkb;
        this.filter = new NodeFilter();
    }

    /*
        stmt s;
        Select s such that Modifies(10, "c")
     */
    @Override
    public ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(closure.getLeftParam());
        } catch (NumberFormatException e) { }

        String leftParamName = "", rightParamName = "";
        if (closure.getLeftParam().startsWith("\"")) {
            leftParamName = closure.getLeftParam().substring(1, closure.getLeftParam().length() - 1);
        }
        if (closure.getRightParam().startsWith("\"")) {
            rightParamName = closure.getRightParam().substring(1, closure.getRightParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if ((leftParamLineNum != 0 || !leftParamName.isEmpty()) && !rightParamName.isEmpty()) {
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLineNumber(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            closureResult.setBoolResult(pkb.checkModifies(n, rightParamName));
        } else if (leftParamLineNum != 0 || !leftParamName.isEmpty()) {
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLineNumber(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            closureResult.setBoolResult(!pkb.getModified(n).isEmpty());
        } else if (!rightParamName.isEmpty()) {
            closureResult.setBoolResult(!pkb.getModifying(rightParamName).isEmpty());
        } else {
            List<String> vars = pkb.getAllVariables();
            closureResult.setBoolResult(false);
            for (String var : vars) {
                if (!pkb.getModifying(var).isEmpty()) {
                    closureResult.setBoolResult(true);
                    break;
                }
            }
        }
        return closureResult;
    }

    /*
        stmt s;
        Select s such that Modifies(s, "c")
     */
    @Override
    public ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws SPAException {
        String rightParamName = "";
        if (closure.getRightParam().startsWith("\"")) {
            rightParamName = closure.getRightParam().substring(1, closure.getRightParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (!rightParamName.isEmpty()) { // zmienna
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getModifying(rightParamName), p1.getType());
            for (Node<ASTNode> res : results) {
                closureResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<String> allVars = pkb.getAllVariables();
            for (String var : allVars) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getModifying(var), p1.getType());
                for (Node<ASTNode> res : results) {
                    closureResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return closureResult;
    }

    /*
        variable v;
        Select v such that Modifies(21, v)
     */
    @Override
    public ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(closure.getLeftParam());
        } catch (NumberFormatException e) { }

        String leftParamName = "";
        if (closure.getLeftParam().startsWith("\"")) {
            leftParamName = closure.getLeftParam().substring(1, closure.getLeftParam().length() - 1);
        }

        ClosureResult closureResult = new ClosureResult();
        if (leftParamLineNum != 0 || !leftParamName.isEmpty()) { // stmt lub proc
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLineNumber(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            List<String> results = pkb.getModified(n);
            for (String res : results) {
                closureResult.addValue(res);
            }
        } else { // _
            List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            allVals.addAll(procNodes);
            for (Node<ASTNode> val : allVals) {
                List<String> results = pkb.getModified(val);
                for (String res : results) {
                    closureResult.addValue(res);
                }
            }
        }
        return closureResult;
    }

    /*
        assign a; variable v;
        Select v such that Modifies(a, v)
     */
    @Override
    public ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        ClosureResult closureResult = new ClosureResult();
        List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
        List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
        allVals.addAll(procNodes);

        List<Node<ASTNode>> allPVals = filter.filterNodesByType(allVals, p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<String> pResults = pkb.getModified(val);
            for (String r : pResults) {
                closureResult.addPq(val.getData().nodeToString(), r);
            }
        }
        List<String> allQVals = pkb.getAllVariables();
        for (String val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getModifying(val), p1.getType());
            for (Node<ASTNode> r : qResults) {
                closureResult.addQp(val, r.getData().nodeToString());
            }
        }
        return closureResult;
    }
}
