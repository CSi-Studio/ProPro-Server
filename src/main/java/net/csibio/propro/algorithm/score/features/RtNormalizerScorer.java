package net.csibio.propro.algorithm.score.features;

import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupScore;
import net.csibio.propro.domain.bean.score.ScoreRtPair;
import net.csibio.propro.domain.options.AnalyzeParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component("rtNormalizerScorer")
public class RtNormalizerScorer {

    @Autowired
    XicScorer xicScorer;
    @Autowired
    LibraryScorer libraryScorer;


//    private float windowLength = 1000;
//    private int binCount = 30;

//    private float rtNormalizationFactor = 1.0f;
//    private int addUpSpectra = 1;
//    private float spacingForSpectraResampling = 0.005f;
//    //scoreForAll use params


    /**
     * return scores.library_corr                     * -0.34664267 +
     * scores.library_norm_manhattan           *  2.98700722 +
     * scores.norm_rt_score                    *  7.05496384 +
     * scores.xcorr_coelution_score            *  0.09445371 +
     * scores.xcorr_shape_score                * -5.71823862 +
     * scores.log_sn_score                     * -0.72989582 +
     * scores.elution_model_fit_score          *  1.88443209;
     *
     * @param peakGroupFeatureList features extracted from chromatogramList in transitionGroup
     * @param normedLibIntMap      intensity in transitionList in transitionGroup
     * @return List of overallQuality
     */
    public List<ScoreRtPair> score(List<PeakGroup> peakGroupFeatureList, HashMap<String, Double> normedLibIntMap, double groupRt, AnalyzeParams params) {

        List<ScoreRtPair> finalScores = new ArrayList<>();
        List<String> defaultScoreTypes = params.getMethod().getScore().getScoreTypes();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            if (peakGroupFeature.getBestRightRt() - peakGroupFeature.getBestLeftRt() < 15d) {
                continue;
            }
            PeakGroupScore scores = new PeakGroupScore(defaultScoreTypes.size());
            xicScorer.calcXICScores(peakGroupFeature, normedLibIntMap, scores, defaultScoreTypes);
//            xicScorer.calculateLogSnScore(peakGroupFeature, scores, defaultScoreTypes);
            libraryScorer.calculateLibraryScores(peakGroupFeature, normedLibIntMap, scores, defaultScoreTypes);

            double ldaScore = -1d * calculateLdaPrescore(scores, defaultScoreTypes);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setLibRt(groupRt);
            scoreRtPair.setRealRt(peakGroupFeature.getApexRt());
            scoreRtPair.setScore(ldaScore);
            scoreRtPair.setScores(scores);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }

    /**
     * The scoreForAll that is really matter to final pairs selection.
     *
     * @param scores pre-calculated
     * @return final scoreForAll
     */
    private double calculateLdaPrescore(PeakGroupScore scores, List<String> scoreTypes) {
//        return scores.get(ScoreType.LibraryCorr.getName(), scoreTypes) * -0.34664267d +
//                scores.get(ScoreType.LibraryRsmd.getName(), scoreTypes) * 2.98700722d +
        return scores.get(ScoreType.XcorrCoelution.getName(), scoreTypes) * 0.09445371d +
                scores.get(ScoreType.XcorrShape.getName(), scoreTypes) * -5.71823862d;
//                scores.get(ScoreType.LogSnScore.getName(), scoreTypes) * -0.72989582d;
    }

}
