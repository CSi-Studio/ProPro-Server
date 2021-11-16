package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BlockIndexQuery extends PageQuery {

    private static final long serialVersionUID = -3258829832460821645L;

    String id;

    String runId;

    Integer level;

    //前体的荷质比窗口开始位置,已经经过overlap参数调整
    Double mzStart;

    //前体的荷质比窗口结束位置,已经经过overlap参数调整
    Double mzEnd;

    //根据目标的前体MZ获取相关的窗口
    Double mz;

    public BlockIndexQuery() {

    }

    public BlockIndexQuery(String runId, Integer msLevel) {
        this.runId = runId;
        this.level = msLevel;
    }
}
