package SPARQLParser.SPARQLStatements;

import java.util.Enumeration;

/**
 * Created by langens-jonathan on 27.04.17.
 */


public class SimpleTripleArgument {
    public enum Type {URI, Literal, Variable}
    public Type type;
    public String value;
    public String language = null;
    public String datatype = null;

    public String toString()
    {
        if(type.equals(Type.Literal))
            return this.asLiteralString();

        if(type.equals(Type.URI))
            return this.asURI();

        return this.asVariable();
    }

    private String asLiteralString()
    {
        if(language != null && !language.isEmpty())
            return this.asLanguageLiteratString();
        else if(datatype != null && !datatype.isEmpty())
            return this.asDatatypeLiteralString();
        else
            return "\"" + value + "\"";
    }

    public void setValue(String value)
    {
        if(value.startsWith("\"")) {
            value = value.substring(1, value.length());

            int indexOfNextQuote = value.indexOf("\"");
            if (indexOfNextQuote > -1) {
                this.value = value.substring(0, indexOfNextQuote);
            }

            value = value.substring(indexOfNextQuote + 1, value.length());

            if (value.startsWith("@")) {
                this.language = value.substring(1, value.length());
            }

            if (value.startsWith("^^")) {
                this.datatype = value.substring(2, value.length());
            }
        } else
        {
            this.value = value;
        }
    }

    private String asLanguageLiteratString()
    {
        return "\"" + value + "\"@" + language;
    }

    private String asDatatypeLiteralString()
    {
        return "\"" + value + "\"^^" + datatype;
    }

    private String asURI()
    {
        return value;
    }

    private String asVariable()
    {
        return value;
    }
}
