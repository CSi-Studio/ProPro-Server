package net.csibio.propro.service;

import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;

import java.util.List;
import java.util.Map;

public interface OverviewService extends BaseService<OverviewDO, OverviewQuery> {

    Map<String, OverviewDO> getDefaultOverviews(String projectId, List<String> expIds);
}
