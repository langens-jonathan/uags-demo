package delta_service.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by langens-jonathan on 10.05.17.
 */
public class GraphsInfo {
    @JsonProperty
    private String name;

    @JsonProperty
    private String type;

    @JsonProperty
    private String typeRestriction;

    @JsonProperty
    private String predicateRestriction;

    @JsonProperty
    private boolean fallthroughRestriction;

    @JsonProperty
    private String graph;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeRestriction() {
        return typeRestriction;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {

        return type;
    }

    public void setTypeRestriction(String typeRestriction) {
        this.typeRestriction = typeRestriction;
    }

    public String getPredicateRestriction() {
        return predicateRestriction;
    }

    public void setPredicateRestriction(String predicateRestriction) {
        this.predicateRestriction = predicateRestriction;
    }

    public boolean isFallthroughRestriction() {
        return fallthroughRestriction;
    }

    public void setFallthroughRestriction(boolean fallthroughRestriction) {
        this.fallthroughRestriction = fallthroughRestriction;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }
}
