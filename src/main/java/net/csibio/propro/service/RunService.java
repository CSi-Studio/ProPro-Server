package net.csibio.propro.service;

import net.csibio.propro.domain.bean.common.FloatPairs;
import net.csibio.propro.domain.bean.run.RunIrt;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.RunQuery;

import java.util.List;

public interface RunService extends BaseService<RunDO, RunQuery> {

    List<RunDO> getAllByProjectId(String projectId);

    List<RunIrt> getAllIrtByProjectId(String projectId);

    void uploadAirdFile(RunDO runDO, TaskDO taskDO);

    /**
     * 根据mz确认母体窗口再查询光谱图
     *
     * @param run
     * @param mz
     * @param rt
     * @return
     */
    FloatPairs getSpectrum(RunDO run, Double mz, Float rt);

    /**
     * 已知母体窗口查询光谱图
     *
     * @param run
     * @param blockIndex
     * @param rt
     * @return
     */
    FloatPairs getSpectrum(RunDO run, BlockIndexDO blockIndex, Float rt);
}
