package net.csibio.propro.algorithm.learner.classifier;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.learner.TrainData;
import net.csibio.propro.domain.bean.learner.TrainPeaks;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.utils.ProProUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

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
                peakGroupScore.setTotalScore(addedScore);
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
            peakGroupScore.setTotalScore(addedScore);
        }
    }

    /**
     * 选择第一批初始数据集
     *
     * @param trainData
     * @param learningParams
     * @return
     */
    public TrainPeaks selectFirstTrainPeaks(TrainData trainData, LearningParams learningParams) {
        List<SelectedPeakGroup> decoyPeaks = new ArrayList<>();
        List<String> scoreTypes = learningParams.getScoreTypes();
        for (DataScore dataScore : trainData.getDecoys()) {
            PeakGroup topDecoy = null;
            double maxMainScore = -Double.MAX_VALUE;
            for (PeakGroup peakGroupScore : dataScore.getPeakGroupList()) {
                double mainScore = peakGroupScore.get(ScoreType.InitScore.getName(), scoreTypes);
                if (mainScore > maxMainScore) {
                    maxMainScore = mainScore;
                    topDecoy = peakGroupScore;
                }
            }

            if (topDecoy == null || topDecoy.getScores() == null) {
                log.error("Scores为空");
                continue;
            }

            SelectedPeakGroup selectedPeakGroup = new SelectedPeakGroup();
            selectedPeakGroup.setDecoy(dataScore.getDecoy());
            selectedPeakGroup.setScores(topDecoy.getScores());
            decoyPeaks.add(selectedPeakGroup);
        }
        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setTopDecoys(decoyPeaks);

        SelectedPeakGroup bestTargetScore = new SelectedPeakGroup(learningParams.getScoreTypes().size());
        bestTargetScore.setDecoy(false);
        bestTargetScore.put(ScoreType.InitScore.getName(), 3d, scoreTypes);
        bestTargetScore.put(ScoreType.CorrShape.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.CorrShapeW.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.CorrCoe.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.CorrCoeW.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.Pearson.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.ApexPearson.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.IntShift.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.Rsmd.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.Manhattan.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.Dotprod.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.Sangle.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LogSn.getName(), 5d, scoreTypes);
        bestTargetScore.put(ScoreType.NormRt.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.IsoCorr.getName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.IsoOverlap.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.MassDev.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.MassDevW.getName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.IonsDelta.getName(), 0d, scoreTypes);

        List<SelectedPeakGroup> bestTargets = new ArrayList<>();
        bestTargets.add(bestTargetScore);
        trainPeaks.setBestTargets(bestTargets);
        return trainPeaks;
    }

    public TrainPeaks selectTrainPeaks(TrainData trainData, LearningParams learningParams, Double cutoff) {

        List<SelectedPeakGroup> topTargetPeaks = scorer.findBestPeakGroup(trainData.getTargets());
        List<SelectedPeakGroup> topDecoyPeaks = scorer.findBestPeakGroup(trainData.getDecoys());

        Double cutoffNew;
        if (topTargetPeaks.size() < 100) {
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SelectedPeakGroup scores : topDecoyPeaks) {
                if (scores.getTotalScore() > decoyMax) {
                    decoyMax = scores.getTotalScore();
                }
            }
            for (SelectedPeakGroup scores : topTargetPeaks) {
                if (scores.getTotalScore() > targetMax) {
                    targetMax = scores.getTotalScore();
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
