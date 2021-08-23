package net.csibio.propro.algorithm.learner.classifier;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.learner.TrainData;
import net.csibio.propro.domain.bean.learner.TrainPeaks;
import net.csibio.propro.domain.bean.score.FeatureScores;
import net.csibio.propro.domain.bean.score.PeptideScores;
import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
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

    public void score(List<PeptideScores> scores, HashMap<String, Double> weightsMap, List<String> scoreTypes) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        for (PeptideScores score : scores) {
            if (score.getFeatureScoresList() == null) {
                continue;
            }
            for (FeatureScores featureScores : score.getFeatureScoresList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += featureScores.get(entry.getKey(), scoreTypes) * entry.getValue();
                }
                featureScores.put(ScoreType.WeightedTotalScore.getName(), addedScore, scoreTypes);
            }
        }
    }

    public TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, LearningParams learningParams, Double cutoff) {

        List<SimpleFeatureScores> topTargetPeaks = ProProUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType, learningParams.getScoreTypes(), true);
        List<SimpleFeatureScores> topDecoyPeaks = ProProUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType, learningParams.getScoreTypes(), false);

        Double cutoffNew;
        if (topTargetPeaks.size() < 100) {
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SimpleFeatureScores scores : topDecoyPeaks) {
                if (scores.getMainScore() > decoyMax) {
                    decoyMax = scores.getMainScore();
                }
            }
            for (SimpleFeatureScores scores : topTargetPeaks) {
                if (scores.getMainScore() > targetMax) {
                    targetMax = scores.getMainScore();
                }
            }
            cutoffNew = (decoyMax + targetMax) / 2;
        } else {
            // find cutoff fdr from scores and only use best target peaks:
            cutoffNew = statistics.findCutoff(topTargetPeaks, topDecoyPeaks, learningParams, cutoff);
        }
        List<SimpleFeatureScores> bestTargetPeaks = ProProUtil.peaksFilter(topTargetPeaks, cutoffNew);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        log.info(topTargetPeaks.size() + " " + topDecoyPeaks.size() + " " + cutoffNew + " " + bestTargetPeaks.size());
        return trainPeaks;
    }
}
