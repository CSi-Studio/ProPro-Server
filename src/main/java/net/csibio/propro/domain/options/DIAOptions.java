package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.domain.bean.score.SlopeIntercept;

@Data
public class DIAOptions {

    //iRT的斜率截距
    SlopeIntercept si;

    //rt窗口
    Double rtWindow;

    //mz窗口
    Double mzWindow;

    //是否使用自适应mz窗口
    Boolean adaptiveMzWindow = false;

    /**
     * shape的筛选阈值,一般建议在0.6左右
     */
    Float minShapeScore;
    /**
     * shape的筛选阈值,一般建议在0.8左右
     */
    Float minShapeWeightScore;

    /**
     * 筛选的FDR值,默认值为0.01
     */
    Float fdr;

}
