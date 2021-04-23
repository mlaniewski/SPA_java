package pl.edu.pb.wi.spa.parser.query;

import pl.edu.pb.wi.spa.common.Predicate;
import pl.edu.pb.wi.spa.common.With;
import pl.edu.pb.wi.spa.common.Closure;
import pl.edu.pb.wi.spa.common.Selector;
import pl.edu.pb.wi.spa.common.Pattern;
import pl.edu.pb.wi.spa.exception.QueryException;
import pl.edu.pb.wi.spa.solver.Solver;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class QueriesParser {
    private Node<ASTNode> ast;
    private String query;
    private List<String> results;

    private Selector selector = new Selector();
    private List<Predicate> predTable = new ArrayList<>();
    private List<Closure> closureTable = new ArrayList<>();
    private List<With> withTable = new ArrayList<>();
    private List<Pattern> patternTable = new ArrayList<>();

    public QueriesParser(Node<ASTNode> ast, String query, List<String> results) {
        this.ast = ast;
        this.query = query;
        this.results = results;
    }

    public void parseQuery() throws QueryException {
        List<String> tokens = Arrays.asList(query.split(";"));
        Iterator<String> tokenIt = tokens.iterator();
        while (tokenIt.hasNext()) {
            String token = tokenIt.next();
            String[] elements = token.split(" ");
            if (icompare(elements[1], "select")) {
                parseResultPart(token);
                break;
            } else {
                parsePredicate(token);
            }
        }

        if (tokenIt.hasNext()) {
            throw new QueryException("Expected 'select' keyword");
        }
    }

    private void parsePredicate(String predicate) throws QueryException {
        predicate = predicate.trim();
        predicate = predicate.replace(",", " ,");
        List<String> tokens = Arrays.asList(predicate.trim().split(" "));

        Iterator<String> tokensIt = tokens.iterator();
        String currentPredicate;
        String token = tokensIt.next().toLowerCase();

        switch (token) {
            case "procedure":
                currentPredicate = "procedure";
                break;
            case "prog_line":
            case "stmt":
            case "stmtlist":
            case "stmtlst":
            case "statement":
                currentPredicate = "statement";
                break;
            case "assign":
                currentPredicate = "assign";
                break;
            case "while":
                currentPredicate = "while";
                break;
            case "if":
                currentPredicate = "if";
                break;
            case "var":
            case "variable":
                currentPredicate = "var";
                break;
            case "call":
                currentPredicate = "call";
                break;
            case "const":
            case "constant":
                currentPredicate = "const";
                break;
            default:
                throw new QueryException("Invalid predicate name: " + token);
        }

        while (tokensIt.hasNext()) {
            token = tokensIt.next();
            if (token.equals(",")) {
                continue;
            }
            predTable.add(new Predicate(currentPredicate, token));
        }
    }

    private void parseResultPart(String resutPart) throws QueryException {
        resutPart = resutPart.trim();
        resutPart = resutPart.replace("(", " ( ");
        resutPart = resutPart.replace(")", " )");
        resutPart = resutPart.replace(",", " ,");
        resutPart = resutPart.replace(".", " . ");
        List<String> tokens = Arrays.asList(resutPart.split(" "));
        Iterator<String> tokensIt = tokens.iterator();

        String token = tokensIt.next();
        if (!icompare(token, "select")) {
            throw new QueryException("Expected 'select' keyword at the beginning");
        }

        token = tokensIt.next(); //after select

        //====================
        // parse selector
        //====================
        if (token.equals("<")) { //TODO ??

        } else if (icompare(token, "boolean")) {
            token = tokensIt.next();
            selector.setType("boolean");
        } else {
            String tmpvar = token;
            token = tokensIt.next(); //after var name
            if (tokensIt.hasNext() && token.equals(".")) {
                selector.setType("property");
                token = tokensIt.next(); //after .
                if (icompare(token, "stmt") || icompare(token, "varname") || icompare(token, "procname") || icompare(token, "value")) {
                    selector.addVariableProperty(tmpvar, token);
                    token = tokensIt.next();	//after property name
                } else {
                    throw new QueryException("Unsupported variable property: " + token);
                }
            } else {
                if (checkResultPartElement(tmpvar)) {
                    selector.addVariable(tmpvar);
                    selector.setType("variable");
                }
            }
        }

        //==========================
        // parse after selector
        //==========================
        while (tokensIt.hasNext()) {
            if (icompare(token, "and")) {
                if (tokensIt.hasNext()) {
                    token = tokensIt.next(); //after and
                }
                else {
                    throw new QueryException("Unexpected 'and' at the end.");
                }
            }

            //==========================
            // parse such that
            //==========================
            if (icompare(token, "such")) {
                token = tokensIt.next(); //after such
                if (!icompare(token, "that")) {
                    throw new QueryException("Expected 'that' after 'such'");
                }
                token = tokensIt.next(); //after that

                while (true) {
                    String tmpvar = token.toLowerCase();
                    token = tokensIt.next();
                    if (!token.equals("(")) {
                        throw new QueryException("Expected '(' got: " + token);
                    }
                    token = tokensIt.next(); //after (
                    String lhs = token;
                    token = tokensIt.next();
                    if (!token.equals(",")) {
                        throw new QueryException("Expected ',' got: " + token);
                    }
                    token = tokensIt.next(); //after ,
                    String rhs = token;
                    token = tokensIt.next();
                    if (!token.equals(")")) {
                        throw new QueryException("Expected ')' got: " + token);
                    }
                    closureTable.add(new Closure(tmpvar, lhs, rhs));

                    if (tokensIt.hasNext()) {
                        token = tokensIt.next(); //after )
                    }
                    if (tokensIt.hasNext()) {
                        if (!icompare(token, "and")) {
                            break;
                        } else {
                            token = tokensIt.next(); //after and
                        }
                    } else {
                        break;
                    }
                }
            }
            //==========================
            // parse with
            //==========================
            else if (icompare(token, "with")) {
                token = tokensIt.next(); //after with

                while (true) {
                    With tmp = new With();
                    tmp.setLhsVarName(token);
                    token = tokensIt.next(); //after lhs var name

                    if (token.equals(".")) {
                        token = tokensIt.next(); //after .
                        token = token.replace("#", "");
                        tmp.setLhsIsProperty(true);
                        tmp.setLhsPropertyName(token);
                        token = tokensIt.next(); //after property name
                    }

                    tmp.setOperand(token);
                    token = tokensIt.next(); //after operand = or other if exists
                    tmp.setRhsVarName(token);

                    if (tokensIt.hasNext()) {
                        token = tokensIt.next(); //after rhs var name
                        if (token.equals(".")) {
                            token = tokensIt.next(); //after .
                            tmp.setRhsIsProperty(true);
                            tmp.setRhsPropertyName(token);

                            if (tokensIt.hasNext()) {
                                token = tokensIt.next(); //after rhs property name
                            }
                        }
                    }
                    withTable.add(tmp);

                    if (tokensIt.hasNext()) {
                        if (!icompare(token, "and")) {
                            break;
                        } else {
                            token = tokensIt.next(); //after and
                        }
                    } else {
                        break;
                    }
                }
            }
            //==========================
            // parse pattern
            //==========================
            else if (icompare(token, "pattern")) {
                token = tokensIt.next(); //after pattern
                String tmpvar = token;
                token = tokensIt.next(); //after var name
                if (!token.equals("(")) {
                    throw new QueryException("Expected '(' got: " + token);
                }
                token = tokensIt.next(); //after (
                String lhs = token;
                token = tokensIt.next();
                if (!token.equals(",")) {
                    throw new QueryException("Expected ',' got: " + token);
                }
                token = tokensIt.next(); //after ,
                String rhs = "";
                while (tokensIt.hasNext() && !token.equals(")")) {
                    rhs += token;
                    token = tokensIt.next();
                }
                if (!token.equals(")")) {
                    throw new QueryException("Expected ')' at the end of pattern, got: " + token);
                }
                patternTable.add(new Pattern(tmpvar, lhs, rhs));
                if (tokensIt.hasNext()) {
                    token = tokensIt.next(); //after )
                }
            }
            else {
                throw new QueryException("Invalid query part: " + token);
            }
        }

        Solver solver = new Solver(results, selector, closureTable, patternTable, predTable, withTable, ast);
        solver.evaluate();
    }

    private boolean checkResultPartElement(String token) throws QueryException {
        if (icompare(token, "procedure") || icompare(token, "prog_line") ||
                icompare(token, "stmt") || icompare(token, "stmtlist") ||
                icompare(token, "stmtlst") || icompare(token, "statement") ||
                icompare(token, "assign") || icompare(token, "while") ||
                icompare(token, "if") || icompare(token, "var") ||
                icompare(token, "variable") || icompare(token, "call") ||
                icompare(token, "const") || icompare(token, "constant")) {
            return true;
        } else {
            for (Predicate predicate : predTable) {
                if (icompare(predicate.getValue(), token)) {
                    return true;
                }
            }
        }
        throw new QueryException("Invalid searching result element: " + token);
    }


    private boolean icompare(String s1, String s2) {
        return s1.toLowerCase().equals(s2);
    }
}
