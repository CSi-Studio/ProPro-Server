package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.score.SimpleFeatureScores;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 21:34
 */

@Data
public class ErrorStat {

    List<SimpleFeatureScores> bestFeatureScoresList;

    StatMetrics statMetrics;

    Pi0Est pi0Est;

}

