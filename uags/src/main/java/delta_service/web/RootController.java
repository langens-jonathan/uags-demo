package delta_service.web;

import SPARQLParser.SPARQL.InvalidSPARQLException;
import SPARQLParser.SPARQL.SPARQLQuery;
import SPARQLParser.SPARQLStatements.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import delta_service.Util.Pair;
import delta_service.callback.CallBackConfiguration;
import delta_service.config.Configuration;
import delta_service.graphs.Graph;
import delta_service.graphs.GraphService;
import delta_service.query.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

@RestController
public class RootController {

  @Inject
  private QueryService queryService;

    @Inject
    private GraphService graphService;

    private static final Logger log = LoggerFactory.getLogger(RootController.class);
    

  /**
   * initializes the callback service with 2 call back sets (allDifferences and effectiveDifferences)
   */
  @PostConstruct
  public void init()
  {
      this.graphService.loadGraphsFromYAML();

//      try {
//          ObjectMapper mapper = new ObjectMapper();
//          String filename = System.getenv("SUBSCRIBERSFILE");
//          CallBackConfiguration callBackConfiguration = mapper.readValue(new File(filename), CallBackConfiguration.class);
//          for(String callbackString : callBackConfiguration.getPotentials())
//              this.queryService.addCallBack("potentialDifferences", callbackString);
//          for(String callbackString : callBackConfiguration.getEffectives())
//              this.queryService.addCallBack("effectiveDifferences", callbackString);
//      } catch (IOException e) {
//          e.printStackTrace();
//      }
  }

