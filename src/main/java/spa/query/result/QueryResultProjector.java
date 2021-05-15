package spa.query.result;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class QueryResultProjector {

    public void printResult(List<String> result) {
        if (result.isEmpty()) {
            System.out.println("none");
        } else {
            Collections.sort(result);
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                System.out.print(it.next());
                if (it.hasNext()) {
                    System.out.print(",");
                }
            }
            System.out.println();
        }
    }
}
