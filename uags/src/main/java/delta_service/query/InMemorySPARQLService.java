package delta_service.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import delta_service.graphs.Graph;

public class InMemorySPARQLService {
	
	public void biteMe()
	{
		// creating the model
		Model model = ModelFactory.createDefaultModel();
		
		// creating some in memory URI's and Properties (as Jena differentiates between them...)
		Resource testInstance = model.createResource("http://test/test1");
		Property is_a = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource testClass = model.createResource("http://test/Test");
		
		// creating and adding a statement to our in memory SPAQRL store
		Statement s = model.createStatement(testInstance, is_a, testClass);
		model.add(s);
		
		// creating a query
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setCommandText("select * where {?s ?p ?o.}");
		Query q = pss.asQuery();
		
		// executing that query
		QueryExecution qe = QueryExecutionFactory.create(q, model);
		ResultSet rs = qe.execSelect();
		while(rs.hasNext())
		{
			QuerySolution qs = rs.next();
			// do stuff with qs here...
		}
	}
	
	public List<String> getUpdateQueries(Map<String, DifferenceTriples> differenceTriplesMap, List<Graph> graphs)
	{
		// this should be different functions for each block....
		
		// getting all inserts and deletes in to 2 sets for further processing
		List<String> updateQueries = new ArrayList<String>();
		
		Set<Triple> inserts = new HashSet<Triple>();
		Set<Triple> deletes = new HashSet<Triple>();
		
		for(String graph : differenceTriplesMap.keySet())
		{
			inserts.addAll(differenceTriplesMap.get(graph).getAllInsertTriples());
			deletes.addAll(differenceTriplesMap.get(graph).getAllDeleteTriples());	
		}
		
		// adding the deletes to a model
		Model deleteModel = ModelFactory.createDefaultModel();
		
		for(Triple i : deletes)
		{
			if(i.getObjectType().equalsIgnoreCase("uri"))
			{
				deleteModel.add(
						deleteModel.createStatement(
								deleteModel.createResource(i.getSubject()),
								deleteModel.createProperty(i.getPredicate()),
								deleteModel.createResource(i.getObjectString()))
							);
			}
			else
			{
				deleteModel.add(
						deleteModel.createStatement(
								deleteModel.createResource(i.getSubject()),
								deleteModel.createProperty(i.getPredicate()),
								deleteModel.createResource(i.getObjectAsString()))
								);
			}
		}
		
		for(int i = graphs.size() - 1; i >= 0 ; --i)
		{
			Graph graph = graphs.get(i);
			String query = "WITH <" + graph.getGraph() + "> DELETE DATA { ";
			// creating a query
			ParameterizedSparqlString pss = new ParameterizedSparqlString();
			pss.setCommandText(graph.getUpdateFiler());
			Query q = pss.asQuery();
			
			// executing that query
			QueryExecution qe = QueryExecutionFactory.create(q, deleteModel);
			ResultSet rs = qe.execSelect();
			boolean hasSolutions = false;
			while(rs.hasNext())
			{
				hasSolutions = true;
				QuerySolution qs = rs.next();
				// do stuff with qs here...
				query += "<" + qs.get("s") + "> <" + qs.get("p") + "> ";
				if(qs.get("o").isURIResource())
					query += "<" + qs.get("o") + "> . ";
				else
					query += "\"" + qs.get("o") + "\" . ";
			}
			
			query += "}";
			System.out.println(query);
			if(hasSolutions)updateQueries.add(query);
			
			// creating a query
			ParameterizedSparqlString dpss = new ParameterizedSparqlString();
			dpss.setCommandText(graph.getUpdateFiler().replaceAll("SELECT ?s ?p ?o", "DELETE { ?s ?p ?o }"));
			Query dq = dpss.asQuery();
			QueryExecution dqe = QueryExecutionFactory.create(dq, deleteModel);
			dqe.execSelect();
			
		}
		// adding the inserts to a model
		Model insertModel = ModelFactory.createDefaultModel();
		
		for(Triple i : inserts)
		{
			if(i.isObjectIsURI())
			{
				insertModel.add(
					insertModel.createStatement(
							insertModel.createResource(i.getSubject()),
							insertModel.createProperty(i.getPredicate()),
							insertModel.createResource(i.getObjectString()))
							);
			}
			else
			{
				insertModel.add(
						insertModel.createStatement(
								insertModel.createResource(i.getSubject()),
								insertModel.createProperty(i.getPredicate()),
								insertModel.createResource(i.getObjectAsString()))
								);
			}
		}
		
		for(int i = graphs.size() - 1; i >= 0 ; --i)
		{
			Graph graph = graphs.get(i);
			String query = "WITH <" + graph.getGraph() + "> INSERT DATA { ";
			// creating a query
			ParameterizedSparqlString pss = new ParameterizedSparqlString();
			pss.setCommandText(graph.getUpdateFiler());
			Query q = pss.asQuery();
			
			// executing that query
			QueryExecution qe = QueryExecutionFactory.create(q, insertModel);
			ResultSet rs = qe.execSelect();
			boolean hadSolutions = false;
			while(rs.hasNext())
			{
				hadSolutions = true;
				QuerySolution qs = rs.next();
				// do stuff with qs here...
				query += "<" + qs.get("s") + "> <" + qs.get("p") + "> ";
				if(qs.get("o").isURIResource())
					query += "<" + qs.get("o") + "> . ";
				else
					query += "\"" + qs.get("o") + "\" . ";
			}
			
			query += "}";
			System.out.println(query);
			if(hadSolutions)updateQueries.add(query);
			
			// creating a query
			ParameterizedSparqlString dpss = new ParameterizedSparqlString();
			dpss.setCommandText(graph.getUpdateFiler().replaceAll("SELECT ?s ?p ?o", "DELETE { ?s ?p ?o }"));
			Query dq = dpss.asQuery();
			QueryExecution dqe = QueryExecutionFactory.create(dq, insertModel);
			dqe.execSelect();
			
		}
		
		return updateQueries;
	}
}
