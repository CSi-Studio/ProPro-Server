package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;

import java.util.List;
import java.util.Map;

public interface OverviewService extends BaseService<OverviewDO, OverviewQuery> {

    /**
     * 获取实验的默认overview
     *
     * @param expIds
     * @return
     */
    Map<String, OverviewDO> getDefaultOverviews(List<String> expIds);

    /**
     * 将某一个实验下的所有overview的defaultOne设置为false
     *
     * @param expId
     * @return
     */
    Result resetDefaultOne(String expId);
}
