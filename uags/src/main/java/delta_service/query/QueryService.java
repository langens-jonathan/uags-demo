package delta_service.query;

import SPARQLParser.SPARQL.InvalidSPARQLException;
import SPARQLParser.SPARQL.SPARQLQuery;
import SPARQLParser.SPARQLStatements.*;
import delta_service.callback.CallBack;
import delta_service.callback.CallBackService;
import delta_service.callback.CallBackSetNotFoundException;
import delta_service.config.Configuration;
import delta_service.graphs.Graph;
import delta_service.graphs.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by langens-jonathan on 31.05.16.
 *
 * The Query service has only 1 method: the getDifferenceTriples method that will return a map
 * with deltas for all graphs on which the passed query has an effect.
 */
@Service
public class QueryService
{
    public SPARQLService sparqlService;
    private CallBackService callBackService;

    private Queue<QueryInfo> updateQueries = new ArrayDeque<QueryInfo>();
    private List<QueryInfo> processedQueries = new ArrayList<QueryInfo>();
    private QueryInfo currentQuery = null;

    public List<QueryInfo> getProcessedQueries(){return this.processedQueries;}
    public QueryInfo getCurrentQuery(){return this.currentQuery;}

    private boolean isProcessingUpdateQueries = false;

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    public QueryService() {
        this.sparqlService = new SPARQLService();

        this.callBackService = new CallBackService();
        this.callBackService.addCallBackSet("allDifferences");
        this.callBackService.addCallBackSet("potentialDifferences");
        this.callBackService.addCallBackSet("effectiveDifferences");
    }

    public Response postSPARQLResponse(String location, String query, Map<String, String> headers) throws IOException
    {
        return this.sparqlService.postSPARQLResponse(location, query, headers);
    }

