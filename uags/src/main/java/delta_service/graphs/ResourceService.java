package delta_service.graphs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import delta_service.config.Configuration;
import delta_service.query.QueryService;
import delta_service.query.Response;
import delta_service.query.Triple;

public class ResourceService {
	private static Map<String, String> resources = new HashMap<String, String>();
	
	public static String getTypeForURI(String URI)
	{
		if(!ResourceService.resources.containsKey(URI))
			resources.put(URI, getTypeForObject(URI));
		return resources.get(URI);
	}
	
	private static String getTypeForObject(String URI)
	{
		String typeQuery = 
					"SELECT ?type \n" +
					"WHERE " +
					"{\n" +
					 "<" + URI + "> a ?type .\n" +
					"}";
			
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Accept", "application/json");
			
			try {
				Response sparqlResponse = new QueryService().sparqlService.getSPARQLResponse(Configuration.getProperty("queryURL") + "?query=" + URLEncoder.encode(typeQuery, "UTF-8"), headers);
				System.out.println(sparqlResponse.responseText);
				
		        String jsonString = sparqlResponse.responseText;

		        ObjectMapper mapper = new ObjectMapper();
		        Map<String, Object> jsonMap = mapper.readValue(jsonString, Map.class);

		        List l =((List<Map<String,Object>>)((Map) jsonMap.get("results")).get("bindings"));
		        List<Triple> triples = new ArrayList<Triple>();

		        for(Object tripleMap : l)
		        {
		            Map<String, Object> cTripleMap = (Map<String, Object>) tripleMap;
		            
		            Map<String, Object> sMap = (Map<String, Object>) cTripleMap.get("type");
		            return(String) sMap.get("value");
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
	        
			
			return "";
	}

}
