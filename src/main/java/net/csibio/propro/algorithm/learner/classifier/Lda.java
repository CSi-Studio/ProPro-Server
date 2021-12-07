package net.csibio.propro.algorithm.learner.classifier;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.learner.*;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.utils.ProProUtil;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("lda")
public class Lda extends Classifier {

    /**
     * @param peptideList
     * @param learningParams
     * @return
     */
    public HashMap<String, Double> classifier(List<DataScore> peptideList, LearningParams learningParams) {
        log.info("开始训练学习数据权重");
        if (peptideList.size() < 500) {
            learningParams.setXevalNumIter(10);
            learningParams.setSsIterationFdr(0.02);
            learningParams.setProgressiveRate(0.8);
        }

        LDALearnData ldaLearnData = learnRandomized(peptideList, learningParams);
        if (ldaLearnData == null || ldaLearnData.getWeightsMap() == null) {
            log.info("本轮训练失败");
            return null;
        }
        score(peptideList, ldaLearnData.getWeightsMap(), learningParams.getScoreTypes());
        List<SelectedPeakGroup> selectedPeakGroups = scorer.findBestPeakGroup(peptideList);
        int count = 0;
        ErrorStat errorStat = statistics.errorStatistics(selectedPeakGroups, learningParams);
        count = ProProUtil.checkFdr(errorStat.getStatMetrics().getFdr(), learningParams.getFdr());
        if (count > 0) {
            log.info("本轮尝试有效果:检测结果:" + count + "个");
        }

        return ldaLearnData.getWeightsMap();
    }

    public LDALearnData learnRandomized(List<DataScore> scores, LearningParams learningParams) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {
            TrainData trainData = ProProUtil.split(scores, learningParams.getTrainTestRatio());
            TrainPeaks trainPeaks = selectFirstTrainPeaks(trainData, learningParams);

            HashMap<String, Double> weightsMap = learn(trainPeaks, learningParams.getScoreTypes());
            log.info("Train Weight:" + JSONArray.toJSONString(weightsMap));

            //根据weightsMap计算子分数的加权总分
            score(trainData, weightsMap, learningParams.getScoreTypes());
            weightsMap = new HashMap<>();
            HashMap<String, Double> lastWeightsMap = new HashMap<>();
            for (int times = 0; times < learningParams.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, learningParams, learningParams.getSsIterationFdr());
                lastWeightsMap = weightsMap;
                weightsMap = learn(trainPeaksTemp, learningParams.getScoreTypes());
                log.info("Train Weight:" + JSONArray.toJSONString(weightsMap));
                for (Double value : weightsMap.values()) {
                    if (value == null || Double.isNaN(value)) {
                        log.info("本轮训练一坨屎:" + JSON.toJSONString(weightsMap));
                        continue;
                    }
                }
                if (lastWeightsMap.size() != 0) {
                    for (String key : weightsMap.keySet()) {
                        weightsMap.put(key, weightsMap.get(key) * learningParams.getProgressiveRate() + lastWeightsMap.get(key) * (1d - learningParams.getProgressiveRate()));
                    }
                }
                score(trainData, weightsMap, learningParams.getScoreTypes());
            }
            ldaLearnData.setWeightsMap(weightsMap);
            return ldaLearnData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 使用apache的svd库进行计算
     *
     * @param trainPeaks
     * @return key为子分数的名称, value是该子分数的权重值
     */
    public HashMap<String, Double> learn(TrainPeaks trainPeaks, List<String> scoreTypes) {

        int row = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        if (row == 0) {
            log.error("训练数据集为空");
        }
        int column = scoreTypes.size();

        //先将需要进入学习的打分转化为二维矩阵
        RealMatrix scoresMatrix = MatrixUtils.createRealMatrix(row, column);
        RealVector labelVector = new ArrayRealVector(row);
        List<SelectedPeakGroup> totalPeakGroups = new ArrayList<>();
        totalPeakGroups.addAll(trainPeaks.getBestTargets());
        totalPeakGroups.addAll(trainPeaks.getTopDecoys());
        assignment(scoresMatrix, labelVector, totalPeakGroups, scoreTypes);
        //计算SVD的解
        SingularValueDecomposition solver = new SingularValueDecomposition(scoresMatrix);
        RealVector realVector = solver.getSolver().solve(labelVector);

        //输出最终的权重值
        HashMap<String, Double> weightsMap = new HashMap<>();
        int tempJ = 0;
        for (String key : scoreTypes) {
            weightsMap.put(key, realVector.getEntry(tempJ));
            tempJ++;
        }

        for (Double value : weightsMap.values()) {
            if (value == null || Double.isNaN(value)) {
                log.info("本轮训练结果很差:" + JSON.toJSONString(weightsMap));
                return null;
            }
        }
        return weightsMap;
    }

    private void assignment(RealMatrix xMatrix, RealVector yVector, List<SelectedPeakGroup> peakGroupList, List<String> scoreTypes) {

        Set<String> skipTypes = new HashSet<>();
        skipTypes.add(ScoreType.CorrShape.getName());
        skipTypes.add(ScoreType.CorrShapeW.getName());
        for (int i = 0; i < peakGroupList.size(); i++) {
            yVector.setEntry(i, peakGroupList.get(i).getDecoy() ? 0 : 1);
            try {
                for (int j = 0; j < scoreTypes.size(); j++) {
                    if (skipTypes.contains(scoreTypes.get(j))) {
                        xMatrix.setEntry(i, j, 0);
                    } else {
                        xMatrix.setEntry(i, j, peakGroupList.get(i).get(scoreTypes.get(j), scoreTypes));
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }


    }
}
