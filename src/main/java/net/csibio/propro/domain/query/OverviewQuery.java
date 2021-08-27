package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OverviewQuery extends PageQuery {

    String id;

    String projectId;

    String name;

    String insLibId;

    String anaLibId;

    String methodId;

    String expId;
    public OverviewQuery() {
    }

    public OverviewQuery(String projectId) {
        this.projectId = projectId;
    }

    public OverviewQuery(String projectId,String expId){
         this.projectId=projectId;
         this.expId=expId;
    }

}
