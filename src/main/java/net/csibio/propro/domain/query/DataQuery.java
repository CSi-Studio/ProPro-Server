package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataQuery extends PageQuery {

    String id;

    //路由id
    String projectId;

    String overviewId;

    Integer status;

    String peptideRef;

    String protein;

    Boolean decoy;

    Double mzStart;

    Double mzEnd;

    public DataQuery() {
    }

    public DataQuery(String overviewId) {
        this.overviewId = overviewId;
    }

    public DataQuery(int pageNo, int pageSize) {
        super(pageNo, pageSize);
    }
}
