package delta_service.graphs;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import SPARQLParser.SPARQLStatements.IStatement;
import SPARQLParser.SPARQLStatements.SelectBlock;
import SPARQLParser.SPARQLStatements.SimpleStatement;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import delta_service.Util.Pair;
import delta_service.config.Configuration;
import delta_service.query.*;
import org.springframework.stereotype.Service;

@Service
public class GraphService {
	private static GraphInfoFile gif = null;

	public static String getGraphInfo()
	{
		if(GraphService.gif == null)
		{
			return "NO GRAPH CONFIG LOADED";
		}

		String toreturn = "<table border=\"1\"><tr><td>NAME</td><td>GRAPH</td><td>HIVE/PERSONAL</td><td>PREDICATE</td><td>TYPE</td><td>FALLTHROUGH</td></tr>";
		for(GraphsInfo g : GraphService.gif.getGraphsInfo())
		{
			toreturn += "<tr><td>" + g.getName() + "</td><td>" +
					g.getGraph() + "</td><td>" +
					g.getType() + "</td><td>" +
					g.getPredicateRestriction().replace("<", "").replace(">", "") + "</td><td>" +
					g.getTypeRestriction().replace("<", "").replace(">", "") + "</td><td>" +
					g.isFallthroughRestriction() + "</td></tr>";
		}

		toreturn += "</table>";
		return toreturn;
	}

