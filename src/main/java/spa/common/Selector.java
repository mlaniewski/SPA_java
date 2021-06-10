package spa.common;

import java.util.*;

public class Selector {
    private String type;
    private List<String> variables = new ArrayList<>();

    public void addVariable(String name) {
        variables.add(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getVariables() {
        return variables;
    }
}
