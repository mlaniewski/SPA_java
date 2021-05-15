package spa.query.parser;

import spa.common.*;

import java.util.ArrayList;
import java.util.List;

public class QueryTree {

    private Selector selector = new Selector();
    private List<Predicate> predTable = new ArrayList<>();
    private List<Closure> closureTable = new ArrayList<>();
    private List<With> withTable = new ArrayList<>();
    private List<Pattern> patternTable = new ArrayList<>();

    public Selector getSelector() {
        return selector;
    }

    public List<Predicate> getPredTable() {
        return predTable;
    }

    public List<Closure> getClosureTable() {
        return closureTable;
    }

    public List<With> getWithTable() {
        return withTable;
    }

    public List<Pattern> getPatternTable() {
        return patternTable;
    }
}
