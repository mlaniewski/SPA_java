package pl.edu.pb.wi.spa.common;

import java.util.*;

public class Selector {
    private String type;
    private List<String> variables = new ArrayList<>();
    private Map<String, Set<String>> variableProperties = new HashMap<>();

    public void addVariableProperty(String variable, String property) {
        variableProperties.computeIfAbsent(variable, k -> new HashSet<>());
        variableProperties.get(variable).add(property);
    }

    public void addVariable(String name) {
        variables.add(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
