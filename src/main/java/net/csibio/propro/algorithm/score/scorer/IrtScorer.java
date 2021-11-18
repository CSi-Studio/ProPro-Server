package net.csibio.propro.algorithm.score.scorer;

import net.csibio.propro.algorithm.peak.PeakPicker;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.features.DIAScorer;
import net.csibio.propro.algorithm.score.features.LibraryScorer;
import net.csibio.propro.algorithm.score.features.XicScorer;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupListWrapper;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component("irtScorer")
public class IrtScorer {

    @Autowired
    XicScorer xicScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    PeptideService peptideService;
    @Autowired
    PeakPicker peakPicker;

    /**
     * return scores.library_corr                     * -0.34664267 +
     * scores.library_norm_manhattan           *  2.98700722 +
     * scores.norm_rt_score                    *  7.05496384 +
     * scores.xcorr_coelution_score            *  0.09445371 +
     * scores.xcorr_shape_score                * -5.71823862 +
     * scores.log_sn_score                     * -0.72989582 +
     * scores.elution_model_fit_score          *  1.88443209;
     *
     * @param data features extracted from chromatogramList in transitionGroup
     * @return List of overallQuality
     */
    public DataDO score(DataDO data, AnalyzeParams params) {

        int maxIonsCount = Arrays.stream(data.getIonsLow()).max().getAsInt();
        PeptideCoord coord = peptideService.getOne(new PeptideQuery(params.getInsLibId(), data.getPeptideRef()), PeptideCoord.class);
        PeakGroupListWrapper peakGroupListWrapper = peakPicker.searchByIonsCount(data, coord, params.getMethod().getIrt().getSs());
        if (!peakGroupListWrapper.isFound()) {
            return null;
        }
        List<PeakGroup> peakGroupList = peakGroupListWrapper.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormIntMap();

        List<String> scoreTypes4Irt = ScoreType.usedScoreTypes();

        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(scoreTypes4Irt.size());
            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes4Irt);
            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, scoreTypes4Irt);
            peakGroup.put(ScoreType.IonsDelta.getName(), (maxIonsCount - peakGroup.getIonsLow()) * 1d / maxIonsCount, scoreTypes4Irt);
            double deltaWeight = (maxIonsCount - peakGroup.getIonsLow()) * 1d / maxIonsCount;
            peakGroup.put(ScoreType.IonsDelta, deltaWeight, scoreTypes4Irt);
            peakGroup.put(ScoreType.TotalScore, totalScore(peakGroup, scoreTypes4Irt), scoreTypes4Irt);
        }
        data.setPeakGroupList(peakGroupList);
        return data;
    }

    /**
     * The scoreForAll that is really matter to final pairs selection.
     *
     * @param scores pre-calculated
     * @return final scoreForAll
     */
    private double totalScore(PeakGroup scores, List<String> scoreTypes) {
//        return scores.get(ScoreType.LibraryCorr.getName(), scoreTypes) * -0.34664267d +
//                scores.get(ScoreType.LibraryRsmd.getName(), scoreTypes) * 2.98700722d +
//             scores.get(ScoreType.LogSnScore.getName(), scoreTypes) * -0.72989582 +
//        return scores.get(ScoreType.XcorrCoelution.getName(), scoreTypes) * (-0.09445371d) +
//                scores.get(ScoreType.XcorrShape.getName(), scoreTypes) * -5.71823862d;
        return scores.get(ScoreType.XcorrShape.getName(), scoreTypes) +
                scores.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes) +
                scores.get(ScoreType.LibraryDotprod.getName(), scoreTypes) +
                scores.get(ScoreType.LibraryCorr.getName(), scoreTypes) -
                scores.get(ScoreType.IonsDelta.getName(), scoreTypes) -
                scores.get(ScoreType.XcorrCoelutionWeighted.getName(), scoreTypes)

                ;
    }

}
