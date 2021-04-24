package pl.edu.pb.wi.spa.ast;

import pl.edu.pb.wi.spa.exception.PKBException;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;
import pl.edu.pb.wi.spa.tree.NodeType;

import java.util.*;

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

    public List<Node<ASTNode>> getProcedures() {
        return procedures;
    }

    public List<Node<ASTNode>> getWhiles() {
        return whiles;
    }

    public List<Node<ASTNode>> getIfs() {
        return ifs;
    }

    public List<Node<ASTNode>> getAssigments() {
        return assigments;
    }

    public Set<String> getVariables() {
        return variables;
    }

    public Set<String> getConstants() {
        return constants;
    }

    public List<Node<ASTNode>> getCallNodes() {
        return callNodes;
    }

    public List<Node<ASTNode>> getProgramLines() {
        return programLines;
    }

    private List<Node<ASTNode>> createNodeCollection(Set<Integer> set) {
        List<Node<ASTNode>> nodes = new ArrayList<>();
        if (set != null) {
            for (Integer val : set) {
                nodes.add(ASTNode.getNodeById(val));
            }
        }
        return nodes;
    }

    public Node<ASTNode> getProcedureByName(String procName) {
        Optional<Node<ASTNode>> procNode = procedures.stream().filter(node -> node.getData().getParam(NodeParamType.NAME).equals(procName)).findFirst();
        if (procNode.isPresent()) {
            return procNode.get();
        }
        return null;
    }

    public Node<ASTNode> getStmtByLineNumber(int lineNumber) throws PKBException {
        int idx = lineNumber - 1;
        if (programLines.size() <= idx) {
            throw new PKBException();
        }
        return programLines.get(idx);
    }

    public int getLineNumber(Node<ASTNode> node) {
        return node.getData().getLineNumber();
    }

    public NodeType getNodeType(Node<ASTNode> node) {
        return node.getData().getNodeType();
    }

    //*****************
    // PKB API
    //*****************
    //TODO sprawdzic
    public boolean checkFollows(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient) {
        Iterator<Node<ASTNode>> sIt = s1.getChildren().iterator();
        ASTNode s = sIt.next().getData();
        if (_transient) {
            Node<ASTNode> end = s1.getParent().getChildren().get(s1.getParent().getChildren().size()-1);
            while (!s.equals(end.getData())) {
                if (s.equals(s2.getData())) {
                    return true;
                }
                s = sIt.next().getData();
            }
            return false;
        } else {
            return s.equals(s2.getData());
        }
    }

    //TODO sprawdzic
    public List<Node<ASTNode>> getFollowing(Node<ASTNode> s1, boolean _transient) {
        List<Node<ASTNode>> following = new ArrayList<>();
        Iterator<Node<ASTNode>> sIt = s1.getChildren().iterator();
        Node<ASTNode> end = s1.getParent().getChildren().get(s1.getParent().getChildren().size()-1);

        Node<ASTNode> s = sIt.next();
        if (_transient) {
            while (!s.getData().equals(end.getData())) {
                following.add(s);
                if (!sIt.hasNext()) {
                    break;
                }
                s = sIt.next();
            }
        } else {
            if (!s.getData().equals(end.getData())) {
                following.add(s);
            }
        }
        return following;
    }

    //TODO sprawdzic
    public List<Node<ASTNode>> getFollowed(Node<ASTNode> s2, boolean _transient) {
        List<Node<ASTNode>> followed = new ArrayList<>();
        Node<ASTNode> end = s2.getChildren().iterator().next();
        Iterator<Node<ASTNode>> sIt = s2.getParent().getChildren().iterator();

        Node<ASTNode> s = sIt.next();
        while (!s.getData().equals(end.getData())) {
            if (_transient) {
                followed.add(s);
                s = sIt.next();
            } else {
                Node<ASTNode> prev = s;
                s = sIt.next();
                if (s.getData().equals(end.getData())) {
                    followed.add(prev);
                }
            }
        }
        return followed;
    }

    public boolean checkParent(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient) {
        int s1Id = s1.getData().getId();
        int s2Id = s2.getData().getId();
        return _transient ?
                parentT.get(s2Id).contains(s1Id) :
                parent.get(s2Id) == s1Id;
    }

    //TODO sprawdzic
    public List<Node<ASTNode>> getParent(Node<ASTNode> s1, boolean _transient) {
        List<Node<ASTNode>> parents = new ArrayList<>();
        int s1Id = s1.getData().getId();

        if (!_transient) {
            Node<ASTNode> p = ASTNode.getNodeById(parent.get(s1Id));
            if (p != null) {
                parents.add(p);
            }
        } else {
            Set<Integer> idSet = parentT.get(s1Id);
            if (idSet != null) {
                for (Integer id : idSet) {
                    Node<ASTNode> p = ASTNode.getNodeById(id);
                    if (p != null) {
                        parents.add(p);
                    }
                }
            }
        }
        return parents;
    }

    //TODO sprawdzic
    public List<Node<ASTNode>> getChildren(Node<ASTNode> s2, boolean _transient) {
        List<Node<ASTNode>> result = new ArrayList<>();
        int s2Id = s2.getData().getId();
        Map<Integer, Set<Integer>> rel = _transient ? childrenT : children;
        Set<Integer> relSet = rel.get(s2Id);

        if (relSet != null) {
            for (Integer id : relSet) {
                result.add(ASTNode.getNodeById(id));
            }
        }
        return result;
    }

    public boolean checkUses(Node<ASTNode> n, String var) {
        Set<String> vars = uses.get(n.getData().getId());
        return vars.contains(var);
    }

    public List<String> getUsed(Node<ASTNode> n) {
        Set<String> vars = uses.get(n.getData().getId());
        return new ArrayList<>(vars);
    }

    public List<Node<ASTNode>> getUsing(String var) {
        Set<Integer> nodes = used.get(var);
        return createNodeCollection(nodes);
    }

    public boolean checkModifies(Node<ASTNode> n, String var) {
        Set<String> vars = modifies.get(n.getData().getId());
        return vars.contains(var);
    }

    public List<String> getModified(Node<ASTNode> n) {
        Set<String> vars = modifies.get(n.getData().getId());
        return new ArrayList<>(vars);
    }

    public List<Node<ASTNode>> getModifying(String var) {
        Set<Integer> nodes = modified.get(var);
        return createNodeCollection(nodes);
    }

    public boolean checkCalls(Node<ASTNode> p1, Node<ASTNode> p2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? callersT : callers;
        int p1Id = p1.getData().getId();
        int p2Id = p2.getData().getId();
        return rel.get(p1Id).contains(p2Id);
    }

    public List<Node<ASTNode>> getCallees(Node<ASTNode> p1, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? callersT : callers;
        return createNodeCollection(rel.get(p1.getData().getId()));
    }

    public List<Node<ASTNode>> getCallers(Node<ASTNode> p2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? calleesT : callees;
        return createNodeCollection(rel.get(p2.getData().getId()));
    }

    public boolean checkNext(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? nextT : nextN;
        int s1Id = s1.getData().getId();
        int s2Id = s2.getData().getId();
        return rel.get(s1Id).contains(s2Id);
    }

    public List<Node<ASTNode>> getNext(Node<ASTNode> s1, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? nextT : nextN;
        return createNodeCollection(rel.get(s1.getData().getId()));
    }

    public List<Node<ASTNode>> getPrev(Node<ASTNode> s2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? prevT : prevN;
        return createNodeCollection(rel.get(s2.getData().getId()));
    }

    //TODO sprawdzić
    public List<Node<ASTNode>> getPattern(String var, String expr) {
        List<Node<ASTNode>> result = new ArrayList<>();
        if (!var.equals("_")) {
            var  = var.substring(1, var.length() - 2);
        }
        if (expr.equals("_")) {
            if (var.equals("_")) {
                return assigments;
            }
            for (Node<ASTNode> assignment : assigments) {
                Node<ASTNode> child = assignment.getChildren().iterator().next();
                if (child.getData().getParam(NodeParamType.NAME).equals(var)) {
                    result.add(assignment);
                }
            }
        }
        if (expr.charAt(0) == '_') {
            String p = expr.substring(2, expr.length() - 4);
            String finalVar = var;
            pattern.forEach((first, second) -> {
                Node<ASTNode> node = ASTNode.getNodeById(first);
                Node<ASTNode> child = node.getChildren().iterator().next();
                if (child.getData().getParam(NodeParamType.NAME).equals(finalVar) || finalVar.equals("_")) {
                    for (String ptrn : second) {
                        if (ptrn.equals(p)) {
                            result.add(node);
                            break;
                        }
                    }
                }
            });
        } else {
            String p = expr.substring(1, expr.length() - 2);
            String finalVar = var;
            fullPattern.forEach((first, second) -> {
                Node<ASTNode> node = ASTNode.getNodeById(first);
                Node<ASTNode> child = node.getChildren().iterator().next();
                if (second.equals(p) &&
                        (child.getData().getParam(NodeParamType.NAME).equals(finalVar) || finalVar.equals("_"))) {
                    result.add(node);
                }
            });
        }

        return result;
    }
}
