package net.csibio.propro.algorithm.score.features;

import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.score.PeakGroupScore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:05
 */
@Component("initScorer")
public class InitScorer {

    public void calcInitScore(PeakGroupScore scores, List<String> scoreTypes) {

        Double libraryCorr = scores.get(ScoreType.LibraryCorr.getName(), scoreTypes);
        libraryCorr = (libraryCorr == null ? 0 : libraryCorr);

        Double libraryRsmd = scores.get(ScoreType.LibraryRsmd.getName(), scoreTypes);
        libraryRsmd = (libraryRsmd == null ? 0 : libraryRsmd);

//        Double normRtScore = scores.get(ScoreType.NormRtScore.getName(), scoreTypes);
//        normRtScore = (normRtScore == null ? 0 : normRtScore);

        Double isotopeCorrelationScore = scores.get(ScoreType.IsotopeCorrelationScore.getName(), scoreTypes);
        isotopeCorrelationScore = (isotopeCorrelationScore == null ? 0 : isotopeCorrelationScore);

        Double isotopeOverlapScore = scores.get(ScoreType.IsotopeOverlapScore.getName(), scoreTypes);
        isotopeOverlapScore = (isotopeOverlapScore == null ? 0 : isotopeOverlapScore);

        Double massdevScore = scores.get(ScoreType.MassdevScore.getName(), scoreTypes);
        massdevScore = (massdevScore == null ? 0 : massdevScore);

        Double xcorrCoelution = scores.get(ScoreType.XcorrCoelution.getName(), scoreTypes);
        xcorrCoelution = (xcorrCoelution == null ? 0 : xcorrCoelution);

        Double xcorrShape = scores.get(ScoreType.XcorrShape.getName(), scoreTypes);
        xcorrShape = (xcorrShape == null ? 0 : xcorrShape);

        Double xcorrShapeWeight = scores.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes);
        xcorrShapeWeight = (xcorrShapeWeight == null ? 0 : xcorrShapeWeight);

        Double ionsCountDeltaScore = scores.get(ScoreType.IonsCountDeltaScore.getName(), scoreTypes);
        ionsCountDeltaScore = (ionsCountDeltaScore == null ? 0 : ionsCountDeltaScore);

        Double logSnScore = scores.get(ScoreType.LogSnScore.getName(), scoreTypes);
        logSnScore = (logSnScore == null ? 0 : logSnScore);

//        scores.put(ScoreType.InitScore.getName(), (-5) * normRtScore + (-3) * libraryRsmd + (-5) * ionsDeltaScore + (5) * xcorrShape + (5) * isotopeCorrelationScore, scoreTypes);
        scores.put(ScoreType.InitScore.getName(),
//                (0.19011762) * libraryCorr +
//                        (-2.47298914) * libraryRsmd +
//                        (-5.63906731) * normRtScore +
                (0.62640133) * isotopeCorrelationScore +
//                        (-0.36006925) * isotopeOverlapScore +
                        (-0.08814003) * massdevScore +
                        (-0.13978311) * xcorrCoelution +
                        (1.16475032) * xcorrShape +
//                        (0.05) * ionsCountWeightScore +
                        (-0.2) * ionsCountDeltaScore +
                        (0.61712054) * logSnScore, scoreTypes);
    }

    public void calculateSwathLdaPrescore1(PeakGroupScore scores, Double temp, List<String> scoreTypes) {

        Double libraryCorr = scores.get(ScoreType.LibraryCorr.getName(), scoreTypes);
        libraryCorr = (libraryCorr == null ? 0 : libraryCorr);

        Double libraryRsmd = scores.get(ScoreType.LibraryRsmd.getName(), scoreTypes);
        libraryRsmd = (libraryRsmd == null ? 0 : libraryRsmd);

//        Double normRtScore = scores.get(ScoreType.NormRtScore.getName(), scoreTypes);
//        normRtScore = (normRtScore == null ? 0 : normRtScore);

        Double isotopeCorrelationScore = scores.get(ScoreType.IsotopeCorrelationScore.getName(), scoreTypes);
        isotopeCorrelationScore = (isotopeCorrelationScore == null ? 0 : isotopeCorrelationScore);

        Double isotopeOverlapScore = scores.get(ScoreType.IsotopeOverlapScore.getName(), scoreTypes);
        isotopeOverlapScore = (isotopeOverlapScore == null ? 0 : isotopeOverlapScore);

        Double massdevScore = scores.get(ScoreType.MassdevScore.getName(), scoreTypes);
        massdevScore = (massdevScore == null ? 0 : massdevScore);

        Double xcorrCoelution = scores.get(ScoreType.XcorrCoelution.getName(), scoreTypes);
        xcorrCoelution = (xcorrCoelution == null ? 0 : xcorrCoelution);

        Double xcorrShape = scores.get(ScoreType.XcorrShape.getName(), scoreTypes);
        xcorrShape = (xcorrShape == null ? 0 : xcorrShape);

//        Double ionsCountWeightScore = scores.get(ScoreType.IonsCountWeightScore.getName(), scoreTypes);
//        ionsCountWeightScore = (ionsCountWeightScore == null ? 0 : ionsCountWeightScore);

        Double ionsCountDeltaScore = scores.get(ScoreType.IonsCountDeltaScore.getName(), scoreTypes);
        ionsCountDeltaScore = (ionsCountDeltaScore == null ? 0 : ionsCountDeltaScore);

        Double logSnScore = scores.get(ScoreType.LogSnScore.getName(), scoreTypes);
        logSnScore = (logSnScore == null ? 0 : logSnScore);

//        scores.put(ScoreType.InitScore.getName(), (-5) * normRtScore + (-3) * libraryRsmd + (-5) * ionsDeltaScore + (5) * xcorrShape + (5) * isotopeCorrelationScore, scoreTypes);
        scores.put(ScoreType.InitScore.getName(),
                (0.19011762) * libraryCorr +
                        (-2.47298914) * libraryRsmd +
//                        (-5.63906731) * normRtScore +
                        (0.62640133) * isotopeCorrelationScore +
                        (-0.36006925) * isotopeOverlapScore +
                        (-0.08814003) * massdevScore +
                        (-0.13978311) * xcorrCoelution +
                        (1.16475032) * xcorrShape +
//  BY改造前               (0.19267813) * ionCountScore +
//  BY改造后               (0.10267813) * ionCountScore +
//                        (temp) * ionWeightMaxScore +
                        (-temp) * ionsCountDeltaScore +
                        (0.61712054) * logSnScore, scoreTypes);
    }
}
