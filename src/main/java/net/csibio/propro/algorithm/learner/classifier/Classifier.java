package net.csibio.propro.algorithm.learner.classifier;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.data.PeptideScore;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.learner.TrainData;
import net.csibio.propro.domain.bean.learner.TrainPeaks;
import net.csibio.propro.domain.bean.score.PeakGroupScore;
import net.csibio.propro.domain.bean.score.SelectedPeakGroupScore;
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
    public Statistics statistics;

    /**
     * Get clfScore with given confidence(params).
     * 根据weightsMap计算子分数的(加权总分-平均加权总分)
     */
    public void score(TrainData data, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        score(data.getTargets(), weightsMap, scoreTypes);
        score(data.getDecoys(), weightsMap, scoreTypes);
    }

    public void score(List<PeptideScore> scores, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        for (PeptideScore score : scores) {
            if (score.getScoreList() == null) {
                continue;
            }
            for (PeakGroupScore peakGroupScore : score.getScoreList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += peakGroupScore.get(entry.getKey(), scoreTypes) * entry.getValue();
                }
                peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), addedScore, scoreTypes);
            }
        }
    }

    public void scoreForPeakGroups(List<PeakGroupScore> scoreList, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        if (scoreList == null || scoreList.size() == 0) {
            return;
        }
        for (PeakGroupScore peakGroupScore : scoreList) {
            double addedScore = 0;
            for (Map.Entry<String, Double> entry : entries) {
                addedScore += peakGroupScore.get(entry.getKey(), scoreTypes) * entry.getValue();
            }
            peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), addedScore, scoreTypes);
        }
    }

    public TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, LearningParams learningParams, Double cutoff) {

        List<SelectedPeakGroupScore> topTargetPeaks = ProProUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType, learningParams.getScoreTypes(), true);
        List<SelectedPeakGroupScore> topDecoyPeaks = ProProUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType, learningParams.getScoreTypes(), false);

        Double cutoffNew;
        if (topTargetPeaks.size() < 100) {
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SelectedPeakGroupScore scores : topDecoyPeaks) {
                if (scores.getMainScore() > decoyMax) {
                    decoyMax = scores.getMainScore();
                }
            }
            for (SelectedPeakGroupScore scores : topTargetPeaks) {
                if (scores.getMainScore() > targetMax) {
                    targetMax = scores.getMainScore();
                }
            }
            cutoffNew = (decoyMax + targetMax) / 2;
        } else {
            // find cutoff fdr from scores and only use best target peaks:
            cutoffNew = statistics.findCutoff(topTargetPeaks, topDecoyPeaks, learningParams, cutoff);
        }
        List<SelectedPeakGroupScore> bestTargetPeaks = ProProUtil.peaksFilter(topTargetPeaks, cutoffNew);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        log.info(topTargetPeaks.size() + " " + topDecoyPeaks.size() + " " + cutoffNew + " " + bestTargetPeaks.size());
        return trainPeaks;
    }
}
