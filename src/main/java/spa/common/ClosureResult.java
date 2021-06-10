package spa.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClosureResult {
    private String resultType; // BOOL, SET, MAP
    private boolean boolResult;
    private String p = "";
    private String q = "";
    private Set<String> vals = new HashSet<>();
    private Map<String, Set<String>> pq = new HashMap<>();
    private Map<String, Set<String>> qp = new HashMap<>();


    public void addValue(String val) {
        vals.add(val);
    }

    public void addPq(String k, String v) {
        pq.computeIfAbsent(k, s -> new HashSet<>());
        pq.get(k).add(v);
    }

    public void addQp(String k, String v) {
        qp.computeIfAbsent(k, s -> new HashSet<>());
        qp.get(k).add(v);
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public boolean isBoolResult() {
        return boolResult;
    }

    public void setBoolResult(boolean boolResult) {
        this.boolResult = boolResult;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Map<String, Set<String>> getPq() {
        return pq;
    }

    public Map<String, Set<String>> getQp() {
        return qp;
    }

    public Set<String> getVals() {
        return vals;
    }
}
