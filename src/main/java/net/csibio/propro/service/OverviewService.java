package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.excel.peptide.PeptideRow;

import java.util.List;
import java.util.Map;

public interface OverviewService extends BaseService<OverviewDO, OverviewQuery> {

    OverviewDO init(RunDO run, AnalyzeParams params);

    /**
     * 获取实验的默认overview
     *
     * @param runIds
     * @return
     */
    Map<String, OverviewDO> getDefaultOverviews(List<String> runIds);

    /**
     * 将某一个实验下的所有overview的defaultOne设置为false
     *
     * @param runId
     * @return
     */
    Result resetDefaultOne(String runId);

    /**
     * 对overview进行鉴定统计
     *
     * @param overview
     * @return
     */
    Result statistic(OverviewDO overview);

    /**
     * 统计某一个项目下的所有结果矩阵
     *
     * @param projectId
     * @return
     */
    Result<List<PeptideRow>> report(String projectId);

    /**
     * 统计若干个实验的结果矩阵
     *
     * @param runIds
     * @return
     */
    Result<List<PeptideRow>> report(List<String> runIds);
}
