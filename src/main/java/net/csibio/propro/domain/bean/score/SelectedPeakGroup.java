package net.csibio.propro.domain.bean.score;

import lombok.Data;
import net.csibio.propro.domain.bean.data.DataScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class SelectedPeakGroup extends BaseScores {

    String id;

    List<String> proteins;

    String peptideRef;

    Boolean decoy;

    Double libRt;

    Double irt;

    //本峰值对应的最佳RT时间
    Double apexRt;

    Double selectedRt;

    //定量结果
    Double intensitySum;
    //定量修正过以后的sum
    Double fitIntSum;

    //本峰对应的最终综合打分
    Double totalScore;

    Double pValue;

    Double qValue;

    Integer ionsLow;

    Double fdr;

    public SelectedPeakGroup() {
    }

    public SelectedPeakGroup(int scoreTypesSize) {
        this.scores = new Double[scoreTypesSize];
    }

    public SelectedPeakGroup(DataScore dataScore) {
        this.id = dataScore.getId();
        this.irt = dataScore.getIrt();
        this.libRt = dataScore.getLibRt();
        this.proteins = dataScore.getProteins();
        this.peptideRef = dataScore.getPeptideRef();
        this.decoy = dataScore.getDecoy();
    }
}
