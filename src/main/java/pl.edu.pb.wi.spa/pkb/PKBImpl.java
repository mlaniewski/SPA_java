package pl.edu.pb.wi.spa.pkb;

import pl.edu.pb.wi.spa.common.AST;
import pl.edu.pb.wi.spa.exception.PKBException;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;

import java.util.*;

public class PKBImpl implements PKB {
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
    private Set<String> variables;
    private Set<String> constants;
    private AST ast;

    public PKBImpl(Map<Integer, Set<Integer>> callers, Map<Integer, Set<Integer>> callersT, Map<Integer, Set<Integer>> callees, Map<Integer, Set<Integer>> calleesT, Map<Integer, Set<String>> modifies, Map<String, Set<Integer>> modified, Map<Integer, Set<String>> uses, Map<String, Set<Integer>> used, Map<Integer, Integer> parent, Map<Integer, Set<Integer>> parentT, Map<Integer, Set<Integer>> children, Map<Integer, Set<Integer>> childrenT, Map<Integer, Set<Integer>> nextN, Map<Integer, Set<Integer>> nextT, Map<Integer, Set<Integer>> prevN, Map<Integer, Set<Integer>> prevT, Map<Integer, Set<String>> pattern, Map<Integer, String> fullPattern, Set<String> variables, Set<String> constants, AST ast) {
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
        this.variables = variables;
        this.constants = constants;
        this.ast = ast;
    }

    public List<Node<ASTNode>> getProcedures() {
        return ast.getProcedures();
    }

    public List<Node<ASTNode>> getWhiles() {
        return ast.getWhiles();
    }

    public List<Node<ASTNode>> getIfs() {
        return ast.getIfs();
    }

    public List<Node<ASTNode>> getAssignments() {
        return ast.getAssignments();
    }

    public Set<String> getVariables() {
        return variables;
    }

    //TODO to nie powinno byc tak
    @Override
    public Set<String> getConstants() {
        return constants;
    }

    public List<Node<ASTNode>> getCallNodes() {
        return ast.getCallNodes();
    }

