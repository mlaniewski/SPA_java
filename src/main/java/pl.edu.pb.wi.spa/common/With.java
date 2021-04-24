package pl.edu.pb.wi.spa.common;

public class With {
    private String operand = "";
    private String lhsVarName = "";
    private String lhsPropertyName = "";
    private String rhsVarName = "";
    private String rhsPropertyName = "";
    private boolean rhsIsProperty;
    private boolean lhsIsProperty;

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public String getLhsVarName() {
        return lhsVarName;
    }

    public void setLhsVarName(String lhsVarName) {
        this.lhsVarName = lhsVarName;
    }

    public String getLhsPropertyName() {
        return lhsPropertyName;
    }

    public void setLhsPropertyName(String lhsPropertyName) {
        this.lhsPropertyName = lhsPropertyName;
    }

    public String getRhsVarName() {
        return rhsVarName;
    }

    public void setRhsVarName(String rhsVarName) {
        this.rhsVarName = rhsVarName;
    }

    public String getRhsPropertyName() {
        return rhsPropertyName;
    }

    public void setRhsPropertyName(String rhsPropertyName) {
        this.rhsPropertyName = rhsPropertyName;
    }

    public boolean isRhsIsProperty() {
        return rhsIsProperty;
    }

    public void setRhsIsProperty(boolean rhsIsProperty) {
        this.rhsIsProperty = rhsIsProperty;
    }

    public boolean isLhsIsProperty() {
        return lhsIsProperty;
    }

    public void setLhsIsProperty(boolean lhsIsProperty) {
        this.lhsIsProperty = lhsIsProperty;
    }

    @Override
    public String toString() {
        return "With{" +
                "operand='" + operand + '\'' +
                ", lhsVarName='" + lhsVarName + '\'' +
                ", lhsPropertyName='" + lhsPropertyName + '\'' +
                ", rhsVarName='" + rhsVarName + '\'' +
                ", rhsPropertyName='" + rhsPropertyName + '\'' +
                ", rhsIsProperty=" + rhsIsProperty +
                ", lhsIsProperty=" + lhsIsProperty +
                '}';
    }
}
