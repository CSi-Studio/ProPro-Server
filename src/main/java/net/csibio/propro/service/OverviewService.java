package net.csibio.propro.service;

import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;

import java.util.List;

public interface OverviewService extends BaseService<OverviewDO, OverviewQuery> {

    List<OverviewDO> getDefaultOverviewList(String projectId, List<String> expIds);
}
