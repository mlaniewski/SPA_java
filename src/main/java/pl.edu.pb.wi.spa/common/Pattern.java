package pl.edu.pb.wi.spa.common;

public class Pattern {
    private String varName;
    private String lhs;
    private String rhs;

    public Pattern(String varName, String lhs, String rhs) {
        this.varName = varName;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public String getVarName() {
        return varName;
    }

    public String getLhs() {
        return lhs;
    }

    public String getRhs() {
        return rhs;
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "varName='" + varName + '\'' +
                ", lhs='" + lhs + '\'' +
                ", rhs='" + rhs + '\'' +
                '}';
    }
}
