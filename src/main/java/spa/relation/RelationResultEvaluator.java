package spa.relation;

import spa.common.ClosureResult;
import spa.common.Predicate;
import spa.exception.PKBException;

public interface RelationResultEvaluator {
    ClosureResult getResultWhenNoPredicate(Integer lhsLineNum, Integer rhsLineNum, boolean _transient) throws PKBException;
    ClosureResult getResultWhenLeftPredicate(Predicate p1, Integer rhsLineNum, boolean _transient) throws PKBException;
    ClosureResult getResultWhenRightPredicate(Predicate p2, Integer lhsLineNum, boolean _transient) throws PKBException;
    ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient);
}
