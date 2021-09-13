package net.csibio.propro.service;

import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.ExpDataVO;

import java.util.List;

public interface DataService extends BaseMultiService<DataDO, DataQuery> {

    ExpDataVO getData(String projectId, String expId, String overviewId, String peptideRef);

    /**
     * 根据一个未知的肽段构建其EIC谱图
     * 其中预测RT时间会根据其在库中的兄弟肽段来进行推测
     *
     * @param expIds
     * @param libraryId
     * @param brotherPeptideRef,
     * @param newCharge
     * @return
     */
    List<ExpDataVO> buildData(List<String> expIds, String libraryId, String brotherPeptideRef, int newCharge);

    ExpDataVO buildData(ExperimentDO exp, String libraryId, String originalPeptideRef);
}
