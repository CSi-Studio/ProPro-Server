package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.score.FinalPeakGroupScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 21:34
 */

@Data
public class ErrorStat {

    List<FinalPeakGroupScore> bestFeatureScoresList;

    StatMetrics statMetrics;

    Pi0Est pi0Est;

}

