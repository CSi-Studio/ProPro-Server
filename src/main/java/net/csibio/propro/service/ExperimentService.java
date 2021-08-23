package net.csibio.propro.service;

import net.csibio.propro.domain.bean.experiment.ExpIrt;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.ExperimentQuery;

import java.util.List;

public interface ExperimentService extends BaseService<ExperimentDO, ExperimentQuery> {

    List<ExperimentDO> getAllByProjectId(String projectId);

    List<ExpIrt> getAllIrtByProjectId(String projectId);

    void uploadAirdFile(ExperimentDO experimentDO, TaskDO taskDO);
}
