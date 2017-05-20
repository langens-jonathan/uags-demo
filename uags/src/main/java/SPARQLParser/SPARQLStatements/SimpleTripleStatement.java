package SPARQLParser.SPARQLStatements;

import SPARQLParser.SPARQL.SplitQuery;

import java.util.Iterator;

/**
 * Created by langens-jonathan on 27.04.17.
 */
public class SimpleTripleStatement extends SimpleStatement {
    public SimpleTripleArgument s;
    public SimpleTripleArgument p;
    public SimpleTripleArgument o;
    /**
     * default constructor
     * <p>
     * since a simple statement is just a string you can pass it here already
     *
     * @param statement
     */
    public SimpleTripleStatement(String statement) {
        super(statement);
    }

    public String toString()
    {
        return s.toString() + " " + p.toString() + " " +  o.toString() + " . ";
    }
}
