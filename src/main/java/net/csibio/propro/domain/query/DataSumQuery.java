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

    public void addStatus(Integer status) {
        if (status == null) {
            statusList = new ArrayList<Integer>();
        }
        statusList.add(status);
    }
}
