package spa.common;

public class Relation {
    private String type;
    private String leftParam;
    private String rightParam;

    public Relation(String type, String leftParam, String rightParam) {
        this.type = type;
        this.leftParam = leftParam;
        this.rightParam = rightParam;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLeftParam() {
        return leftParam;
    }

    public void setLeftParam(String leftParam) {
        this.leftParam = leftParam;
    }

    public String getRightParam() {
        return rightParam;
    }

    public void setRightParam(String rightParam) {
        this.rightParam = rightParam;
    }

    @Override
    public String toString() {
        return "Relation{" +
                "type='" + type + '\'' +
                ", leftParam='" + leftParam + '\'' +
                ", rightParam='" + rightParam + '\'' +
                '}';
    }
}
