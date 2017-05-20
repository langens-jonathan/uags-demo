package delta_service.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by langens-jonathan on 27.04.17.
 */
public class QueryStatement {
    public QueryStatementArgument s;
    public QueryStatementArgument p;
    public QueryStatementArgument o;

    public String queryStatment = null;

    public List<String> getUnknowns()
    {
        List<String> unkowns = new ArrayList<String>();
        if(s.value.startsWith("?"))unkowns.add(s.value);
        if(p.value.startsWith("?"))unkowns.add(p.value);
        if(o.value.startsWith("?"))unkowns.add(o.value);
        return unkowns;
    }
}
