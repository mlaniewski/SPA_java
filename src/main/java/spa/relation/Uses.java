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

public class Uses implements RelationResultEvaluator {
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
    public RelationResult getResultWhenNoPredicate(Relation relation, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(relation.getLeftParam());
        } catch (NumberFormatException e) { }

        String leftParamName = "", rightParamName = "";
        if (relation.getLeftParam().startsWith("\"")) {
            leftParamName = relation.getLeftParam().substring(1, relation.getLeftParam().length() - 1);
        }
        if (relation.getRightParam().startsWith("\"")) {
            rightParamName = relation.getRightParam().substring(1, relation.getRightParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if ((leftParamLineNum != 0 || !leftParamName.isEmpty()) && !rightParamName.isEmpty()) {
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLine(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            relationResult.setBoolResult(pkb.isUses(n, rightParamName));
        } else if (leftParamLineNum != 0 || !leftParamName.isEmpty()) {
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLine(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            relationResult.setBoolResult(!pkb.getUsedBy(n).isEmpty());
        } else if (!rightParamName.isEmpty()) {
            relationResult.setBoolResult(!pkb.getUsing(rightParamName).isEmpty());
        } else {
            List<String> vars = pkb.getAllVariables();
            relationResult.setBoolResult(false);
            for (String var : vars) {
                if (!pkb.getUsing(var).isEmpty()) {
                    relationResult.setBoolResult(true);
                    break;
                }
            }
        }
        return relationResult;
    }

    /*
        stmt s;
        Select s such that Uses(s, "c")
     */
    @Override
    public RelationResult getResultWhenLeftPredicate(Relation relation, Predicate p1, boolean _transient) throws SPAException {
        String rightParamName = "";
        if (relation.getRightParam().startsWith("\"")) {
            rightParamName = relation.getRightParam().substring(1, relation.getRightParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if (!rightParamName.isEmpty()) { // zmienna
            List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getUsing(rightParamName), p1.getType());
            for (Node<ASTNode> res : results) {
                relationResult.addValue(res.getData().nodeToString());
            }
        } else { // _
            List<String> allVars = pkb.getAllVariables();
            for (String var : allVars) {
                List<Node<ASTNode>> results = filter.filterNodesByType(pkb.getUsing(var), p1.getType());
                for (Node<ASTNode> res : results) {
                    relationResult.addValue(res.getData().nodeToString());
                }
            }
        }
        return relationResult;
    }

    /*
        variable v;
        Select v such that Uses("Circle", v)

        variable v;
        Select v such that Uses(23, v)
     */
    @Override
    public RelationResult getResultWhenRightPredicate(Relation relation, Predicate p2, boolean _transient) throws SPAException {
        int leftParamLineNum = 0;
        try {
            leftParamLineNum = Integer.valueOf(relation.getLeftParam());
        } catch (NumberFormatException e) { }

        String leftParamName = "";
        if (relation.getLeftParam().startsWith("\"")) {
            leftParamName = relation.getLeftParam().substring(1, relation.getLeftParam().length() - 1);
        }

        RelationResult relationResult = new RelationResult();
        if (leftParamLineNum != 0 || !leftParamName.isEmpty()) { // stmt lub proc
            Node<ASTNode> n = leftParamLineNum != 0 ? pkb.getStmtByLine(leftParamLineNum) : pkb.getProcedureByName(leftParamName);
            List<String> results = pkb.getUsedBy(n);
            for (String res : results) {
                relationResult.addValue(res);
            }
        } else { // _
            List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
            List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
            allVals.addAll(procNodes);
            for (Node<ASTNode> val : allVals) {
                List<String> results = pkb.getUsedBy(val);
                for (String res : results) {
                    relationResult.addValue(res);
                }
            }
        }
        return relationResult;
    }

    /*
        assign a; variable v;
        Select v such that Uses(a, v)
     */
    @Override
    public RelationResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient) {
        RelationResult relationResult = new RelationResult();
        List<Node<ASTNode>> procNodes = pkb.getAllValues("procedure");
        List<Node<ASTNode>> allVals = pkb.getAllValues("statement");
        allVals.addAll(procNodes);

        List<Node<ASTNode>> allPVals = filter.filterNodesByType(allVals, p1.getType());
        for (Node<ASTNode> val : allPVals) {
            List<String> pResults = pkb.getUsedBy(val);
            for (String r : pResults) {
                relationResult.addPq(val.getData().nodeToString(), r);
            }
        }
        List<String> allQVals = pkb.getAllVariables();
        for (String val : allQVals) {
            List<Node<ASTNode>> qResults = filter.filterNodesByType(pkb.getUsing(val), p1.getType());
            for (Node<ASTNode> r : qResults) {
                relationResult.addQp(val, r.getData().nodeToString());
            }
        }
        return relationResult;
    }
}
