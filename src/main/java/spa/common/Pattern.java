package spa.common;

public class Pattern {
    private String varName;
    private String leftParam;
    private String rightParam;

    public Pattern(String varName, String leftParam, String rightParam) {
        this.varName = varName;
        this.leftParam = leftParam;
        this.rightParam = rightParam;
    }

    public String getVarName() {
        return varName;
    }

    public String getLeftParam() {
        return leftParam;
    }

    public String getRightParam() {
        return rightParam;
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "varName='" + varName + '\'' +
                ", leftParam='" + leftParam + '\'' +
                ", rightParam='" + rightParam + '\'' +
                '}';
    }
}