    public void addCallBack(String setName, String callBackLocation)
    {
        CallBack callback = new CallBack();
        callback.setUrl(callBackLocation);
        try {
            this.callBackService.addCallBack(setName, callback);
        } catch (CallBackSetNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void notifyCallBacks(String setname, String message)
    {
        try {
            this.callBackService.notifyCallBacks(setname, message);
        } catch (CallBackSetNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void registerUpdateQuery(QueryInfo sparqlQuery)
    {
        this.updateQueries.add(sparqlQuery);
        this.startProcessingUpdateQueries();
    }

    public void processNextQuery()
    {
        if(this.updateQueries.size() > 0) {
            this.isProcessingUpdateQueries = true;
            this.currentQuery = this.updateQueries.remove();
            try {
                this.processUpdateQuery(this.currentQuery);
            }
            catch (Exception e)
            {
                this.currentQuery.headers.put("ERROR", e.getLocalizedMessage());
                e.printStackTrace();
            }
            finally {
                this.processedQueries.add(this.currentQuery);
                this.currentQuery = null;
                if (this.updateQueries.size() > 0) {
                    this.processNextQuery();
                } else {
                    this.isProcessingUpdateQueries = false;
                }
            }
        }
    }

    public void startProcessingUpdateQueries()
    {
        if(this.isProcessingUpdateQueries == false)
        {
            this.processNextQuery();
        }
    }

    public void processUpdateQuery(QueryInfo queryInfo) throws InvalidSPARQLException, IOException {
            // 1. calculate the difference triples (for this we want the state of the DB as before the update)
            SPARQLQuery parsedQuery = queryInfo.query;
            List<String> accessibleGraphs = new ArrayList<String>();
            
            for(Graph g : GraphService.getGraphsForUser(queryInfo.userId))
            	accessibleGraphs.add(g.getGraph());
            
            String reformedQuery = this.rewriteUpdateQuery(parsedQuery, queryInfo.graphMap, queryInfo.functionalStatements, queryInfo.user);

            // 2. prepare the JSON responses
//            String potJson = "{\"query\":\"" + URLEncoder.encode(queryInfo.originalQuery, "UTF-8") + "\", \"delta\":[";
//            String effectiveJson = "{\"query\":\"" + URLEncoder.encode(queryInfo.originalQuery, "UTF-8") + "\", \"delta\":[";
//
//            for (String g : diff.keySet()) {
//                potJson += "{\"type\":\"potential\",\"graph\":\"" + g + "\"," + diff.get(g).getPotentialChangesAsJSON() + "},";
//                effectiveJson += "{\"type\":\"effective\",\"graph\":\"" + g + "\"," + diff.get(g).getEffectiveChangesAsJSON() + "},";
//            }
//
//            if (!diff.keySet().isEmpty()) {
//                potJson = potJson.substring(0, potJson.length() - 1);
//                effectiveJson = effectiveJson.substring(0, effectiveJson.length() - 1);
//            }
//
//            potJson += "]}";
//            effectiveJson += "]}";
//
//        if(Configuration.logDeltaResults)
//        {
//            log.info("Delta effectives:\n" + effectiveJson);
//            log.info("Delta potential:\n" + potJson);
//        }

            // 3. perform the actual query on the DB
            //queryInfo.response = this.postSPARQLResponse(queryInfo.endpoint, queryInfo.originalQuery, queryInfo.headers);
        
        	// instead of doig 3 we will now do 3' where we figure out what to put where XD
//        	InMemorySPARQLService inMemSPARQLService = new InMemorySPARQLService();
//        	List<String> updateQueries = inMemSPARQLService.getUpdateQueries(diff, GraphService.getGraphsForUser(queryInfo.userId));
        	
        	queryInfo.response = new Response();
        	queryInfo.response.responseText = reformedQuery;
//
//        	for(String uq : updateQueries)
//        	{
//        		Response tr = this.postSPARQLResponse(queryInfo.endpoint, uq, queryInfo.headers);
//        		queryInfo.response.responseText += tr.responseText;
//        		queryInfo.headers = tr.responseHeaders;
//        	}

//            // 4. notify the callback endpoints
//            this.notifyCallBacks("potentialDifferences", potJson);
//            this.notifyCallBacks("effectiveDifferences", effectiveJson);

            this.isProcessingUpdateQueries = false;
    }

    public QueryService(SPARQLService service){this.sparqlService = service;}

    /**
     * Returns a map that projects graph names on DifferenceTriples-objects. Those DifferenceTriples-objects
     * contain 4 sets, 2 of potential insert and delete triples and 2 with effective insert and delete
     * triples.
     *
     * note for debugging: This map is maintained throughout the entire function, whenever a set of triples is
     * requested for a certain graph it will be returned from this map, if that graph is not yet present as a
     * key in the map a new differenceTriples object will be added to the map for that graph.
     *
     * @param parsedQuery a SPARQLQuery object that has been initialized with a query
     * @return a map of graph names and difference triple objects (Map<String, DifferenceTriples>)
     * @throws InvalidSPARQLException if the parsedQuery object does not contain a valid SPARQL query we throw
     *         an exception.
     */
    public Map<String, DifferenceTriples> getDifferenceTriples(SPARQLQuery parsedQuery, List<String> accessibleGraphs) throws InvalidSPARQLException, IOException {
        // the deltas for each query will be stored in a map the projects each graph on a differenceTriples object
        Map<String, DifferenceTriples> differenceTriplesMap = new HashMap<String, DifferenceTriples>();

        /*
         * we build a queryPrefix quickly because this will be necessary for almost every subsequent query
         * that we will construct.
         */
        String queryPrefix = "";
        for(String key : parsedQuery.getPrefixes().keySet())
        {
            queryPrefix += "PREFIX " + key + ": <" + parsedQuery.getPrefixes().get(key) + ">\n";
        }

        /*
         * we clone the query as to ensure that the original query object remains untouched
         */
        SPARQLQuery clonedQuery = parsedQuery.clone();

        /*
         * here we will split DELETE_INSERT blocks in to 2 separate blocks of DELETE and INSERT parts
         */
        UpdateBlockStatement statementToReplace = null;
        UpdateBlockStatement deleteStatement = null;
        UpdateBlockStatement insertStatement = null;

        do {
            statementToReplace = null;
            for(IStatement s : clonedQuery.getStatements()) {
                if (s.getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                    // cast it, the updateblockstatement has functional support that will be handy later on
                    statementToReplace = (UpdateBlockStatement) s;
                    if (statementToReplace.getUpdateType().equals(BlockStatement.BLOCKTYPE.DELETE_INSERT)) {
                        // lets split it and put 2 at that location
                        if (statementToReplace.getStatements().get(0).getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                            deleteStatement = (UpdateBlockStatement) statementToReplace.getStatements().get(0);
                            deleteStatement.setWhereBlock(statementToReplace.getWhereBlock().clone());
                        }

                        if (statementToReplace.getStatements().get(1).getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                            insertStatement = (UpdateBlockStatement) statementToReplace.getStatements().get(1);
                            insertStatement.setWhereBlock(statementToReplace.getWhereBlock().clone());
                        }
                        break;
                    }
                    else
                    {
                        statementToReplace = null;
                    }
                }
            }
            if(statementToReplace != null)
            {
                int i = clonedQuery.getStatements().indexOf(statementToReplace);
                clonedQuery.getStatements().remove(statementToReplace);
                clonedQuery.getStatements().add(i, deleteStatement);
                clonedQuery.getStatements().add(i + 1, insertStatement);
            }
        } while (statementToReplace != null);

        /*
         * we loop over the blocks in the query, for every block the idea is:
         *  1. find out on which graph it operates (if none than it's the graph for the entire query
         *  2. remove all graph statements for the block
         *  3. transform it into a construct
         *  4. whatever comes out will be the potential difference triples
         */
        for(IStatement statement : clonedQuery.getStatements()) {
            // check if the block is an update (otherwise it won't generate a delta)
            if (statement.getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                // cast it, the updateblockstatement has functional support that will be handy later on
                UpdateBlockStatement updateBlockStatement = (UpdateBlockStatement)statement;

                /*
                 * step 1. finding the graph
                 */
                String originalGraph = updateBlockStatement.getGraph();

                if(originalGraph.isEmpty())
                    originalGraph = parsedQuery.getGraph();

                /*
                 * step 2. replacing all graph statements in the block
                 */
                updateBlockStatement.replaceGraphStatements("");

                /*
                 * step 3. transforming it into a construct
                 */
                String extractQuery = queryPrefix + "WITH <" + originalGraph + ">\n";
                extractQuery += "CONSTRUCT\n{\n";

                for(IStatement innerStatement : updateBlockStatement.getStatements())
                {
                    extractQuery += innerStatement.toString() + "\n";
                }

                extractQuery += "}\nWHERE\n{\nGRAPH ?original_graph_for_scope_service {";

                if(updateBlockStatement.getWhereBlock() != null) {
                    for (IStatement whereStatement : updateBlockStatement.getWhereBlock().getStatements()) {
                        extractQuery += whereStatement.toString() + "\n";
                    }
                }

                extractQuery += "}\nFILTER (?original_graph_for_scope_service in (";
                
                for(String aGraph : accessibleGraphs)
                	extractQuery += "<" + aGraph + ">,";
                
                if(accessibleGraphs.size() > 0)
                	extractQuery = extractQuery.substring(0, extractQuery.length() - 1);
                
                extractQuery += "))\n}";

                if(Configuration.logImportantQueries)
                {
                    if(updateBlockStatement.getUpdateType().equals(BlockStatement.BLOCKTYPE.INSERT))
                    {
                        log.info("Query to extract insert block:\n" + extractQuery);
                    }
                    else
                    {
                        log.info("Query to extract delete block:\n" + extractQuery);
                    }
                }

                /*
                 * step 4. transforming the construct query in a set of triples, we will automatically
                 *         add the triples to the differenceTriples object for that graph in the map
                 */
                Set<Triple> insertTriples = null;
                Set<Triple> deleteTriples = null;

                if(!differenceTriplesMap.containsKey(originalGraph))
                    differenceTriplesMap.put(originalGraph, new DifferenceTriples());

                insertTriples = differenceTriplesMap.get(originalGraph).getAllInsertTriples();
                deleteTriples = differenceTriplesMap.get(originalGraph).getAllDeleteTriples();

                /*
                 * TODO: make sure that the authentication headers are set and passed if Configuration has them
                 * for query user and pwd.
                 */
                List<Triple> triples = this.sparqlService.getTriplesViaConstruct(Configuration.queryEndpoint + "?query=" + URLEncoder.encode(extractQuery, "UTF-8"));

                if(updateBlockStatement.getUpdateType().equals(BlockStatement.BLOCKTYPE.INSERT))
                {
                    insertTriples.addAll(triples);
                }
                else
                {
                    deleteTriples.addAll(triples);
                }
            }
        }
        
        return differenceTriplesMap;
    }

    public String rewriteUpdateQuery(SPARQLQuery parsedQuery, Map<String, List<SimpleStatement>> graphMap, List<SimpleFunctionStatement> functionalStatements, String user) throws InvalidSPARQLException, IOException {
        // just to fool intellij.... ijkl = 1
        int ijkl = 1;
        // the deltas for each query will be stored in a map the projects each graph on a differenceTriples object
        Map<String, DifferenceTriples> differenceTriplesMap = new HashMap<String, DifferenceTriples>();

        boolean isInsertQuery = true;

            /*
             * we build a queryPrefix quickly because this will be necessary for almost every subsequent query
             * that we will construct.
             */
        String queryPrefix = "";
        for(String key : parsedQuery.getPrefixes().keySet())
        {
            queryPrefix += "PREFIX " + key + ": <" + parsedQuery.getPrefixes().get(key) + ">\n";
        }

        Map<String, List<String>> localSubjectTypes = new HashMap<String, List<String>>();
        if(parsedQuery.getStatements().size() > 0)
        {
            if(parsedQuery.getStatements().get(0).getClass().equals(UpdateBlockStatement.class))
            {
                UpdateBlockStatement updateBlockStatement = (UpdateBlockStatement) parsedQuery.getStatements().get(0);
                for(IStatement iStatement : updateBlockStatement.getStatements())
                {
                    if(iStatement.getClass().equals(SimpleTripleStatement.class))
                    {
                        SimpleTripleStatement sts = (SimpleTripleStatement) iStatement;
                        if(sts.p.value.equals("a") ||
                                sts.p.value.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
                        {
                            String subject = sts.s.value.substring(1, sts.s.value.length() - 1);
                            if(!localSubjectTypes.containsKey(subject))
                            {
                                localSubjectTypes.put(subject, new ArrayList<String>());
                            }
                            localSubjectTypes.get(subject).add(sts.o.value);
                        }
                    }
                }
            }
        }

        // quickly check for an update query without a where block
        if(parsedQuery.getStatements().size() > 0)
        {
            if(parsedQuery.getStatements().get(0).getClass().equals(UpdateBlockStatement.class))
            {
                UpdateBlockStatement updateBlockStatement = (UpdateBlockStatement) parsedQuery.getStatements().get(0);
                if(updateBlockStatement.getWhereBlock() == null)
                {
                    /********
                     *
                     */
                    String replacementQuery = queryPrefix;
                    HashMap<String, List<String>> graphStatementMap = new HashMap<String, List<String>>();

                    if(isInsertQuery) {
                        replacementQuery += "\nINSERT DATA\n{\n";
                    } else {
                        replacementQuery += "\nDELETE DATA\n{\n";
                    }

                    for(IStatement iStatement : updateBlockStatement.getStatements())
                    {
                        if(iStatement.getClass().equals(SimpleTripleStatement.class)) {
                            SimpleTripleStatement statement = (SimpleTripleStatement) iStatement;
                            List<String> graphsForStatement = GraphService.getGraphsForStatement(statement.s.value.substring(1, statement.s.value.length() - 1),
                                    statement.p.value, statement.o.value,
                                    statement.o.type == SimpleTripleArgument.Type.URI, statement.o.language,
                                    statement.o.datatype, user, localSubjectTypes);
                            for (String graph : graphsForStatement) {
                                if (!graphStatementMap.containsKey(graph)) {
                                    graphStatementMap.put(graph, new ArrayList<String>());
                                }
                                graphStatementMap.get(graph).add(statement.toString());
                            }
                        }
                    }

                    for(String graph : graphStatementMap.keySet())
                    {
                        replacementQuery += "GRAPH <" + graph + ">\n{\n";
                        for(String statement : graphStatementMap.get(graph))
                        {
                            replacementQuery += statement + "\n";
                        }
                        replacementQuery += "}\n";
                    }

                    replacementQuery += "}";

                    return replacementQuery;

                    /*********
                     *
                     */
                }
            }
        }
            /*
             * we clone the query as to ensure that the original query object remains untouched
             */
        SPARQLQuery clonedQuery = parsedQuery.clone();

            /*
             * here we will split DELETE_INSERT blocks in to 2 separate blocks of DELETE and INSERT parts
             */
        UpdateBlockStatement statementToReplace = null;
        UpdateBlockStatement deleteStatement = null;
        UpdateBlockStatement insertStatement = null;

        do {

            statementToReplace = null;
            for(IStatement s : clonedQuery.getStatements()) {
                if (s.getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                    // cast it, the updateblockstatement has functional support that will be handy later on
                    statementToReplace = (UpdateBlockStatement) s;
                    if (statementToReplace.getUpdateType().equals(BlockStatement.BLOCKTYPE.DELETE_INSERT)) {
                        // lets split it and put 2 at that location
                        if (statementToReplace.getStatements().get(0).getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                            deleteStatement = (UpdateBlockStatement) statementToReplace.getStatements().get(0);
                            deleteStatement.setWhereBlock(statementToReplace.getWhereBlock().clone());
                        }

                        if (statementToReplace.getStatements().get(1).getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
                            insertStatement = (UpdateBlockStatement) statementToReplace.getStatements().get(1);
                            insertStatement.setWhereBlock(statementToReplace.getWhereBlock().clone());
                        }
                        break;
                    }
                    else
                    {
                        statementToReplace = null;
                    }
                }
            }
            if(statementToReplace != null)
            {
                int i = clonedQuery.getStatements().indexOf(statementToReplace);
                clonedQuery.getStatements().remove(statementToReplace);
                clonedQuery.getStatements().add(i, deleteStatement);
                clonedQuery.getStatements().add(i + 1, insertStatement);
            }
        } while (statementToReplace != null);

        String constructQuery = queryPrefix + "\nCONSTRUCT\n{\n";

        UpdateBlockStatement updateBlockStatement = null;

        if(!parsedQuery.getStatements().isEmpty())
        {
            if(!parsedQuery.getStatements().get(0).getClass().equals(UpdateBlockStatement.class))
            {
                return "CANNOT PROCESS QUERY";
            }
            updateBlockStatement = (UpdateBlockStatement) parsedQuery.getStatements().get(0);

            isInsertQuery = updateBlockStatement.getUpdateType().equals(BlockStatement.BLOCKTYPE.INSERT);

            for(IStatement statement : updateBlockStatement.getStatements())
            {
                constructQuery += statement.toString() + "\n";
            }
        }


        constructQuery += "}\n";

        if(!updateBlockStatement.getWhereBlock().getStatements().isEmpty()) {

            constructQuery += "WHERE\n{\n";

            Map<String, List<String>> reverseGraphMap = new HashMap<String, List<String>>();

            for (String g : graphMap.keySet()) {
                for (IStatement iStatement : graphMap.get(g)) {
                    String statementAsString = iStatement.toString();
                    if (!reverseGraphMap.containsKey(statementAsString)) {
                        reverseGraphMap.put(statementAsString, new ArrayList<String>());
                    }
                    reverseGraphMap.get(statementAsString).add(g);
                }
            }

            int statementCounter = 0;

            for (String statement : reverseGraphMap.keySet()) {
                if (reverseGraphMap.get(statement).isEmpty())
                    return "You cannot execute this query";

                constructQuery += "GRAPH ?g_" + statementCounter + "\n{" + statement + "\n}\n";
                constructQuery += "FILTER(?g_" + statementCounter++ + " IN (";
                for (String graph : reverseGraphMap.get(statement)) {
                    constructQuery += "<" + graph + ">,";
                }
                constructQuery = constructQuery.substring(0, constructQuery.length() - 1);
                constructQuery += "))\n";
            }

            for (SimpleFunctionStatement sfs : functionalStatements) {
                constructQuery += sfs.toString() + "\n";
            }

            constructQuery += "}";
        }

        List<Triple> triples = this.sparqlService.getTriplesViaConstruct(Configuration.queryEndpoint + "?query=" + URLEncoder.encode(constructQuery, "UTF-8"));

        HashMap<String, List<String>> graphStatementMap = new HashMap<String, List<String>>();

        String replacementQuery = queryPrefix;

        if(isInsertQuery) {
            replacementQuery += "\nINSERT DATA\n{\n";
        } else {
            replacementQuery += "\nDELETE DATA\n{\n";
        }

        for(Triple t : triples)
        {
            List<String> graphsForStatement = GraphService.getGraphsForStatement(t.getSubject(), t.getPredicate(), t.getObjectAsString(),
                    t.isObjectIsURI(), t.getObjectLanguage(), t.getObjectType(),
                    user, localSubjectTypes);
            for(String graph : graphsForStatement) {
                if(!graphStatementMap.containsKey(graph))
                {
                    graphStatementMap.put(graph, new ArrayList<String>());
                }
                graphStatementMap.get(graph).add(t.toString());
            }
        }

        for(String graph : graphStatementMap.keySet())
        {
            replacementQuery += "GRAPH <" + graph + ">\n{\n";
            for(String statement : graphStatementMap.get(graph))
            {
                replacementQuery += statement + "\n";
            }
            replacementQuery += "}\n";
        }

        replacementQuery += "}";

        return replacementQuery;
            /*
             * we loop over the blocks in the query, for every block the idea is:
             *  1. find out on which graph it operates (if none than it's the graph for the entire query
             *  2. remove all graph statements for the block
             *  3. transform it into a construct
             *  4. whatever comes out will be the potential difference triples
             */
//        for(IStatement statement : clonedQuery.getStatements()) {
//            // check if the block is an update (otherwise it won't generate a delta)
//            if (statement.getType().equals(IStatement.StatementType.UPDATEBLOCK)) {
//                // cast it, the updateblockstatement has functional support that will be handy later on
//                UpdateBlockStatement updateBlockStatement = (UpdateBlockStatement)statement;
//
//
//                    /*
//                     * step 2. replacing all graph statements in the block
//                     */
//                updateBlockStatement.replaceGraphStatements("");
//
//                    /*
//                     * step 3. transforming it into a construct
//                     */
//                String extractQuery = queryPrefix + "\n";
//                extractQuery += "CONSTRUCT\n{\n";
//
//                for(IStatement innerStatement : updateBlockStatement.getStatements())
//                {
//                    extractQuery += innerStatement.toString() + "\n";
//                }
//
//                extractQuery += "}\nWHERE\n{\nGRAPH ?original_graph_for_scope_service {";
//
//                if(updateBlockStatement.getWhereBlock() != null) {
//                    for (IStatement whereStatement : updateBlockStatement.getWhereBlock().getStatements()) {
//                        extractQuery += whereStatement.toString() + "\n";
//                    }
//                }
//
//                extractQuery += "}\nFILTER (?original_graph_for_scope_service in (";
//
//                for(String aGraph : accessibleGraphs)
//                    extractQuery += "<" + aGraph + ">,";
//
//                if(accessibleGraphs.size() > 0)
//                    extractQuery = extractQuery.substring(0, extractQuery.length() - 1);
//
//                extractQuery += "))\n}";
//
//                if(Configuration.logImportantQueries)
//                {
//                    if(updateBlockStatement.getUpdateType().equals(BlockStatement.BLOCKTYPE.INSERT))
//                    {
//                        log.info("Query to extract insert block:\n" + extractQuery);
//                    }
//                    else
//                    {
//                        log.info("Query to extract delete block:\n" + extractQuery);
//                    }
//                }
//
//                    /*
//                     * step 4. transforming the construct query in a set of triples, we will automatically
//                     *         add the triples to the differenceTriples object for that graph in the map
//                     */
//                Set<Triple> insertTriples = null;
//                Set<Triple> deleteTriples = null;
//
//                if(!differenceTriplesMap.containsKey(originalGraph))
//                    differenceTriplesMap.put(originalGraph, new DifferenceTriples());
//
//                insertTriples = differenceTriplesMap.get(originalGraph).getAllInsertTriples();
//                deleteTriples = differenceTriplesMap.get(originalGraph).getAllDeleteTriples();
//
//                    /*
//                     * TODO: make sure that the authentication headers are set and passed if Configuration has them
//                     * for query user and pwd.
//                     */
//                List<Triple> triples = this.sparqlService.getTriplesViaConstruct(Configuration.getProperty("queryURL") + "?query=" + URLEncoder.encode(extractQuery, "UTF-8"));
//
//                if(updateBlockStatement.getUpdateType().equals(BlockStatement.BLOCKTYPE.INSERT))
//                {
//                    insertTriples.addAll(triples);
//                }
//                else
//                {
//                    deleteTriples.addAll(triples);
//                }
//            }
//        }
//
//    return differenceTriplesMap;
}
}





