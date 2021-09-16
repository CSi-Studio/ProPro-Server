package net.csibio.propro.domain.bean.score;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;

import java.util.List;

@Data
public class BaseScores {

    /**
     * return scores.library_corr                     * -0.34664267 + 2
     * scores.library_norm_manhattan           *  2.98700722 + 2
     * scores.norm_rt_score                    *  7.05496384 + //0
     * scores.xcorr_coelution_score            *  0.09445371 + 1
     * scores.xcorr_shape_score                * -5.71823862 + 1
     * scores.log_sn_score                     * -0.72989582 + 1
     * scores.elution_model_fit_score          *  1.88443209; //0
     * <p>
     * <p>
     * <p>
     * scores.xcorr_coelution_score 互相关偏移的mean + std
     * scores.weighted_coelution_score 带权重的相关偏移sum
     * scores.xcorr_shape_score 互相关序列最大值的平均值
     * scores.weighted_xcorr_shape 带权重的互相关序列最大值的平均值
     * scores.log_sn_score log(距离ApexRt最近点的stn值之和)
     * <p>
     * scores.var_intensity_score 同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
     * <p>
     * scores.library_corr //对experiment和library intensity算Pearson相关系数
     * scores.library_norm_manhattan //对experiment intensity 算平均占比差距
     * <p>
     * scores.massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
     * scores.weighted_massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
     */
    Double[] scores;

    public void put(String typeName, Double score, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(typeName);
        if (index != -1) {
            scores[index] = score;
        }
    }

    public void put(ScoreType scoreType, Double score, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(scoreType.getName());
        if (index != -1) {
            scores[index] = score;
        }
    }

    public Double get(ScoreType scoreType, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(scoreType.getName());
        if (scores == null || index == -1) {
            return null;
        } else {
            Double d = scores[index];
            return d == null ? 0d : d;
        }
    }

    public Double get(String typeName, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(typeName);
        if (scores == null || index == -1) {
            return null;
        } else {
            Double d = scores[index];
            return d == null ? 0d : d;
        }
    }

    //所谓的Remove是不会真正的Remove掉相关的分数,只是将对应的分数置为null
    public void remove(String typeName, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(typeName);
        if (scores != null && index != -1) {
            scores[index] = null;
        }
    }

    public void remove(ScoreType scoreType, List<String> scoreTypes) {
        int index = scoreTypes.indexOf(scoreType.getName());
        if (scores != null && index != -1) {
            scores[index] = null;
        }
    }

//    public void totalScore
}
