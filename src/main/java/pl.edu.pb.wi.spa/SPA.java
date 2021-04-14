package pl.edu.pb.wi.spa;

import pl.edu.pb.wi.spa.ast.builder.Builder;
import pl.edu.pb.wi.spa.parser.Parser;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

public class SPA {
    public static void main(String[] args) {
        final String sourceFile = "tests/iteracja1/prog1.simple";
        Builder builder = new Builder();
        Parser parser = new Parser(builder);

        parser.parse(sourceFile);

        Node<ASTNode> ast = builder.getAstTree();
        printTree(ast);

        builder.getAST();
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
