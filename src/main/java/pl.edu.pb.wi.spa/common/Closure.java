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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public void setRhs(String rhs) {
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