	public static List<Graph> getGraphsForUser(String UUIDUser)
	{
		List<Graph> graphs = new ArrayList<Graph>();
		
		String graphQuery = "PREFIX mu: <http://mu.semte.ch/vocabularies>" +
				"PREFIX mus: <http://mu.semte.ch/vocabularies/session>" +
				"PREFIX auth: <http://mu.semte.ch/vocabularies/authorization/>" +
				"" +
				"SELECT ?graph " +
				"WHERE " +
				"{" +
				"GRAPH <http://protected/graphs> {" +
				"  ?user mu:uuid \"" + UUIDUser + "\" ." +
				"  ?user auth:hasRight ?grant ." +
				"  ?grant auth:operatesOn ?graph ." +
				"}" +
				"}";
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		
		try {
			Response sparqlResponse = new QueryService().sparqlService.getSPARQLResponse(Configuration.queryEndpoint + "?query=" + URLEncoder.encode(graphQuery, "UTF-8"), headers);
			System.out.println(sparqlResponse.responseText);
			
	        String jsonString = sparqlResponse.responseText;

	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Object> jsonMap = mapper.readValue(jsonString, Map.class);

	        List l =((List<Map<String,Object>>)((Map) jsonMap.get("results")).get("bindings"));
	        List<Triple> triples = new ArrayList<Triple>();

	        for(Object tripleMap : l)
	        {
	            Map<String, Object> cTripleMap = (Map<String, Object>) tripleMap;
	            
	            Map<String, Object> sMap = (Map<String, Object>) cTripleMap.get("graph");
	            String sValue = (String) sMap.get("value");
	            Graph g = new Graph();
	            g.setGraph(sValue);
	            graphs.add(g);
	        }
	        
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		
		return graphs;
	}
	
	public static List<YAMLScope> getScopesForUser(String userUUID)
	{
		List<YAMLScope> lscopes = Configuration.getScopesConfiguration().getScopes();
		List<Graph> graphs = GraphService.getGraphsForUser(userUUID);
		List<YAMLScope> rscopes = new ArrayList<YAMLScope>();
		
		for(YAMLScope scope : lscopes)
		{
			boolean userHasAccess = false;
			for(Graph g:graphs)
			{
				if(g.getGraph().equals(scope.getGraph()))
					userHasAccess = true;
			}
			if(!userHasAccess)
				rscopes.add(scope);
		}
		List<YAMLScope> toreturn = new ArrayList<YAMLScope>();
		for(YAMLScope scope : lscopes)
			if(!rscopes.contains(scope))
				toreturn.add(scope);
		return toreturn;
	}
	
	public static Map<YAMLScope, List<String>> buildScopesMap(List<YAMLScope> scopes)
	{
		Map<YAMLScope, List<String>> scopesMap = new HashMap<YAMLScope, List<String>>();
		for(YAMLScope scope : scopes)
		{
			scopesMap.put(scope, new ArrayList<String>());
		}
		return scopesMap;
	}
	
	public static boolean isGeneralStatement(String statement)
	{
		if(statement.toLowerCase().contains("filter"))
			return true;
		return false;
	}
	
	public static void addToGeneralList(String statement, List<String> generalStatements)
	{
		generalStatements.add(statement);
	}
	
	public static void addToScopesMap(String statement, Map<YAMLScope, List<String>> scopesMap)
	{
		for(YAMLScope scope : scopesMap.keySet())
		{
			if(scope.canHandleLine(statement))
			{
				scopesMap.get(scope).add(statement);
				if(!scope.fallThrough(statement))
					return;
			}
		}
	}
	
	public static String buildWhereBlock(List<String> generalStatements, Map<YAMLScope, List<String>> scopesMap)
	{
		String wb = "{\n";
		
		for(YAMLScope scope:scopesMap.keySet())
		{
			List<String> statementsForScope = scopesMap.get(scope);
			if(statementsForScope.size() > 0)
			{
				wb += "GRAPH <" + scope.getGraph() + ">\n{\n";
				for(String statement : statementsForScope)
					wb += statement + "\n";
				wb += "}\n";
			}
		}
		
		for(String statement : generalStatements)
			wb += statement + "\n";
		wb += "}";
		
		return wb;
	}
	
	public static String transformWhereBlockForUser(String userUUID, SelectBlock selectBlock)
	{		
		List<YAMLScope> lscopes = GraphService.getScopesForUser(userUUID);
		
		List<String> generalStatements = new ArrayList<String>();
		Map<YAMLScope, List<String>> scopesMap = GraphService.buildScopesMap(lscopes);
		
		for(IStatement statement:selectBlock.getStatements())
		{
			if(statement.getClass().equals(SimpleStatement.class)){
				// process a simple block statement
				SimpleStatement sStatement = (SimpleStatement)statement;
				if(GraphService.isGeneralStatement(sStatement.toString()))
				{
					GraphService.addToGeneralList(sStatement.toString(), generalStatements);
				}
				else
				{
					GraphService.addToScopesMap(sStatement.toString(), scopesMap);
				}
			}
		}
		
		return buildWhereBlock(generalStatements, scopesMap);
	}

	public static void loadGraphsFromYAML()
	{
		File file = new File("/config/graphs.yml");
		final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		GraphInfoFile gif = null;
		try {
			gif = mapper.readValue(file, GraphInfoFile.class);
			GraphService.gif = gif;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> getGraphsForStatement(String subject, String predicate, String object, boolean isURI, String language, String datatype, String user, Map<String, List<String>> localSubjectTypes)
	{
		List<String> subjectTypes = new ArrayList<String>();

		List<String> graphs = new ArrayList<String>();

		if(predicate.equals("a") || predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
		{
			subjectTypes.add(object);
		}

		String queryForSubjectType = "SELECT distinct ?type WHERE { <" + subject + "> a ?type . }";

		SPARQLService sparqlService = new SPARQLService();
		try {
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept", "application/json");
			Response response = sparqlService.getSPARQLResponse("http://localhost:8890/sparql?query=" + URLEncoder.encode(queryForSubjectType, "UTF-8"),
					headers);

			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> jsonMap = mapper.readValue(response.responseText, Map.class);

			List<Map<String, Object>> l =((List<Map<String,Object>>)((Map) jsonMap.get("results")).get("bindings"));

			for(Map<String, Object> m : l)
			{
				Map<String, Object> typeMap = (Map<String, Object>)m.get("type");
				subjectTypes.add((String) typeMap.get("value"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for(GraphsInfo graphsInfo : GraphService.gif.getGraphsInfo())
		{
			String graphName = graphsInfo.getGraph();
			if(graphsInfo.getType().equalsIgnoreCase("personal")) {
				graphName += "/" + user;
			}
			if(graphsInfo.getTypeRestriction().isEmpty() && graphsInfo.getPredicateRestriction().isEmpty())
			{
				graphs.add(graphName);
				if(!graphsInfo.isFallthroughRestriction()) {
					return graphs;
				}
			}
			else
			{
				if(graphsInfo.getPredicateRestriction().equals("<" + predicate + ">"))
				{
					graphs.add(graphName);
					if(!graphsInfo.isFallthroughRestriction()){
						return  graphs;
					}
				}
				else
				{
					for(String subjectType : subjectTypes) {
						if(subjectType.equals(graphsInfo.getTypeRestriction()))
						{
							graphs.add(graphName);
							if(!graphsInfo.isFallthroughRestriction()) {
								return graphs;
							}
						}
					}
					if(localSubjectTypes.containsKey(subject))
					{
						for(String subjectType : localSubjectTypes.get(subject)) {
							if (subjectType.equals(graphsInfo.getTypeRestriction())) {
								graphs.add(graphName);
								if (!graphsInfo.isFallthroughRestriction()) {
									return graphs;
								}
							}
						}
					}
				}
			}
		}
		return graphs;
	}

	// get graph map for statement lists
	public static Map<String, List<SimpleStatement>> getGraphMap(List<Pair<SimpleStatement, Pair<String, String>>> statementList, String userUUID)
	{
		Map<String, List<SimpleStatement>> graphMap = new HashMap<String, List<SimpleStatement>>();

		for(Pair<SimpleStatement, Pair<String, String>> statementEntry : statementList)
		{
			SimpleStatement statement = statementEntry.getLeft();
			String objectRestriction = statementEntry.getRight().getLeft();
			String predicateRestriction = statementEntry.getRight().getRight();

			// if both subject and predicate are variables then we just add all graphs
			if(objectRestriction.startsWith("?") && predicateRestriction.startsWith("?"))
			{
				for(GraphsInfo graphsInfo : GraphService.gif.getGraphsInfo())
				{
					String graphName = graphsInfo.getGraph();
					if(graphsInfo.getType().equalsIgnoreCase("personal"))
					{
						graphName += "/" + userUUID;
					}

					if(!graphMap.containsKey(graphName))
					{
						graphMap.put(graphName, new ArrayList<SimpleStatement>());
					}
					graphMap.get(graphName).add(statement);
				}
				continue;
			}

			// else we try to figure things out..
			boolean written = false;

			for(GraphsInfo graphsInfo : GraphService.gif.getGraphsInfo())
			{
				if(written == true)
					continue;

				String graphName = graphsInfo.getGraph();
				if(graphsInfo.getType().equalsIgnoreCase("personal"))
				{
					graphName += "/" + userUUID;
				}

				if(graphsInfo.getPredicateRestriction().isEmpty() && graphsInfo.getTypeRestriction().isEmpty())
				{
					if(!graphMap.containsKey(graphName))
					{
						graphMap.put(graphName, new ArrayList<SimpleStatement>());
					}
					graphMap.get(graphName).add(statement);
					written = true;
				}

				if(!graphsInfo.getTypeRestriction().isEmpty() && graphsInfo.getPredicateRestriction().isEmpty())
				{
					if(graphsInfo.getTypeRestriction().equals(objectRestriction) || objectRestriction.startsWith("?"))
					{
						if(!graphMap.containsKey(graphName))
						{
							graphMap.put(graphName, new ArrayList<SimpleStatement>());
						}
						graphMap.get(graphName).add(statement);

						// only set written to yes if the typerestriction really was equal and if
						// this graph has no fall through
						written = graphsInfo.getTypeRestriction().equals(objectRestriction) && !graphsInfo.isFallthroughRestriction();
					}
				}

				if(graphsInfo.getTypeRestriction().isEmpty() && !graphsInfo.getPredicateRestriction().isEmpty())
				{
					if(graphsInfo.getPredicateRestriction().equals(predicateRestriction) || predicateRestriction.startsWith("?"))
					{
						if(!graphMap.containsKey(graphName))
						{
							graphMap.put(graphName, new ArrayList<SimpleStatement>());
						}
						graphMap.get(graphName).add(statement);

						// only set written to yes if the predicate restriction really was equal and if
						// this graph has no fall through
						written = graphsInfo.getPredicateRestriction().equals(predicateRestriction) && !graphsInfo.isFallthroughRestriction();
					}
				}

				if(!graphsInfo.getTypeRestriction().isEmpty() && !graphsInfo.getPredicateRestriction().isEmpty())
				{
					if(graphsInfo.getTypeRestriction().equals(objectRestriction) &&
							graphsInfo.getPredicateRestriction().equals(predicateRestriction))
					{
						if(!graphMap.containsKey(graphName))
						{
							graphMap.put(graphName, new ArrayList<SimpleStatement>());
						}
						graphMap.get(graphName).add(statement);
						written = true;
					}
				}
			}
		}

		return graphMap;
	}

//	public static void saveGraphsToYAML()
//	{
//		GraphInfoFile gif = new GraphInfoFile();
//
//		GraphsInfo uuid = new GraphsInfo();
//		uuid.setName("uuid graph");
//		uuid.setTypeRestriction("ShoppingCart");
//		uuid.setType("hive");
//		uuid.setFallthroughRestriction(false);
//		uuid.setPredicateRestriction("");
//		uuid.setGraph("http://mu.semte.ch/uuid");
//
//		GraphsInfo gi1 = new GraphsInfo();
//		gi1.setName("personalGraph-shoppingCart");
//		gi1.setTypeRestriction("ShoppingCart");
//		gi1.setType("personal");
//		gi1.setFallthroughRestriction(false);
//		gi1.setPredicateRestriction("");
//		gi1.setGraph("http://mu.semte.ch/personal");
//
//		GraphsInfo gi12 = new GraphsInfo();
//		gi12.setName("personalGraph-shoppingCartPredicate");
//		gi12.setTypeRestriction("");
//		gi12.setType("personal");
//		gi12.setFallthroughRestriction(false);
//		gi12.setPredicateRestriction("http://example.com/shopping/containsItem");
//		gi12.setGraph("http://mu.semte.ch/personal");
//
//		GraphsInfo gi2 = new GraphsInfo();
//		gi2.setName("rest basket");
//		gi2.setTypeRestriction("");
//		gi2.setType("hive");
//		gi2.setFallthroughRestriction(true);
//		gi2.setPredicateRestriction("");
//		gi2.setGraph("http://mu.semte.ch/application");
//		List<GraphsInfo> gi = new ArrayList<GraphsInfo>();
//		gi.add(uuid);
//		gi.add(gi1);
//		gi.add(gi12);
//		gi.add(gi2);
//		gif.setGraphsInfo(gi);
//
//
//		final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//
//		try {
//			mapper.writeValue(new File("/home/langens-jonathan/Downloads/example.yml"), gif);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
