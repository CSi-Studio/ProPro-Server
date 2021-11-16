package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OverviewQuery extends PageQuery {

    String id;

    List<String> ids;

    String projectId;

    Boolean defaultOne;

    String name;

    String insLibId;

    String anaLibId;

    String methodId;

    String runId;

    List<String> runIds;

    public OverviewQuery() {
    }

    public OverviewQuery(String projectId) {
        this.projectId = projectId;
    }

    public OverviewQuery(String projectId, String runId) {
        this.projectId = projectId;
        this.runId = runId;
    }

}
