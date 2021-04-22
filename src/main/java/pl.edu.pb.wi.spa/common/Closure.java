package pl.edu.pb.wi.spa.common;

public class Closure {
    private String type;
    private String lhs;
    private String rhs;

    public Closure(String type, String lhs, String rhs) {
        this.type = type;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public String toString() {
        return "Closure{" +
                "type='" + type + '\'' +
                ", lhs='" + lhs + '\'' +
                ", rhs='" + rhs + '\'' +
                '}';
    }
}