    public List<Node<ASTNode>> getProgramLines() {
        return ast.getProgramLines();
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

    @Override
    public Node<ASTNode> getProcedureByName(String procName) {
        return ast.getProcedureByName(procName);
    }

    @Override
    public Node<ASTNode> getStmtByLineNumber(int lineNumber) throws PKBException {
        int idx = lineNumber - 1;
        if (ast.getProgramLines().size() <= idx) {
            throw new PKBException();
        }
        return ast.getProgramLines().get(idx);
    }

    //*****************
    // PKB API
    //*****************
    @Override
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

    @Override
    public List<Node<ASTNode>> getFollowing(Node<ASTNode> s1, boolean _transient) {
        List<Node<ASTNode>> following = new ArrayList<>();
        List<Node<ASTNode>> s1Childs = s1.getParent().getChildren();
        Iterator<Node<ASTNode>> sIt = s1Childs.iterator();
        Node<ASTNode> end = s1Childs.get(s1Childs.size() - 1);

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

    @Override
    public List<Node<ASTNode>> getFollowed(Node<ASTNode> s2, boolean _transient) {
        List<Node<ASTNode>> followed = new ArrayList<>();
        Node<ASTNode> end = s2;
        Iterator<Node<ASTNode>> sIt = s2.getParent().getChildren().iterator();

        Node<ASTNode> s = sIt.next();
        while (!s.getData().equals(end.getData())) {
            if (_transient) {
                followed.add(s);
                if (sIt.hasNext()) {
                    s = sIt.next();
                }
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

    @Override
    public boolean checkParent(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient) {
        int s1Id = s1.getData().getId();
        int s2Id = s2.getData().getId();
        return _transient ?
                parentT.get(s2Id).contains(s1Id) :
                parent.get(s2Id) == s1Id;
    }

    @Override
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

    @Override
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

    @Override
    public boolean checkUses(Node<ASTNode> n, String var) {
        Set<String> vars = uses.get(n.getData().getId());
        return vars.contains(var);
    }

    @Override
    public List<String> getUsed(Node<ASTNode> n) {
        Set<String> vars = uses.get(n.getData().getId());
        if (vars == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(vars);
    }

    @Override
    public List<Node<ASTNode>> getUsing(String var) {
        Set<Integer> nodes = used.get(var);
        return createNodeCollection(nodes);
    }

    @Override
    public boolean checkModifies(Node<ASTNode> n, String var) {
        Set<String> vars = modifies.get(n.getData().getId());
        return vars.contains(var);
    }

    @Override
    public List<String> getModified(Node<ASTNode> n) {
        Set<String> vars = modifies.get(n.getData().getId());
        return new ArrayList<>(vars);
    }

    @Override
    public List<Node<ASTNode>> getModifying(String var) {
        Set<Integer> nodes = modified.get(var);
        return createNodeCollection(nodes);
    }

    @Override
    public boolean checkCalls(Node<ASTNode> p1, Node<ASTNode> p2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? callersT : callers;
        int p1Id = p1.getData().getId();
        int p2Id = p2.getData().getId();
        return rel.get(p1Id).contains(p2Id);
    }

    @Override
    public List<Node<ASTNode>> getCallees(Node<ASTNode> p1, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? callersT : callers;
        return createNodeCollection(rel.get(p1.getData().getId()));
    }

    @Override
    public List<Node<ASTNode>> getCallers(Node<ASTNode> p2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? calleesT : callees;
        return createNodeCollection(rel.get(p2.getData().getId()));
    }

    @Override
    public boolean checkNext(Node<ASTNode> s1, Node<ASTNode> s2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? nextT : nextN;
        int s1Id = s1.getData().getId();
        int s2Id = s2.getData().getId();
        if (rel.get(s1Id) == null) {
            return false;
        }
        return rel.get(s1Id).contains(s2Id);
    }

    @Override
    public List<Node<ASTNode>> getNext(Node<ASTNode> s1, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? nextT : nextN;
        return createNodeCollection(rel.get(s1.getData().getId()));
    }

    @Override
    public List<Node<ASTNode>> getPrev(Node<ASTNode> s2, boolean _transient) {
        Map<Integer, Set<Integer>> rel = _transient ? prevT : prevN;
        return createNodeCollection(rel.get(s2.getData().getId()));
    }

    @Override
    public List<Node<ASTNode>> getPattern(String var, String expr) {
        List<Node<ASTNode>> result = new ArrayList<>();
        if (!var.equals("_")) {
            var  = var.substring(1, var.length() - 1);
        }
        if (expr.equals("_")) {
            if (var.equals("_")) {
                return ast.getAssignments();
            }
            for (Node<ASTNode> assignment : ast.getAssignments()) {
                Node<ASTNode> child = assignment.getChildren().iterator().next();
                if (child.getData().getParam(NodeParamType.NAME).equals(var)) {
                    result.add(assignment);
                }
            }
        }
        else if (expr.charAt(0) == '_') {
            String p;
            if (expr.length() < 4) {
                p = expr.substring(2);
            } else {
                 p = expr.substring(2, expr.length() - 2);
            }
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
            String p = expr.substring(1, expr.length() - 1);
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

    @Override
    public Set<String> getAllPropertyValues(String propName) {
        Set<String> result = new HashSet<>();
        if (propName.equals("stmt")) {
            int lines = ast.getProgramLines().size();
            for (int i = 1; i <= lines; i++) {
                result.add(String.valueOf(i));
            }
        } else if (propName.equals("value")) {
            result.addAll(constants);
        } else if (propName.equals("procName")) {
            List<Node<ASTNode>> procedures = ast.getProcedures();
            for (Node<ASTNode> p : procedures) {
                result.add(p.getData().getParam(NodeParamType.NAME));
            }
        } else if (propName.equals("varName")) {
            result.addAll(variables);
        }

        return result;
    }

    @Override
    public List<Node<ASTNode>> getAllValues(String predType) {
        List<Node<ASTNode>> result = new ArrayList<>();
        List<List<Node<ASTNode>>> nodeTypes = new ArrayList<>();
        if (predType.equals("procedure")) {
            nodeTypes.add(ast.getProcedures());
        } else if (predType.equals("statement")) {
            nodeTypes.add(ast.getWhiles());
            nodeTypes.add(ast.getIfs());
            nodeTypes.add(ast.getAssignments());
            nodeTypes.add(ast.getCallNodes());
        } else if (predType.equals("while")) {
            nodeTypes.add(ast.getWhiles());
        } else if (predType.equals("if")) {
            nodeTypes.add(ast.getIfs());
        }
        else if (predType.equals("call")) {
            nodeTypes.add(ast.getCallNodes());
        } else if (predType.equals("assign")) {
            nodeTypes.add(ast.getAssignments());
        }

        for (List<Node<ASTNode>> nodeType : nodeTypes) {
            result.addAll(nodeType);
        }
        return result;
    }

    @Override
    public List<String> getAllVariables() {
        return new ArrayList<>(variables);
    }
}
