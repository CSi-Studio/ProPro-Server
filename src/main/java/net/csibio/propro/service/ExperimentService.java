package net.csibio.propro.service;

import net.csibio.propro.domain.bean.common.FloatPairs;
import net.csibio.propro.domain.bean.experiment.ExpIrt;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.ExperimentQuery;

import java.util.List;

public interface ExperimentService extends BaseService<ExperimentDO, ExperimentQuery> {

    List<ExperimentDO> getAllByProjectId(String projectId);

    List<ExpIrt> getAllIrtByProjectId(String projectId);

    void uploadAirdFile(ExperimentDO experimentDO, TaskDO taskDO);

    /**
     * 根据mz确认母体窗口再查询光谱图
     *
     * @param exp
     * @param mz
     * @param rt
     * @return
     */
    FloatPairs getSpectrum(ExperimentDO exp, Double mz, Float rt);

    /**
     * 已知母体窗口查询光谱图
     *
     * @param exp
     * @param blockIndex
     * @param rt
     * @return
     */
    FloatPairs getSpectrum(ExperimentDO exp, BlockIndexDO blockIndex, Float rt);
}
