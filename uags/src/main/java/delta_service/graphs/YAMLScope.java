package delta_service.graphs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YAMLScope {
	@JsonProperty
	private YAMLPatternType type;
	
	@JsonProperty
	private boolean fallthrough;
	
	@JsonProperty
	private YAMLOperatesOnType operatesOn;
	
	@JsonProperty
	private String selector;
	
	@JsonProperty
	private String graph;

	public YAMLPatternType getType() {
		return type;
	}

	public void setType(YAMLPatternType type) {
		this.type = type;
	}

	public boolean isFallthrough() {
		return fallthrough;
	}

	public void setFallthrough(boolean fallthrough) {
		this.fallthrough = fallthrough;
	}

	public YAMLOperatesOnType getOperatesOn() {
		return operatesOn;
	}

	public void setOperatesOn(YAMLOperatesOnType operatesOn) {
		this.operatesOn = operatesOn;
	}

	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	public String getGraph() {
		return graph;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}
	
	public boolean canHandleLine(String line)
	{
		String[] parts = line.trim().split(" ");
		if(parts.length < 3)return false;
		return this.canHandleTriple(parts[0], parts[1], parts[2]);
	}

	public boolean canHandleTriple(String s, String p, String o)
	{		
		if(this.getSelector().equals("*"))
			return true;
	
		if(this.getOperatesOn().equals(YAMLOperatesOnType.predicate))
		{
			if(p.equals("<" + this.getSelector() + ">") || p.contains("?"))
			{
				return true;
			}
		}
		else
		{
			if(ResourceService.getTypeForURI(s).equals("<" + this.getSelector() + ">") || s.contains("?"))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean fallThrough(String s, String p, String o)
	{			
		if(this.getOperatesOn().equals(YAMLOperatesOnType.predicate))
		{
			if(p.contains("?"))
			{
				return true;
			}
		}
		else
		{
			if(s.contains("?"))
			{
				return true;
			}
		}

		return this.isFallthrough();
	}
	
	public boolean fallThrough(String line)
	{
		String[] parts = line.split(" ");
		if(parts.length != 3)return true;
		return this.fallThrough(parts[0], parts[1], parts[2]);
	}
}
