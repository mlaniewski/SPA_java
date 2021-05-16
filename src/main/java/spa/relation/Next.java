package spa.relation;

import spa.common.ClosureResult;
import spa.common.NodeFilter;
import spa.common.Predicate;
import spa.exception.PKBException;
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

    @Override
    public ClosureResult getResultWhenNoPredicate(Integer lhsLineNum, Integer rhsLineNum, boolean _transient) throws PKBException {
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

    @Override
    public ClosureResult getResultWhenLeftPredicate(Predicate p1, Integer rhsLineNum, boolean _transient) throws PKBException {
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

    @Override
    public ClosureResult getResultWhenRightPredicate(Predicate p2, Integer lhsLineNum, boolean _transient) throws PKBException {
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
