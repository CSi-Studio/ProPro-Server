package net.csibio.propro.service;

import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.ExpDataVO;

public interface DataService extends BaseMultiService<DataDO, DataQuery> {

    ExpDataVO getData(String projectId, String expId, String overviewId, String peptideRef);
}
