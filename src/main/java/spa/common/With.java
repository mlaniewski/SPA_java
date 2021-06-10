package spa.common;

public class With {
    private String operand = "";
    private String leftParamVarName = "";
    private String leftParamPropertyName = "";
    private String rightParamVarName = "";
    private String rightParamPropertyName = "";
    private boolean isRightParamProperty;
    private boolean isLeftParamProperty;

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public String getLeftParamVarName() {
        return leftParamVarName;
    }

    public void setLeftParamVarName(String leftParamVarName) {
        this.leftParamVarName = leftParamVarName;
    }

    public String getLeftParamPropertyName() {
        return leftParamPropertyName;
    }

    public void setLeftParamPropertyName(String leftParamPropertyName) {
        this.leftParamPropertyName = leftParamPropertyName;
    }

    public String getRightParamVarName() {
        return rightParamVarName;
    }

    public void setRightParamVarName(String rightParamVarName) {
        this.rightParamVarName = rightParamVarName;
    }

    public String getRightParamPropertyName() {
        return rightParamPropertyName;
    }

    public void setRightParamPropertyName(String rightParamPropertyName) {
        this.rightParamPropertyName = rightParamPropertyName;
    }

    public boolean isRightParamProperty() {
        return isRightParamProperty;
    }

    public void setRightParamProperty(boolean rightParamProperty) {
        this.isRightParamProperty = rightParamProperty;
    }

    public boolean isLeftParamProperty() {
        return isLeftParamProperty;
    }

    public void setLeftParamProperty(boolean leftParamProperty) {
        this.isLeftParamProperty = leftParamProperty;
    }

    @Override
    public String toString() {
        return "With{" +
                "operand='" + operand + '\'' +
                ", leftParamVarName='" + leftParamVarName + '\'' +
                ", leftParamPropertyName='" + leftParamPropertyName + '\'' +
                ", rightParamVarName='" + rightParamVarName + '\'' +
                ", rightParamPropertyName='" + rightParamPropertyName + '\'' +
                ", isRightParamProperty=" + isRightParamProperty +
                ", isLeftParamProperty=" + isLeftParamProperty +
                '}';
    }
}
