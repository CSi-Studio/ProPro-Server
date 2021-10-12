package net.csibio.propro.domain.bean.score;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-07 23:39
 */
@Data
public class ScoreRtPair {
    double libRt;
    double score;
    double realRt;
    PeakGroupScore scores;
}
