package delta_service.graphs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YAMLScopesFile {
	@JsonProperty
	private List<YAMLScope> scopes;

	public List<YAMLScope> getScopes() {
		return scopes;
	}

	public void setScopes(List<YAMLScope> scopes) {
		this.scopes = scopes;
	}

}
