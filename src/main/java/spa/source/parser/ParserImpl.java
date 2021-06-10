package spa.source.parser;

import spa.common.AST;
import spa.exception.SPAException;
import spa.tree.ASTNode;
import spa.tree.Node;
import spa.tree.NodeParamType;
import spa.tree.NodeType;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class ParserImpl implements Parser {
    private Iterator<String> tokens;
    private String token;
    private String procName;
    private AST ast;

    @Override
    public AST parse(String filename) {
        ast = new AST();
        try {
            tokens = createTokensIterator(filename);
            Node<ASTNode> program = ast.createNode(NodeType.PROGRAM);

            token = tokens.next();
            while (tokens.hasNext()) {
                ast.addChild(program, parseProcedure());
            }
            ast.addLineNumbers();
        } catch (FileNotFoundException | SPAException e) {
            e.printStackTrace();
        }
        return ast;
    }

    private Iterator<String> createTokensIterator(String filename) throws FileNotFoundException {
        List<String> list = new ArrayList<>();
        File file = new File(filename);
        Scanner sc = new Scanner(file);

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            int i = 0;
            int j = 0;
            int end = line.length();

            while (i != end) {
                if (Character.isDigit(line.charAt(i))) {
                    while (j != end && Character.isDigit(line.charAt(j))) {
                        j++;
                    }
                } else if (Character.isLetterOrDigit(line.charAt(i))) {
                    while (j != end && Character.isLetterOrDigit(line.charAt(j))) {
                        j++;
                    }
                } else { // other char
                    j++;
                }
                list.add(line.substring(i, j));
                while (j != end && Character.isWhitespace(line.charAt(j))) {
                    j++;
                }
                i = j;
            }
        }
        return list.iterator();
    }

    private Node<ASTNode> parseProcedure() throws SPAException {
        validate(match(token, "procedure"), String.format("Expected 'procedure'. Found '%s'.", token));
        token = tokens.next();
        validate(matchName(token), String.format("Expected procedure name. Found '%s'.", token));
        procName = token;
        token = tokens.next();
        Node<ASTNode> procNode = ast.createNode(NodeType.PROCEDURE);
        procNode.getData().putParam(NodeParamType.NAME, procName);
        ast.addChild(procNode, parseStmtLst(NodeType.STMTLST));
        return procNode;
    }

    private Node<ASTNode> parseStmtLst(NodeType nodeType) throws SPAException {
        validate(match(token, "{"), String.format("Expected '{'. Found '%s'.", token));
        token = tokens.next();
        validate(!match(token, "}"), "StmtLst must have at least one stmt.");
        Node<ASTNode> stmtLstNode = ast.createNode(nodeType);
        while (!match(token, "}")) {
            ast.addChild(stmtLstNode, parseStmt());
        }
        if (tokens.hasNext()) {
            token = tokens.next();
        }
        return stmtLstNode;
    }

    private Node<ASTNode> parseStmt() throws SPAException {
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

    private Node<ASTNode> parseCall() throws SPAException {
        token = tokens.next();
        validate(matchName(token), String.format("Expected calle procedure name. Found '%s'.", token));
        Node<ASTNode> callNode = ast.createNode(NodeType.CALL);
        callNode.getData().putParam(NodeParamType.CALLER, procName);
        callNode.getData().putParam(NodeParamType.CALLED_BY, token);
        token = tokens.next();
        validate(match(token, ";"), String.format("Expected ';'. Found '%s'.", token));
        token = tokens.next();
        return callNode;
    }

    private Node<ASTNode> parseWhile() throws SPAException {
        token = tokens.next();
        validate(matchName(token), String.format("Expected while. Found '%s'.", token));
        Node<ASTNode> whileNODE = ast.createNode(NodeType.WHILE);
        whileNODE.getData().putParam(NodeParamType.COND, token);
        token = tokens.next();
        ast.addChild(whileNODE, parseStmtLst(NodeType.STMTLST));

        return whileNODE;
    }

    private Node<ASTNode> parseIf() throws SPAException {
        token = tokens.next();
        validate(matchName(token), String.format("Expected variable name. Found '%s'.", token));
        Node<ASTNode> ifNode = ast.createNode(NodeType.IF);
        ifNode.getData().putParam(NodeParamType.COND, token);
        token = tokens.next();
        validate(match(token, "then"), String.format("Expected 'then'. Found '%s'.", token));
        token = tokens.next();
        ast.addChild(ifNode, parseStmtLst(NodeType.THEN));
        validate(match(token, "else"), String.format("Expected 'else'. Found '%s'.", token));
        token = tokens.next();
        ast.addChild(ifNode, parseStmtLst(NodeType.ELSE));
        return ifNode;
    }

    private Node<ASTNode> parseAssignment() throws SPAException {
        validate(matchName(token), String.format("Expected variable name. Found '%s'.", token));
        Node<ASTNode> assignment = ast.createNode(NodeType.ASSIGN);
        Node<ASTNode> variable = ast.createNode(NodeType.VARIABLE);
        variable.getData().putParam(NodeParamType.NAME, token);
        ast.addChild(assignment, variable);
        token = tokens.next();
        validate(match(token, "="), String.format("Expected '='. Found '%s'.", token));
        token = tokens.next();
        Node<ASTNode> expression = ast.createNode(NodeType.EXPRESSION);
        ast.addChild(expression, parseExpression());
        ast.addChild(assignment, expression);
        validate(match(token, ";"), String.format("Expected ';'. Found '%s'.", token));
        token = tokens.next();
        return assignment;
    }

    private Node<ASTNode> parseExpression() throws SPAException {
        Node<ASTNode> expr = parseTerm();
        while (match(token, "+") || match(token, "-")) {
            String operatorName = token;
            token = tokens.next();
            Node<ASTNode> left = expr;
            Node<ASTNode> right = parseTerm();
            expr = ast.createNode(NodeType.OPERATOR);
            expr.getData().putParam(NodeParamType.NAME, operatorName);
            ast.addChild(expr, left);
            ast.addChild(expr, right);
        }
        return expr;
    }

    private Node<ASTNode> parseTerm() throws SPAException {
        Node<ASTNode> term = parseFactor();
        while (match(token, "*"))
        {
            token = tokens.next();
            Node<ASTNode> left = term;
            Node<ASTNode> right = parseFactor();
            term = ast.createNode(NodeType.OPERATOR);
            term.getData().putParam(NodeParamType.NAME, "*");
            ast.addChild(term, left);
            ast.addChild(term, right);
        }
        return term;
    }

    private Node<ASTNode> parseFactor() throws SPAException {
        Node<ASTNode> factor;
        if (match(token, "(")) {
            token = tokens.next();
            factor = parseExpression();
            validate(match(token, ")"), String.format("Expected ')'. Found '%s'.",token));
        } else {
            validate(matchName(token) || matchNumber(token),
                    String.format("Expected variable name or constant. Found '%s'.", token));
            factor = ast.createNode(matchNumber(token) ? NodeType.CONSTANT : NodeType.VARIABLE);
            factor.getData().putParam(NodeParamType.NAME, token);
        }
        token = tokens.next();
        return factor;
    }

    private boolean matchNumber(String tokenValue) {
        try {
            Integer.valueOf(tokenValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean matchName(String tokenValue) {
        return tokenValue.matches("[a-z|A-Z]+\\w*");
    }

    private boolean match(String tokenValue, String value) {
        return value.equals(tokenValue);
    }

    private void validate(boolean match, String error) throws SPAException {
        if (!match) {
            throw new SPAException(error);
        }
    }
}
