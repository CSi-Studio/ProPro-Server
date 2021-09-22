package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.ExpDataVO;

public interface DataService extends BaseMultiService<DataDO, DataQuery> {

    ExpDataVO getDataFromDB(String projectId, String expId, String overviewId, String peptideRef);

    /**
     * 根据一个肽段生成其预测 兄弟(2电)肽段并且构建其EIC谱图
     * 其中预测RT时间会根据其在库中的兄弟肽段来进行推测
     *
     * @param exp                数据所属实验
     * @param libraryId          原肽段所属库
     * @param originalPeptideRef 原肽段PeptideRef
     * @param changeCharge       使用异电肽段
     * @param overviewId         原肽段检测结果对应的overviewId
     * @return
     */
    Result<ExpDataVO> predictDataFromFile(ExperimentDO exp, String libraryId, String originalPeptideRef, Boolean changeCharge, String overviewId);
}
