package net.csibio.propro.domain.bean.score;

import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class SelectedPeakGroupScore extends BaseScores {

    String id;

    List<String> proteins;

    String peptideRef;

    Boolean decoy;

    //本峰值对应的最佳RT时间
    Double rt;

    //定量结果
    Double intensitySum;

    //本峰对应的最终综合打分
    Double mainScore;

    Double pValue;

    Double qValue;

    Double fdr;

    String fragIntFeature;

    public SelectedPeakGroupScore() {
    }

    public SelectedPeakGroupScore(int scoreTypesSize) {
        this.scores = new Double[scoreTypesSize];
    }

    public SelectedPeakGroupScore(String id, List<String> proteins, String peptideRef, Boolean decoy) {
        this.id = id;
        this.proteins = proteins;
        this.peptideRef = peptideRef;
        this.decoy = decoy;
    }
}
