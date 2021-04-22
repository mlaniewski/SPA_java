package pl.edu.pb.wi.spa.ast;

import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AST {
    private Map<Integer, Set<Integer>> callers;
    private Map<Integer, Set<Integer>> callersT;
    private Map<Integer, Set<Integer>> callees;
    private Map<Integer, Set<Integer>> calleesT;
    private Map<Integer, Set<String>> modifies;
    private Map<String, Set<Integer>> modified;
    private Map<Integer, Set<String>> uses;
    private Map<String, Set<Integer>> used;
    private Map<Integer, Integer> parent;
    private Map<Integer, Set<Integer>> parentT;
    private Map<Integer, Set<Integer>> children;
    private Map<Integer, Set<Integer>> childrenT;
    private Map<Integer, Set<Integer>> nextN;
    private Map<Integer, Set<Integer>> nextT;
    private Map<Integer, Set<Integer>> prevN;
    private Map<Integer, Set<Integer>> prevT;
    private Map<Integer, Set<String>> pattern;
    private Map<Integer, String> fullPattern;
    //public ?
    private List<Node<ASTNode>> procedures;
    private List<Node<ASTNode>> whiles;
    private List<Node<ASTNode>> ifs;
    private List<Node<ASTNode>> assigments;
    private Set<String> variables;
    private Set<String> constants;
    private List<Node<ASTNode>> callNodes;
    private List<Node<ASTNode>> programLines;


    public AST(Map<Integer, Set<Integer>> callers,
               Map<Integer, Set<Integer>> callersT,
               Map<Integer, Set<Integer>> callees,
               Map<Integer, Set<Integer>> calleesT,
               Map<Integer, Set<String>> modifies,
               Map<String, Set<Integer>> modified,
               Map<Integer, Set<String>> uses,
               Map<String, Set<Integer>> used,
               Map<Integer, Integer> parent,
               Map<Integer, Set<Integer>> parentT,
               Map<Integer, Set<Integer>> children,
               Map<Integer, Set<Integer>> childrenT,
               Map<Integer, Set<Integer>> nextN,
               Map<Integer, Set<Integer>> nextT,
               Map<Integer, Set<Integer>> prevN,
               Map<Integer, Set<Integer>> prevT,
               Map<Integer, Set<String>> pattern,
               Map<Integer, String> fullPattern,
               List<Node<ASTNode>> procedures,
               List<Node<ASTNode>> whiles,
               List<Node<ASTNode>> ifs,
               List<Node<ASTNode>> assignments,
               Set<String> variables,
               Set<String> constants,
               List<Node<ASTNode>> callNodes,
               List<Node<ASTNode>> programLines) {
        this.callers = callers;
        this.callersT = callersT;
        this.callees = callees;
        this.calleesT = calleesT;
        this.modifies = modifies;
        this.modified = modified;
        this.uses = uses;
        this.used = used;
        this.parent = parent;
        this.parentT = parentT;
        this.children = children;
        this.childrenT = childrenT;
        this.nextN = nextN;
        this.nextT = nextT;
        this.prevN = prevN;
        this.prevT = prevT;
        this.pattern = pattern;
        this.fullPattern = fullPattern;
        this.procedures = procedures;
        this.whiles = whiles;
        this.ifs = ifs;
        this.assigments = assignments;
        this.variables = variables;
        this.constants = constants;
        this.callNodes = callNodes;
        this.programLines = programLines;
    }

    @Deprecated
    private List<Node<ASTNode>> createNodeCollection(Set<Integer> begin, Set<Integer> end) {
        return null;
    }

    public Node<ASTNode> getProcedureByName(String procName) {
        Optional<Node<ASTNode>> procNode = procedures.stream().filter(node -> node.getData().getParam(NodeParamType.NAME).equals(procName)).findFirst();
        if (procNode.isPresent()) {
            return procNode.get();
        }
        return null;
    }
}
