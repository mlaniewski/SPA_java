package spa.query.parser;

import spa.common.*;

import java.util.ArrayList;
import java.util.List;

public class QueryTree {

    private Selector selector = new Selector();
    private List<Predicate> predicateList = new ArrayList<>();
    private List<Relation> relationList = new ArrayList<>();
    private List<With> withList = new ArrayList<>();
    private List<Pattern> patternList = new ArrayList<>();

    public Selector getSelector() {
        return selector;
    }

    public List<Predicate> getPredicateList() {
        return predicateList;
    }

    public List<Relation> getRelationList() {
        return relationList;
    }

    public List<With> getWithList() {
        return withList;
    }

    public List<Pattern> getPatternList() {
        return patternList;
    }
}
