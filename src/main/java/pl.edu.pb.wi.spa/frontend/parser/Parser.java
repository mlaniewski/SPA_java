package pl.edu.pb.wi.spa.frontend.parser;

import pl.edu.pb.wi.spa.common.AST;

public interface Parser {
    AST parse(String filename);
}
