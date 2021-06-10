package spa.query.parser;

import spa.common.Closure;
import spa.common.Pattern;
import spa.common.Predicate;
import spa.common.With;
import spa.exception.SPAException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class QueriesParser {
    private QueryTree queryTree;

    public QueriesParser() {
        this.queryTree = new QueryTree();
    }

    public void parse(String query) throws SPAException {
        List<String> tokens = Arrays.asList(query.split(";"));
        Iterator<String> tokenIt = tokens.iterator();
        while (tokenIt.hasNext()) {
            String token = tokenIt.next();
            String[] elements = token.split(" ");
            if (compare(elements[1], "select")) {
                parseResultPart(token);
                break;
            } else {
                parsePredicate(token);
            }
        }

        if (tokenIt.hasNext()) {
            throw new SPAException("Expected 'select' keyword");
        }
    }

    private void parsePredicate(String predicate) throws SPAException {
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
                throw new SPAException("Invalid predicate name: " + token);
        }

        while (tokensIt.hasNext()) {
            token = tokensIt.next();
            if (token.equals(",")) {
                continue;
            }
            queryTree.getPredTable().add(new Predicate(currentPredicate, token));
        }
    }

    private void parseResultPart(String resutPart) throws SPAException, SPAException {
        resutPart = resutPart.trim();
        resutPart = resutPart.replace("(", " ( ");
        resutPart = resutPart.replace(")", " )");
        resutPart = resutPart.replace(",", " , ");
        resutPart = resutPart.replace(".", " . ");
        List<String> tokens = Arrays.asList(resutPart.split(" "));
        tokens = tokens.stream().map(String::trim).filter((t)->!t.equals("")).collect(Collectors.toList());
        Iterator<String> tokensIt = tokens.iterator();

        String token = tokensIt.next();
        if (!compare(token, "select")) {
            throw new SPAException("Expected 'select' keyword at the beginning");
        }

        token = tokensIt.next();

        // parse selector
        if (compare(token, "boolean")) {
            token = tokensIt.next();
            queryTree.getSelector().setType("boolean");
        } else {
            String tmpvar = token;
            token = tokensIt.next();
            if (checkResultPartElement(tmpvar)) {
                queryTree.getSelector().addVariable(tmpvar);
                queryTree.getSelector().setType("variable");
            }
        }

        // parse after selector
        while (tokensIt.hasNext()) {
            if (compare(token, "and")) {
                if (tokensIt.hasNext()) {
                    token = tokensIt.next();
                }
                else {
                    throw new SPAException("Unexpected 'and' at the end.");
                }
            }

            // parse such that
            if (compare(token, "such")) {
                token = tokensIt.next();
                if (!compare(token, "that")) {
                    throw new SPAException("Expected 'that' after 'such'");
                }
                token = tokensIt.next();

                boolean end = false;
                while (!end) {
                    String tmpvar = token.toLowerCase();
                    token = tokensIt.next();
                    if (!token.equals("(")) {
                        throw new SPAException("Expected '(' got: " + token);
                    }
                    token = tokensIt.next();
                    String lhs = token;
                    token = tokensIt.next();
                    if (!token.equals(",")) {
                        throw new SPAException("Expected ',' got: " + token);
                    }
                    token = tokensIt.next();
                    String rhs = token;
                    token = tokensIt.next();
                    if (!token.equals(")")) {
                        throw new SPAException("Expected ')' got: " + token);
                    }
                    queryTree.getClosureTable().add(new Closure(tmpvar, lhs, rhs));

                    if (tokensIt.hasNext()) {
                        token = tokensIt.next();
                    }
                    if (tokensIt.hasNext()) {
                        if (!compare(token, "and")) {
                            end = true;
                        } else {
                            token = tokensIt.next();
                        }
                    } else {
                        end = true;
                    }
                }
            }

            // parse with
            else if (compare(token, "with")) {
                token = tokensIt.next();

                boolean end = false;
                while (!end) {
                    With tmp = new With();
                    tmp.setLhsVarName(token);
                    token = tokensIt.next();

                    if (token.equals(".")) {
                        token = tokensIt.next();
                        token = token.replace("#", "");
                        tmp.setLhsIsProperty(true);
                        tmp.setLhsPropertyName(token);
                        token = tokensIt.next();
                    }

                    tmp.setOperand(token);
                    token = tokensIt.next();
                    tmp.setRhsVarName(token);

                    if (tokensIt.hasNext()) {
                        token = tokensIt.next();
                        if (token.equals(".")) {
                            token = tokensIt.next();
                            tmp.setRhsIsProperty(true);
                            tmp.setRhsPropertyName(token);

                            if (tokensIt.hasNext()) {
                                token = tokensIt.next();
                            }
                        }
                    }
                    queryTree.getWithTable().add(tmp);

                    if (tokensIt.hasNext()) {
                        if (!compare(token, "and")) {
                            end = true;
                        } else {
                            token = tokensIt.next();
                        }
                    } else {
                        end = true;
                    }
                }
            }

            // parse pattern
            else if (compare(token, "pattern")) {
                token = tokensIt.next();
                String tmpvar = token;
                token = tokensIt.next();
                if (!token.equals("(")) {
                    throw new SPAException("Expected '(' got: " + token);
                }
                token = tokensIt.next();
                String lhs = token;
                token = tokensIt.next();
                if (!token.equals(",")) {
                    throw new SPAException("Expected ',' got: " + token);
                }
                token = tokensIt.next();
                String rhs = "";
                while (tokensIt.hasNext() && !token.equals(")")) {
                    rhs += token;
                    token = tokensIt.next();
                }
                if (!token.equals(")")) {
                    throw new SPAException("Expected ')' at the end of pattern, got: " + token);
                }
                queryTree.getPatternTable().add(new Pattern(tmpvar, lhs, rhs));
                if (tokensIt.hasNext()) {
                    token = tokensIt.next();
                }
            }
            else {
                throw new SPAException("Invalid query part: " + token);
            }
        }
    }

    private boolean checkResultPartElement(String token) throws SPAException {
        if (compare(token, "procedure") || compare(token, "prog_line") || compare(token, "stmt") || compare(token, "stmtlist") ||
                compare(token, "stmtlst") || compare(token, "statement") || compare(token, "assign") || compare(token, "while") ||
                compare(token, "if") || compare(token, "var") || compare(token, "variable") || compare(token, "call") ||
                compare(token, "const") || compare(token, "constant")) {
            return true;
        } else {
            for (Predicate predicate : queryTree.getPredTable()) {
                if (compare(predicate.getValue(), token)) {
                    return true;
                }
            }
        }
        throw new SPAException("Invalid searching result element: " + token);
    }

    private boolean compare(String s1, String s2) {
        return s1.toLowerCase().equals(s2.toLowerCase());
    }

    public QueryTree getQueryTree() {
        return queryTree;
    }
}
