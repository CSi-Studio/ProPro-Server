package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:06
 */
@Data
@Accessors(chain = true)
public class TaskQuery extends PageQuery {

    String id;

    String name;

    String runId;

    String taskTemplate;

    List<String> statusList;

    public void setStatus(String status) {
        if (statusList == null) {
            statusList = new ArrayList<>();
        } else {
            statusList.clear();
        }
        statusList.add(status);
    }

    public void addStatus(String status) {
        if (statusList == null) {
            statusList = new ArrayList<>();
        }
        statusList.add(status);
    }

    public void clearStatus() {
        if (statusList == null) {
            return;
        }
        statusList.clear();
    }
}
