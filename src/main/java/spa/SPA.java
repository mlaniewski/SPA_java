package spa;

import spa.common.AST;
import spa.exception.PKBException;
import spa.exception.QueryException;
import spa.pkb.PKB;
import spa.pkb.builder.Builder;
import spa.query.evaluator.QueryEvaluator;
import spa.query.parser.QueriesParser;
import spa.query.parser.QueryTree;
import spa.query.result.QueryResultProjector;
import spa.source.parser.Parser;
import spa.source.parser.ParserImpl;

import java.util.Scanner;

public class SPA {
    public static void main(String[] args) throws QueryException, PKBException {
        //final String sourceFile = "tests/iteracja1/prog3.simple";
        if (args.length < 1) {
            return;
        }
        final String sourceFile = args[0];
        Parser parser = new ParserImpl();
        AST ast = parser.parse(sourceFile);

        //ast.printTree();

        Builder builder = new Builder(ast);
        PKB pkb = builder.buildPKB();

        System.out.println("Ready");

        Scanner sc = new Scanner(System.in);
        String q1 = sc.nextLine();
        String q2 = sc.nextLine();
        String query = q1 + " " + q2;

        QueriesParser queriesParser = new QueriesParser();
        queriesParser.parse(query);

        QueryTree queryTree = queriesParser.getQueryTree();

        QueryEvaluator evaluator = new QueryEvaluator(queryTree, pkb);
        evaluator.prepareResults();
        evaluator.evaluate();

        QueryResultProjector projector = new QueryResultProjector();
        projector.printResult(evaluator.getResults());
    }
}
