package net.csibio.propro.algorithm.learner.classifier;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.learner.TrainData;
import net.csibio.propro.domain.bean.learner.TrainPeaks;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.utils.ProProUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类器,目前有LDA和XGBoost两种
 *
 * @author lumiaoshan
 */
@Slf4j
public abstract class Classifier {

    @Autowired
    Statistics statistics;

    @Autowired
    Scorer scorer;

    /**
     * Get clfScore with given confidence(params).
     * 根据weightsMap计算子分数的(加权总分-平均加权总分)
     */
    public void score(TrainData data, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        score(data.getTargets(), weightsMap, scoreTypes);
        score(data.getDecoys(), weightsMap, scoreTypes);
    }

    public void score(List<DataScore> scores, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        for (DataScore score : scores) {
            if (score.getPeakGroupList() == null) {
                continue;
            }
            for (PeakGroup peakGroupScore : score.getPeakGroupList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += peakGroupScore.get(entry.getKey(), scoreTypes) * entry.getValue();
                }
                peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), addedScore, scoreTypes);
            }
        }
    }

    public void scoreForPeakGroups(List<PeakGroup> scoreList, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        if (scoreList == null || scoreList.size() == 0) {
            return;
        }
        for (PeakGroup peakGroupScore : scoreList) {
            double addedScore = 0;
            for (Map.Entry<String, Double> entry : entries) {
                addedScore += peakGroupScore.get(entry.getKey(), scoreTypes) * entry.getValue();
            }
            peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), addedScore, scoreTypes);
        }
    }

    public TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, LearningParams learningParams, Double cutoff) {

        List<SelectedPeakGroup> topTargetPeaks = scorer.findBestPeakGroupByTargetScoreType(trainData.getTargets(), usedScoreType, learningParams.getScoreTypes(), true);
        List<SelectedPeakGroup> topDecoyPeaks = scorer.findBestPeakGroupByTargetScoreType(trainData.getDecoys(), usedScoreType, learningParams.getScoreTypes(), false);

        Double cutoffNew;
        if (topTargetPeaks.size() < 100) {
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SelectedPeakGroup scores : topDecoyPeaks) {
                if (scores.getMainScore() > decoyMax) {
                    decoyMax = scores.getMainScore();
                }
            }
            for (SelectedPeakGroup scores : topTargetPeaks) {
                if (scores.getMainScore() > targetMax) {
                    targetMax = scores.getMainScore();
                }
            }
            cutoffNew = (decoyMax + targetMax) / 2;
        } else {
            // find cutoff fdr from scores and only use best target peaks:
            cutoffNew = statistics.findCutoff(topTargetPeaks, topDecoyPeaks, learningParams, cutoff);
        }
        List<SelectedPeakGroup> bestTargetPeaks = ProProUtil.peaksFilter(topTargetPeaks, cutoffNew);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        log.info(topTargetPeaks.size() + " " + topDecoyPeaks.size() + " " + cutoffNew + " " + bestTargetPeaks.size());
        return trainPeaks;
    }
}
