package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class DataQuery extends PageQuery {

    String id;

    String overviewId;

    String peptideRef;

    String proteinIdentifier;

    Boolean decoy;

    List<Integer> statusList;

    Double mzStart;

    Double mzEnd;
    
    Double fdrStart;

    Double fdrEnd;

    Double qValueStart;

    Double qValueEnd;

    public DataQuery() {
    }

    public DataQuery(String overviewId) {
        this.overviewId = overviewId;
    }

    public DataQuery(int pageNo, int pageSize) {
        super(pageNo, pageSize);
    }

    public void addStatus(Integer status) {
        if (status == null) {
            statusList = new ArrayList();
        }
        statusList.add(status);
    }
}
