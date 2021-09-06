package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class DataSumQuery extends PageQuery {

    String id;

    String overviewId;

    Boolean isUnique;

    String peptideRef;

    Boolean decoy;

    List<Integer> statusList;

    Double fdrStart;

    Double fdrEnd;

    public DataSumQuery() {
    }

    public DataSumQuery(String overviewId) {
        this.overviewId = overviewId;
    }

    public DataSumQuery(int pageNo, int pageSize) {
        super(pageNo, pageSize);
    }

    public DataSumQuery addStatus(Integer status) {
        if (statusList == null) {
            statusList = new ArrayList<Integer>();
        }
        statusList.add(status);
        return this;
    }
}
