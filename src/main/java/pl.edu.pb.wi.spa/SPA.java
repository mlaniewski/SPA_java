package pl.edu.pb.wi.spa;

import pl.edu.pb.wi.spa.ast.AST;
import pl.edu.pb.wi.spa.ast.builder.Builder;
import pl.edu.pb.wi.spa.exception.PKBException;
import pl.edu.pb.wi.spa.exception.QueryException;
import pl.edu.pb.wi.spa.frontend.parser.Parser;
import pl.edu.pb.wi.spa.frontend.parser.ParserImpl;
import pl.edu.pb.wi.spa.parser.query.QueriesParser;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class SPA {
    public static void main(String[] args) throws QueryException, PKBException {
        final String sourceFile = "tests/iteracja1/prog3.simple";
        Builder builder = new Builder();
        Parser parser = new ParserImpl(builder);

        parser.parse(sourceFile);

        Node<ASTNode> astTree = builder.getAstTree();
        printTree(astTree);

        AST ast = builder.getAST();

        System.out.println("Ready");

        Scanner sc = new Scanner(System.in);
        while (true) {
            String q1 = sc.nextLine();
            String q2 = sc.nextLine();
            String query = q1 + " " + q2;

            List<String> results = new ArrayList<>();
            QueriesParser queriesParser = new QueriesParser(ast, query, results);
            queriesParser.parseQuery();

            if (queriesParser.getResults().isEmpty()) {
                System.out.println("none");
            } else {
                Iterator<String> it = queriesParser.getResults().iterator();
                while (it.hasNext()) {
                    System.out.print(it.next());
                    if (it.hasNext()) {
                        System.out.print(", ");
                    }
                }
                System.out.println();
            }
        }

    }

    static void printTree(Node<ASTNode> node) {
        System.out.println(node.getData());
        printTree(node.getChildren().get(0), 1);
    }

    static void printTree(Node<ASTNode> node, int depth) {
        String cut = "";
        for (int i=0; i<depth; i++) cut += " | ";
        if (depth > 0) cut += " └ ";

        System.out.println(cut + node.getData());

        depth++;

        for (Node<ASTNode> astNode : node.getChildren()) {
            printTree(astNode, depth);
        }
    }
}
