package SPARQLParser.SPARQLStatements;

import SPARQLParser.SPARQL.SplitQuery;

import java.util.*;

/**
 * A simple statement is basicly a String
 */
public class SimpleStatement implements IStatement {
    // the string
    private String statement;

    // a hashset with the unknowns
    private Set<String> unknowns = new HashSet<String>();

    /**
     * default constructor
     *
     * since a simple statement is just a string you can pass it here already
     * @param statement
     */
    public SimpleStatement(String statement)
    {
        this.statement = statement;
        calculateUnknowns();
    }

    /**
     * extracts the unknowns in the statement
     */
    private void calculateUnknowns()
    {
        // to detect the unknowns we split the statement into blocks
        for(String token : this.statement.split(" "))
        {
            // removing ), (
            token.replace("(","").replace(")","");

            if(token.startsWith("?"))
            {
                // if the token starts with ? it is a unknown
                this.unknowns.add(token.substring(1, token.length()));
            }
        }
    }

    /**
     * @return the hashset with unknowns
     */
    public Set<String> getUnknowns()
    {
        return this.unknowns;
    }

    /**
     * @return this.statement
     */
    public String toString()
    {
        return statement.toString();
    }

    /**
     * @return a clone of this object
     */
    public IStatement clone()
    {
        return new SimpleStatement(this.statement);
    }

    /**
     * the type of this object is always a simple statement
     * @return StatementType.SIMPLE
     */
    public StatementType getType() {
        return StatementType.SIMPLE;
    }


    /**
     * this will propagate the replacement of ALL subsequent graph statements with the new
     * graph name.
     *
     * note that to remove all graph statements you can just pass an empty string as parameter
     *
     * @param newGraph the name of the new graph
     */
    public void replaceGraphStatements(String newGraph)
    {
    }

    /**
     * this will propagate the replacement of ALL subsequent graph statements which are
     * equal to the oldGraph's name. All graph statements targetting other graphs will remain
     * untouched.
     *
     * @param oldGraph the name of tha graph that needs be replaced
     * @param newGraph the new graph name
     */
    public void replaceGraphStatements(String oldGraph, String newGraph)
    {
    }

    /**
     * a simple statement cannot 'have' a graph, therefor it returns null
     * when asked
     *
     * @return null
     */
    public String getGraph()
    {
        return null;
    }

    public static List<SimpleStatement> generateSimpleStatementList(SplitQuery.SplitQueryIterator iterator)//String fullStatment)
    {
        List<SimpleStatement> statements = new ArrayList<SimpleStatement>();
        //SplitQuery sq = new SplitQuery(fullStatment);

        //Iterator<String> it = sq.iterator();
        //SplitQuery.SplitQueryIterator iterator = (SplitQuery.SplitQueryIterator) it;

        SimpleTripleArgument s = null;
        SimpleTripleArgument p = null;
        SimpleTripleArgument o = null;

        while(iterator.hasNext() && !iterator.peekNext().startsWith("}") && !iterator.peekNext().startsWith("{"))
        {
            String stat = iterator.next();
            if(stat.startsWith("."))
            {
                iterator.breakOff(".");
                continue;
            }
            String s_clipped = stat.trim().replace("(","");
            if(s_clipped.toLowerCase().startsWith("filter") ||
                    s_clipped.toLowerCase().startsWith("bind") ||
                    s_clipped.toLowerCase().startsWith("values"))
            {
                statements.add(SimpleStatement.getFunctionStatement(stat, iterator));
                continue;
            }
            do {
                o = null;
                if(stat.startsWith(","))
                {
                    o = null;
                    iterator.next();
                    iterator.breakOff(",");
                }
                if(stat.startsWith(";"))
                {
                    o = null;
                    p = null;
                    iterator.next();
                    iterator.breakOff(";");
                }
                if(s == null)s = getTripleArgument(stat, iterator);
                if(p == null)p = getTripleArgument(iterator.next(), iterator);
                o = getTripleArgument(iterator.next(), iterator);
                SimpleTripleStatement sts = new SimpleTripleStatement("");
                sts.s = s;
                sts.p = p;
                sts.o = o;
                statements.add(sts);
                stat = iterator.peekNext();
            } while(iterator.peekNext().startsWith(";") || iterator.peekNext().startsWith(","));

            s = null;
            p = null;
            o = null;
        }

        return statements;
    }

    public static SimpleFunctionStatement getFunctionStatement(String next, SplitQuery.SplitQueryIterator iterator)
    {
        String functionStatement = next;
//        while(iterator.hasNext() && !iterator.peekNextIncludingNewLines().startsWith("\n"))
//        {
//            functionStatment += iterator.next();
//        }
        boolean existsFilter = false;
        boolean existsFilterClosed = false;
        while(!iterator.peekNextIncludingNewLines().startsWith("\n") &&
                !iterator.peekNext().trim().toLowerCase().startsWith("filter") &&
                !iterator.peekNext().trim().toLowerCase().startsWith("bind") &&
                !iterator.peekNext().trim().toLowerCase().startsWith("values") &&
                !(iterator.peekNext().trim().startsWith("}") && !existsFilter) &&
                !(iterator.peekNext().trim().startsWith("{") && !existsFilter) &&
                iterator.hasNext())
        {
            String filterPart = iterator.next();
            if(filterPart.trim().toLowerCase().equals("exists") || filterPart.toLowerCase().equals("values"))
            {
                existsFilter = true;
            }

            if(filterPart.trim().startsWith("}"))
            {
                existsFilterClosed = true;
                existsFilter = false;
            }

            functionStatement += " " + filterPart;
        }
        return new SimpleFunctionStatement(functionStatement);
    }

    public static SimpleTripleArgument getTripleArgument(String next, SplitQuery.SplitQueryIterator iterator)
    {
        SimpleTripleArgument sta = new SimpleTripleArgument();
        if(next.endsWith("."))
        {
            iterator.breakOff(".");
        }

        if(next.startsWith("?"))
        {
            sta.type = SimpleTripleArgument.Type.Variable;
            sta.value = next;
            return sta;
        }

        if((next.startsWith("<") && next.endsWith(">")) || !next.startsWith("\""))
        {
            sta.type = SimpleTripleArgument.Type.URI;
            sta.value = next;
            return sta;
        }

        // accumulate until or ;
        String s = next;
        while(iterator.hasNext() && !SimpleStatement.isCorrectLiteralStatement(s)) //!(iterator.peekNext().startsWith("\"")))
        {
            s += iterator.next();
        }
        while(iterator.hasNext() && !(iterator.peekNext().startsWith(".") || iterator.peekNext().startsWith(";")))
        {
            s += iterator.next();
        }
        sta.type = SimpleTripleArgument.Type.Literal;
        sta.setValue(s);
        return sta;
    }

    public static boolean isCorrectLiteralStatement(String literalStatement)
    {
        if(!literalStatement.startsWith("\""))
        {
            return false;
        }

        literalStatement = literalStatement.substring(1, literalStatement.length());

        if(!(literalStatement.indexOf("\"") > -1)) {
            return false;
        }

        literalStatement = literalStatement.substring(literalStatement.indexOf("\"") + 1);

        if(literalStatement.isEmpty()) {
            return true;
        }

        if(literalStatement.startsWith("@") || literalStatement.startsWith("^^")) {
            return true;
        }

        return false;
    }
}
