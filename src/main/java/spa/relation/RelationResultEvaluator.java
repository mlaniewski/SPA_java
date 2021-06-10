package spa.relation;

import spa.common.Relation;
import spa.common.RelationResult;
import spa.common.Predicate;
import spa.exception.SPAException;

public interface RelationResultEvaluator {
    RelationResult getResultWhenNoPredicate(Relation relation, boolean _transient) throws SPAException;
    RelationResult getResultWhenLeftPredicate(Relation relation, Predicate p1, boolean _transient) throws SPAException;
    RelationResult getResultWhenRightPredicate(Relation relation, Predicate p2, boolean _transient) throws SPAException;
    RelationResult getResultWhenBothPredicates(Predicate p1, Predicate p2, boolean _transient);
}
