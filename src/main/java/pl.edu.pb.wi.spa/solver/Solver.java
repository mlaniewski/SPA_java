package pl.edu.pb.wi.spa.solver;

import pl.edu.pb.wi.spa.common.Predicate;
import pl.edu.pb.wi.spa.common.With;
import pl.edu.pb.wi.spa.common.Closure;
import pl.edu.pb.wi.spa.common.Selector;
import pl.edu.pb.wi.spa.common.Pattern;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

import java.util.List;

public class Solver {
    private List<String> results;
    private Selector selector;
    private List<Closure> closureTable;
    private List<Pattern> patternTable;
    private List<Predicate> predTable;
    private List<With> withTable;
    private Node<ASTNode> ast;

    public Solver(List<String> results, Selector selector, List<Closure> closureTable, List<Pattern> patternTable, List<Predicate> predTable, List<With> withTable, Node<ASTNode> ast) {
        this.results = results;
        this.selector = selector;
        this.closureTable = closureTable;
        this.patternTable = patternTable;
        this.predTable = predTable;
        this.withTable = withTable;
        this.ast = ast;
    }

    public void evaluate() {

    }
}