  /**
   * Auto wired web entry point
   *
   * expects a body in the form
   * {
   *     "callback":"<CALLBACKLOCATION>"
   * }
   *
   * a Call Back object with this location is instantiated and added to the all differences set
   * @param request
   * @param response
   * @param body
     * @return
     */
  @RequestMapping(value = "/registerForPotentialDifferences")
  public ResponseEntity<String> registerAD(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) String body)
  {
    Map<String, Object> jsonMap;
        try {
          ObjectMapper mapper = new ObjectMapper();
          jsonMap = mapper.readValue(body, Map.class);
          String callbackString = (String)jsonMap.get("callback");
            this.queryService.addCallBack("potentialDifferences", callbackString);
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }

    return new ResponseEntity<String>("OK", HttpStatus.OK);
  }

  /**
   * Auto wired web entry point
   *
   * expects a body in the form
   * {
   *     "callback":"<CALLBACKLOCATION>"
   * }
   *
   * a Call Back object with this location is instantiated and added to the effective differences set
   * @param request
   * @param response
   * @param body
   * @return
   */
  @RequestMapping(value = "/registerForEffectiveDifferences")
  public ResponseEntity<String> registerED(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) String body)
  {
    Map<String, Object> jsonMap;
    try {
      ObjectMapper mapper = new ObjectMapper();
      jsonMap = mapper.readValue(body, Map.class);
      String callbackString = (String)jsonMap.get("callback");
        this.queryService.addCallBack("effectiveDifferences", callbackString);
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }

    return new ResponseEntity<String>("OK", HttpStatus.OK);
  }

    @RequestMapping(value = "/graphs")
    public ResponseEntity<String> getGraphs(HttpServletRequest request, HttpServletResponse response) throws InvalidSPARQLException
    {
        return new ResponseEntity<String>(GraphService.getGraphInfo(), HttpStatus.OK);
    }

  @RequestMapping(value = "/sparql", produces = {"application/sparql-results+xml", "application/sparql-results+json", "text/html", "text/csv", "*"})
  public ResponseEntity<String> preProcessQuery(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) String body) throws InvalidSPARQLException
  { 
    try {
         /*
         * Getting the query string,... somehow
         */

        String queryString;

        if (request.getParameterMap().containsKey("query")) {
            queryString = request.getParameter("query");
        }
        else
        {
            queryString = URLDecoder.decode(body, "UTF-8");
            if(queryString.toLowerCase().startsWith("update="))
            {
                queryString = queryString.substring(7, queryString.length());
            }
            if(queryString.toLowerCase().startsWith("query="))
            {
                queryString = queryString.substring(6, queryString.length());
            }
        }

        /*
         * Getting the headers ... somehow
         */
        Map<String, String> headers = new HashMap<String, String>();
        Enumeration<String> henum = request.getHeaderNames();
        while(henum.hasMoreElements())
        {
            String headerName = henum.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        

  	    List<Graph> graphs = GraphService.getGraphsForUser(headers.get("userid"));
  	    graphs.size();

        /*
         * if UPDATE then ...
         */
        SPARQLQuery.Type queryType = null;
        try {
            queryType = SPARQLQuery.extractType(queryString);
        }catch(InvalidSPARQLException invalidSPARQLException)
        {
            invalidSPARQLException.printStackTrace();
        }
        if(queryType.equals(SPARQLQuery.Type.UPDATE)) {
            /*
             * Getting the parsed query object ... somehow
             */
            SPARQLQuery parsedQuery = new SPARQLQuery(queryString);


            /*
            TODO this block should be extracted into several methods
             */
            Map<String, List<SimpleStatement>> graphMap = new HashMap<String, List<SimpleStatement>>();
            List<SimpleFunctionStatement> functionalStatements = new ArrayList<SimpleFunctionStatement>();

            if(parsedQuery.getStatements().size() > 0 && parsedQuery.getStatements().get(0).getClass().equals(UpdateBlockStatement.class)) {
                UpdateBlockStatement updateBlockStatement = (UpdateBlockStatement) parsedQuery.getStatements().get(0);
                List<Pair<SimpleStatement, Pair<String, String>>> statementInfo = new ArrayList<Pair<SimpleStatement, Pair<String, String>>>();

                // first I will create a hashmap that contains all typed variables and their type
                Map<String, String> variableTypes = new HashMap<String, String>();
                if (updateBlockStatement.getWhereBlock() != null) {
                    for (IStatement statement : updateBlockStatement.getWhereBlock().getStatements()) {
                        if (statement.getClass().equals(SimpleTripleStatement.class)) {
                            SimpleTripleStatement stt = ((SimpleTripleStatement) statement);
                            if (stt.p.value.equals("a") && stt.s.type.equals(SimpleTripleArgument.Type.Variable)) {
                                variableTypes.put(stt.s.value, stt.o.value);
                            }
                        }
                    }


                    for (IStatement statement : updateBlockStatement.getWhereBlock().getStatements()) {
                        if (statement.getClass().equals(SimpleTripleStatement.class)) {
                            SimpleTripleStatement stt = ((SimpleTripleStatement) statement);
                            String type = "";
                            String predicate = "";
                            if (stt.s.type.equals(SimpleTripleArgument.Type.Variable)) {
                                if (variableTypes.containsKey(stt.s.value)) {
                                    type = variableTypes.get(stt.s.value);
                                }
                            }
                            predicate = stt.p.toString();

                            statementInfo.add(new Pair(statement, new Pair(type, predicate)));
                        } else {
                            if (statement.getClass().equals(SimpleFunctionStatement.class)) {
                                functionalStatements.add((SimpleFunctionStatement) statement);
                            }
                        }
                    }

                    graphMap = this.graphService.getGraphMap(statementInfo, headers.get("userid"));

                }
            }
            /*

             */

            // prepare the query object
            QueryInfo queryInfo = new QueryInfo();
            queryInfo.headers = headers;
            queryInfo.endpoint = Configuration.updateEndpoint;
            queryInfo.originalQuery = queryString;
            queryInfo.query = parsedQuery;
            queryInfo.userId = headers.get("userid");
            queryInfo.graphMap = graphMap;
            queryInfo.functionalStatements = functionalStatements;
            queryInfo.user = headers.get("userid");

            // register it for processing
            this.queryService.registerUpdateQuery(queryInfo);

            // while it has not been process ... wait
            while(this.queryService.getProcessedQueries().contains(queryInfo) == false)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // extract the response and remove all other info
            String queryResponse = queryInfo.response.responseText;
            this.queryService.getProcessedQueries().remove(queryInfo);

            // setting the headers...
            if(queryInfo.response.responseHeaders != null)
            {
                for(String header : queryInfo.response.responseHeaders.keySet())
                {
                    response.setHeader(header, queryInfo.response.responseHeaders.get(header));
                }
            }

            // and then return the result
            return new ResponseEntity<String>(queryInfo.response.responseText, HttpStatus.OK);
        }

        /**
         * If we are dealing with a SELECT query we can just return the response as is...
         */
        if(!queryType.equals(SPARQLQuery.Type.UPDATE))
        {
        	String scopedQuery = "";

            SPARQLQuery parsedQuery = new SPARQLQuery(queryString);
            
            for(String prefix : parsedQuery.getPrefixes().keySet())
            {
            	scopedQuery += "PREFIX " + prefix + ": <" + parsedQuery.getPrefixes().get(prefix) + ">\n";
            }
            
            if(!headers.containsKey("accept") || !headers.get("accept").equalsIgnoreCase("application/json"))
            {
            	return new ResponseEntity<String>("Only Accept: application/json supported", HttpStatus.BAD_REQUEST);
            }
            
            if(parsedQuery.getStatements().size() != 1 || !parsedQuery.getStatements().get(0).getClass().equals(SelectBlock.class))
            {
            	return new ResponseEntity<String>("Queries of this type not (yet) supported", HttpStatus.BAD_REQUEST);
            }
            
            SelectBlock selectBlock = ((SelectBlock) parsedQuery.getStatements().get(0));
            
            scopedQuery += "SELECT " + selectBlock.getSelectClause() + "\n";

            List<Pair<SimpleStatement, Pair<String, String>>> statementInfo = new ArrayList<Pair<SimpleStatement, Pair<String, String>>>();
            List<SimpleFunctionStatement> functionalStatements = new ArrayList<SimpleFunctionStatement>();

            // first I will create a hashmap that contains all typed variables and their type
            Map<String, String> variableTypes = new HashMap<String, String>();
            for(IStatement statement : selectBlock.getStatements())
            {
                if(statement.getClass().equals(SimpleTripleStatement.class))
                {
                    SimpleTripleStatement stt = ((SimpleTripleStatement)statement);
                    if(stt.p.value.equals("a") && stt.s.type.equals(SimpleTripleArgument.Type.Variable))
                    {
                        variableTypes.put(stt.s.value, stt.o.value);
                    }
                }
            }

            for(IStatement statement : selectBlock.getStatements())
            {
                if(statement.getClass().equals(SimpleTripleStatement.class)) {
                    SimpleTripleStatement stt = ((SimpleTripleStatement) statement);
                    String type = "";
                    String predicate = "";
                    if (stt.s.type.equals(SimpleTripleArgument.Type.Variable)) {
                        if (variableTypes.containsKey(stt.s.value)) {
                            type = variableTypes.get(stt.s.value);
                        }
                    }
                    predicate = stt.p.toString();

                    statementInfo.add(new Pair(statement, new Pair(type, predicate)));
                }
                else {
                     if(statement.getClass().equals(SimpleFunctionStatement.class)) {
                        functionalStatements.add((SimpleFunctionStatement)statement);
                     }
                }
            }

            Map<String, List<SimpleStatement>> graphMap = this.graphService.getGraphMap(statementInfo, headers.get("userid"));

            for(String graphName : graphMap.keySet())
            {
                if(graphMap.get(graphName).isEmpty())continue;;

                scopedQuery += "FROM NAMED <" + graphName + ">\n";
            }

            scopedQuery += "\nWHERE\n{\n";

            for(Pair<SimpleStatement, Pair<String, String>> si : statementInfo)
            {
                scopedQuery += si.getLeft() + " .\n";
            }

            for(SimpleFunctionStatement simpleFunctionStatement : functionalStatements)
            {
                scopedQuery += simpleFunctionStatement.toString() + "\n";
            }

            scopedQuery += "}";
            
           // System.out.println("reformed whereblock: " + GraphService.transformWhereBlockForUser(headers.get("userid"), selectBlock));
            
            //scopedQuery += selectBlock.getSelectModifier() + " " + selectBlock.getSolutionModifier();
            for(String mod : selectBlock.getSolutionModifier())
            	scopedQuery += mod + " ";

            return new ResponseEntity<String>(scopedQuery, HttpStatus.OK);
//            System.out.println(scopedQuery);
//
//
//            Response sparqlResponse = this.queryService.sparqlService.getSPARQLResponse(Configuration.getProperty("queryURL") + "?query=" + URLEncoder.encode(scopedQuery, "UTF-8"), headers);
//            String qrp = sparqlResponse.responseText;
//
//            // extracting the graphs...
//	        ObjectMapper mapper = new ObjectMapper();
//	        JSONResponse testResponse = mapper.readValue(qrp, JSONResponse.class);
//	        Set<String> sourceGraphs = new HashSet<String>();
//	        List l =((List<Map<String,Object>>)((Map) testResponse.results).get("bindings"));
//	        List<Triple> triples = new ArrayList<Triple>();
//
//	        for(Object tripleMap : l)
//	        {
//	            Map<String, Object> cTripleMap = (Map<String, Object>) tripleMap;
//	            Map<String, Object> sMap = (Map<String, Object>) cTripleMap.get("original_graph_for_scope_service");
//	            //System.out.println("Source graph: " + ((String)sMap.get("value")));
//	            sourceGraphs.add((String) sMap.get("value"));
//	        }
//
//	        Vector<String> v = new Vector<String>(sourceGraphs);
//	        testResponse.graphs = new String[v.size()];
//	        for(int i = 0; i < v.size(); ++i)
//	        	testResponse.graphs[i] = v.get(i);
//
//            for(String header:sparqlResponse.responseHeaders.keySet())
//            {
//                response.setHeader(header, sparqlResponse.responseHeaders.get(header));
//            }


            //return new ResponseEntity<String>(mapper.writeValueAsString(testResponse), HttpStatus.OK);
        }

    }catch(InvalidSPARQLException e)
    {
      e.printStackTrace();
//    } catch (MalformedURLException e) {
//        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }

      return ResponseEntity.ok("");
  }

    public List<QueryStatement> getQueryStatementsFromStatement(IStatement statement)
    {
        List<QueryStatement> statements = new ArrayList<QueryStatement>();
        for(String stat : statement.toString().split("."))
        {
            statements.add(toQueryStatement(stat));
        }
        return statements;
    }

    public QueryStatement toQueryStatement(String s)
    {
        QueryStatement statement = new QueryStatement();
        // check the special case
        if(s.toLowerCase().trim().startsWith("filter"))
        {
            statement.queryStatment = s;
            return statement;
        }

        // get the first space
        int firstSpace = s.indexOf(" ");

        return null;
    }
}
