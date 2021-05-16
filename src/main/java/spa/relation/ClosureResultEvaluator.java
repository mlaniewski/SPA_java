package spa.relation;

import spa.common.Closure;
import spa.common.ClosureResult;
import spa.common.Predicate;
import spa.exception.PKBException;

public interface ClosureResultEvaluator {
    ClosureResult getResultWhenNoPredicate(Closure closure, boolean _transient) throws PKBException;
    ClosureResult getResultWhenLeftPredicate(Closure closure, Predicate p1, boolean _transient) throws PKBException;
    ClosureResult getResultWhenRightPredicate(Closure closure, Predicate p2, boolean _transient) throws PKBException;
    ClosureResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient);
}
