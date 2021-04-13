package pl.edu.pb.wi.spa;

import pl.edu.pb.wi.spa.parser.Parser;
import pl.edu.pb.wi.spa.tree.ASTNode;
import pl.edu.pb.wi.spa.tree.Node;

public class SPA {
    public static void main(String[] args) {
        final String sourceFile = "tests/iteracja1/prog1.simple";
        Parser parser = new Parser();

        parser.parse(sourceFile);

        Node<ASTNode> ast = parser.getASTTree().get(0).getChildren().get(0);
        printTree(ast);
    }

    static void printTree(Node<ASTNode> node) {
        printTree(node, 0);
    }

    static void printTree(Node<ASTNode> node, int depth) {
        String cut = "";
        for(int i=0; i<depth; i++) cut += " | ";
        if(depth>0) cut += " └ ";

        System.out.println(cut + node.getData());

        depth++;

        for (Node<ASTNode> astNode : node.getChildren()) {
            printTree(astNode, depth);
        }
    }
}
