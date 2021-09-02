package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OverviewQuery extends PageQuery {

    String id;

    String projectId;

    Boolean defaultOne;

    String name;

    String insLibId;

    String anaLibId;

    String methodId;

    String expId;

    List<String> expIds;
    
    public OverviewQuery() {
    }

    public OverviewQuery(String projectId) {
        this.projectId = projectId;
    }

    public OverviewQuery(String projectId, String expId) {
        this.projectId = projectId;
        this.expId = expId;
    }

}
