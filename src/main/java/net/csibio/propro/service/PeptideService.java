package net.csibio.propro.service;

import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.peptide.Protein;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface PeptideService extends BaseService<PeptideDO, PeptideQuery> {

    List<PeptideDO> getAllByLibraryId(String libraryId);

    Result updateDecoyInfos(List<PeptideDO> peptides);

    Result removeAllByLibraryId(String libraryId);

    /**
     * 获取某一个标准库中所有的Transition的RT的取值范围
     *
     * @param libraryId
     * @return
     */
    Double[] getRTRange(String libraryId);

    Result<List<Protein>> getProteinList(PeptideQuery query);

    /**
     * 计算不同蛋白质的数目
     *
     * @param libraryId
     * @return
     */
    Long countByProteinName(String libraryId);

    List<PeptideCoord> buildCoord4Irt(String libraryId, WindowRange mzRange);

    /**
     * 根据分析参数动态构建符合条件的目标肽段
     *
     * @param libraryId 指定库Id
     * @param mzRange   窗口范围
     * @param rtWindow  创建坐标的RT窗口
     * @param si        斜率截距
     * @return
     */
    List<PeptideCoord> buildCoord(String libraryId, WindowRange mzRange, Double rtWindow, SlopeIntercept si);

    /**
     * 根据PeptideRef生成一个全新的PeptideDO
     * 可以指定生成的a,b,c,x,y,z碎片类型 ionTypes
     * 可以指定生成的碎片的带电量种类 chargeTypes
     * 注意:生成的靶向肽段是没有预测rt和预测intensity的
     *
     * @param peptideRef
     * @return
     */
    PeptideDO buildWithPeptideRef(String peptideRef, int minLength, List<String> ionTypes, List<Integer> chargeTypes);
}

