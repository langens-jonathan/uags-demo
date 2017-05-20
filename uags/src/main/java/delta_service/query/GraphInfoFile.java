package delta_service.query;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by langens-jonathan on 10.05.17.
 */
public class GraphInfoFile {
    @JsonProperty
    private List<GraphsInfo> graphsInfo;

    public List<GraphsInfo> getGraphsInfo() {
        return graphsInfo;
    }

    public void setGraphsInfo(List<GraphsInfo> graphsInfo) {
        this.graphsInfo = graphsInfo;
    }
}
