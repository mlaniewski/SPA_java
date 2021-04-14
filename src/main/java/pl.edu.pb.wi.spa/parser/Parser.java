package pl.edu.pb.wi.spa.parser;

import jdk.nashorn.internal.codegen.CompilerConstants;
import pl.edu.pb.wi.spa.ast.builder.Builder;
import pl.edu.pb.wi.spa.exception.ParserException;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;
import pl.edu.pb.wi.spa.tree.NodeParamType;
import pl.edu.pb.wi.spa.tree.NodeType;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class Parser {
    private Iterator<String> tokens;
    private String token;
    private Builder builder = new Builder();
    private String procName;

    @Deprecated
    public List<Node<ASTNode>> getASTTree() {
        return builder.getAstTree();
    }

    public void parse(String filename) {
        try {
            tokens = createTokenList(filename).iterator();
            Node<ASTNode> program = builder.createNode(NodeType.PROGRAM);

            while (tokens.hasNext()) {
                builder.addChild(program, parseProcedure());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
    }

    private List<String> createTokenList(String filename) throws FileNotFoundException {
        List<String> list = new ArrayList<>();
        File file = new File(filename);
        Scanner sc = new Scanner(file);

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            int i = 0;
            int j = 0;
            int end = line.length();

            while (i != end) {
                if (Character.isDigit(line.charAt(i))) { //integer
                    while (j != end && Character.isDigit(line.charAt(j))) {
                        j++;
                    }
                    list.add(line.substring(i, j));
                } else if (Character.isLetterOrDigit(line.charAt(i))) { // name
                    while (j != end && Character.isLetterOrDigit(line.charAt(j))) {
                        j++;
                    }
                    list.add(line.substring(i, j));
                } else { // special char
                    if (j != end)
                    {
                        j++;
                    }
                    list.add(line.substring(i, j));
                }
                while (j != end && Character.isWhitespace(line.charAt(j))) {
                    j++;
                }
                i = j;
            }
        }
        return list;
    }

    private Node<ASTNode> parseProcedure() throws ParserException {
        token = tokens.next();
        validate(match(token, "procedure"), String.format("Expected 'procedure'. Found '%s'.", token));
        token = tokens.next();
        validate(matchName(token), String.format("Expected procedure name. Found '%s'.", token));
        procName = token;
        Node<ASTNode> procNode = builder.createNode(NodeType.PROCEDURE);
        builder.addNodeParameter(procNode, NodeParamType.NAME, procName);
        builder.addChild(procNode, parseStmtLst());
        return procNode;
    }

    private Node<ASTNode> parseStmtLst() throws ParserException {
        token = tokens.next();
        validate(match(token, "{"), String.format("Expected '{'. Found '%s'.", token));
        token = tokens.next();
        validate(!match(token, "}"), "StmtLst must have at least one stmt.");
        Node<ASTNode> stmtLstNode = builder.createNode(NodeType.STMTLST);
        while (!match(token, "}")) {
            builder.addChild(stmtLstNode, parseStmt());
        }
        //tokens.next();
        return stmtLstNode;
    }

    private Node<ASTNode> parseStmt() throws ParserException {
        if(match(token, "call")){
            return parseCall();
        }
        if(match(token, "while")){
            return parseWhile();
        }
        if(match(token, "if")){
            return parseIf();
        }
        return parseAssignment();
    }

    private Node<ASTNode> parseCall() throws ParserException {
        token = tokens.next();
        validate(matchName(token), String.format("Expected calle procedure name. Found '%s'.", token));
        Node<ASTNode> callNode = builder.createNode(NodeType.CALL);
        builder.addNodeParameter(callNode, NodeParamType.CALLER, procName);
        builder.addNodeParameter(callNode, NodeParamType.CALLEE, token);
        token = tokens.next();
        validate(match(token, ";"), String.format("Expected ';'. Found '%s'.", token));
        token = tokens.next();
        return callNode;
    }

    private Node<ASTNode> parseWhile() throws ParserException {
        //token = tokens.next();
        validate(matchName(token), String.format("Expected while. Found '%s'.", token));
        Node<ASTNode> whileNODE = builder.createNode(NodeType.WHILE);
        builder.addNodeParameter(whileNODE, NodeParamType.COND, token);
        token = tokens.next();
        builder.addChild(whileNODE, parseStmtLst());

        return whileNODE;
    }

    private Node<ASTNode> parseIf() throws ParserException {
        token = tokens.next();
        validate(matchName(token), String.format("Expected variable name. Found '%s'.", token));
        Node<ASTNode> ifNode = builder.createNode(NodeType.IF);
        builder.addNodeParameter(ifNode, NodeParamType.COND, token);
        token = tokens.next();
        validate(match(token, "then"), String.format("Expected 'then'. Found '%s'.", token));
        token = tokens.next();
        builder.addChild(ifNode, parseStmtLst());  //-->co tu powinno byc??
        validate(match(token, "else"), String.format("Expected 'else'. Found '%s'.", token));
        token = tokens.next();
        builder.addChild(ifNode, parseStmtLst());  //-->co tu powinno byc??
        return ifNode;
    }

    private Node<ASTNode> parseAssignment() throws ParserException {
        validate(matchName(token), String.format("Expected variable name. Found '%s'.", token));
        Node<ASTNode> assignment = builder.createNode(NodeType.ASSIGN);
        Node<ASTNode> variable = builder.createNode(NodeType.VARIABLE);
        builder.addNodeParameter(variable, NodeParamType.NAME, token);
        builder.addChild(assignment, variable);
        token = tokens.next();
        validate(match(token, "="), String.format("Expected '='. Found '%s'.", token));
        token = tokens.next();
        Node<ASTNode> expression = builder.createNode(NodeType.EXPRESSION);
        builder.addChild(expression, parseExpression());
        builder.addChild(assignment, expression);
        validate(match(token, ";"), String.format("Expected ';'. Found '%s'.", token));
        token = tokens.next();
        return assignment;
    }

    private Node<ASTNode> parseExpression() throws ParserException {
        Node<ASTNode> expr = parseTerm();
        while (match(token, "+") || match(token, "-")) {
            String op = token;
            token = tokens.next();
            Node<ASTNode> left = expr;
            Node<ASTNode> right = parseTerm();
            expr = builder.createNode(NodeType.OPERATOR);
            builder.addNodeParameter(expr, NodeParamType.NAME, op);
            builder.addChild(expr, left);
            builder.addChild(expr, right);
        }
        return expr;
    }

    private Node<ASTNode> parseTerm() throws ParserException {
        Node<ASTNode> term = parseFactor();
        while (match(token, "*"))
        {
            token = tokens.next();
            Node<ASTNode> left = term;
            Node<ASTNode> right = parseFactor();
            term = builder.createNode(NodeType.OPERATOR);
            builder.addNodeParameter(term, NodeParamType.NAME, "*");
            builder.addChild(term, left);
            builder.addChild(term, right);
        }
        return term;
    }

    private Node<ASTNode> parseFactor() throws ParserException {
        Node<ASTNode> factor;
        if (match(token, "(")) {
            token = tokens.next();
            factor = parseExpression();
            validate(match(token, ")"), String.format("Expected ')'. Found '%s'.",token));
        } else {
            validate(matchName(token) || matchInteger(token),
                    String.format("Expected variable name or constant. Found '%s'.", token));
            factor = builder.createNode(matchInteger(token) ? NodeType.CONSTANT : NodeType.VARIABLE);
            builder.addNodeParameter(factor, NodeParamType.NAME, token);
        }
        token = tokens.next();
        return factor;
    }

    boolean matchInteger(String tokenValue) {
        return tokenValue.matches("\\d+");
    }

    private boolean matchName(String tokenValue) {
        return tokenValue.matches("[a-z|A-Z]+[a-z|A-Z|\\d]*");
    }

    private boolean match(String tokenValue, String value) {
        return value.equals(tokenValue);
    }

    private void validate(boolean match, String error) throws ParserException {
        if (!match) {
            throw new ParserException(error);
        }
    }
}
