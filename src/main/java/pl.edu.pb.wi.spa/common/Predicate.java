package pl.edu.pb.wi.spa.common;

public class Predicate {
    private String type = "";  // assign, string, constant, variable, prog_line, procedure, stmt#, any
    private String value = "";

    public Predicate() { }

    public Predicate(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Predicate{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}


