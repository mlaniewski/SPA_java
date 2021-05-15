package spa.source.parser;


import spa.common.AST;

public interface Parser {
    AST parse(String filename);
}
