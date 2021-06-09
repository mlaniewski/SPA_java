package spa;

import spa.common.AST;
import spa.exception.SPAException;
import spa.pkb.PKB;
import spa.pkb.builder.PKBBuilder;
import spa.query.evaluator.QueryEvaluator;
import spa.query.parser.QueriesParser;
import spa.query.parser.QueryTree;
import spa.query.result.QueryResultProjector;
import spa.source.parser.Parser;
import spa.source.parser.ParserImpl;

import java.util.Scanner;

public class SPA {
    public static void main(String[] args) throws SPAException {
        if (args.length < 1) {
            return;
        }
        final String sourceFile = args[0];
        Parser parser = new ParserImpl();
        AST ast = parser.parse(sourceFile);

        //ast.printTree(false);

        PKBBuilder builder = new PKBBuilder(ast);
        PKB pkb = builder.buildPKB();

        System.out.println("Ready");

        Scanner sc = new Scanner(System.in);
        while (true) {
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
}
