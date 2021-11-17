package net.csibio.propro.algorithm.learner.classifier;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.learner.TrainData;
import net.csibio.propro.domain.bean.learner.TrainPeaks;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.utils.ProProUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nico Wang
 * Time: 2018-12-07 10:21
 */
@Component("xgboost")
public class Xgboost extends Classifier {

    public final Logger logger = LoggerFactory.getLogger(Xgboost.class);

    Map<String, Object> params = new HashMap<String, Object>() {
        {
            //original params
//            put("booster", "gbtree");
//            put("min_child_weight", 10);
//            put("eta", 0.6);
//            put("max_depth", 4);
//            put("objective", "binary:logistic");
//            put("eval_metric", "auc");
//            put("seed", "23");
            put("booster", "gbtree");
            put("min_child_weight", 10);//cv
            put("eta", 0.1);//0.01-0.2
            put("max_depth", 4);//3-10,与max_leaf_nodes互斥
//            put("silent", 0);
//            put("alpha", 1);
//            put("lambda", 0.5);// 用于逻辑回归的时候L2正则选项
            put("objective", "binary:logitraw");
            put("eval_metric", "error");
            put("seed", "23");
            put("subsample", 0.5);
        }
    };

    public void classifier(List<DataScore> scores, List<String> scoreTypes, LearningParams learningParams) {
        logger.info("开始训练Booster");
        Booster booster = learnRandomized(scores, learningParams);
        try {
            logger.info("开始最终打分");
            predictAll(booster, scores, ScoreType.InitScore.getName(), learningParams.getScoreTypes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<SelectedPeakGroup> featureScoresList = scorer.findBestPeakGroupByTargetScoreType(scores, ScoreType.WeightedTotalScore.getName(), scoreTypes, false);
        ErrorStat errorStat = statistics.errorStatistics(featureScoresList, learningParams);
        int count = ProProUtil.checkFdr(errorStat.getStatMetrics().getFdr(), learningParams.getFdr());
        if (count > 0) {
            logger.info("XGBooster:检测结果:" + count + "个.");
        }
    }

    public Booster learnRandomized(List<DataScore> scores, LearningParams learningParams) {
        try {
            //Get part of scores as train input.
            TrainData trainData = ProProUtil.split(scores, learningParams.getTrainTestRatio(), learningParams.isDebug(), learningParams.getScoreTypes());
            //第一次训练数据集使用MainScore进行训练
            long startTime = System.currentTimeMillis();
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, learningParams.getMainScore(), learningParams, learningParams.getSsInitialFdr());
            logger.info("高可信Target个数：" + trainPeaks.getBestTargets().size());
            Booster booster = train(trainPeaks, learningParams.getMainScore(), learningParams.getScoreTypes());
            predict(booster, trainData, learningParams.getMainScore(), learningParams.getScoreTypes());
            for (int times = 0; times < learningParams.getXevalNumIter(); times++) {
                logger.info("开始第" + times + "轮训练");
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getName(), learningParams, learningParams.getXgbIterationFdr());
                logger.info("高可信Target个数：" + trainPeaksTemp.getBestTargets().size());
                booster = train(trainPeaksTemp, ScoreType.WeightedTotalScore.getName(), learningParams.getScoreTypes());
                predict(booster, trainData, ScoreType.WeightedTotalScore.getName(), learningParams.getScoreTypes());
            }
            logger.info("总时间：" + (System.currentTimeMillis() - startTime));
            List<SelectedPeakGroup> featureScoresList = scorer.findBestPeakGroupByTargetScoreType(scores, ScoreType.WeightedTotalScore.getName(), learningParams.getScoreTypes(), false);
            ErrorStat errorStat = statistics.errorStatistics(featureScoresList, learningParams);
            int count = ProProUtil.checkFdr(errorStat.getStatMetrics().getFdr(), learningParams.getFdr());
            logger.info("Train count:" + count);
            return booster;
        } catch (Exception e) {
            logger.error("learnRandomizedXGB Fail.\n");
            e.printStackTrace();
            return null;
        }
    }

    public Booster train(TrainPeaks trainPeaks, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        DMatrix trainMat = trainPeaksToDMatrix(trainPeaks, skipScoreType, scoreTypes);
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);

        Booster booster = XGBoost.train(trainMat, this.params, 500, watches, null, null);
        return booster;
    }

    public void predict(Booster booster, TrainData trainData, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        List<DataScore> totalGroupScore = new ArrayList<>(trainData.getDecoys());
        totalGroupScore.addAll(trainData.getTargets());
        predictAll(booster, totalGroupScore, skipScoreType, scoreTypes);
    }

    public void predictAll(Booster booster, List<DataScore> scores, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        int scoreTypesCount = scoreTypes.size();
        if (skipScoreType.equals(ScoreType.InitScore.getName())) {
            scoreTypesCount -= 1;
        } else {
            scoreTypesCount -= 2;
        }
//        List<Float> testData = new ArrayList<>();
        for (DataScore dataScore : scores) {
            for (PeakGroup peakGroupScore : dataScore.getPeakGroupList()) {
                if (!dataScore.getDecoy() && !checkRationality(peakGroupScore, scoreTypes)) {
                    peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), 0d, scoreTypes);
                    continue;
                }
                float[] testData = new float[scoreTypesCount];
                int tempIndex = 0;
                for (String scoreName : scoreTypes) {
                    if (scoreName.equals(ScoreType.WeightedTotalScore.getName()) || scoreName.equals(ScoreType.InitScore.getName())) {
                        continue;
                    }
                    testData[tempIndex] = peakGroupScore.get(scoreName, scoreTypes).floatValue();
                    tempIndex++;
                }
                DMatrix dMatrix = new DMatrix(testData, 1, scoreTypesCount);
                float[][] predicts = booster.predict(dMatrix);
                double score = predicts[0][0];
                peakGroupScore.put(ScoreType.WeightedTotalScore.getName(), score, scoreTypes);
            }
        }
    }

    public DMatrix trainPeaksToDMatrix(TrainPeaks trainPeaks, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        int scoreTypesCount = scoreTypes.size();
        if (skipScoreType.equals(ScoreType.InitScore.getName())) {
            scoreTypesCount -= 1;
        } else {
            scoreTypesCount -= 2;
        }

        float[] trainData = new float[totalLength * scoreTypesCount];
        float[] trainLabel = new float[totalLength];
        int dataIndex = 0, labelIndex = 0;
        for (SelectedPeakGroup sample : trainPeaks.getBestTargets()) {
            for (String scoreName : scoreTypes) {
                if (scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.InitScore.getName())) {
                    continue;
                }
                trainData[dataIndex] = sample.get(scoreName, scoreTypes).floatValue();
                trainLabel[labelIndex] = 1;
                dataIndex++;
            }
            labelIndex++;
        }
        for (SelectedPeakGroup sample : trainPeaks.getTopDecoys()) {
            for (String scoreName : scoreTypes) {
                if (scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.InitScore.getName())) {
                    continue;
                }
                trainData[dataIndex] = sample.get(scoreName, scoreTypes).floatValue();
                trainLabel[labelIndex] = 0;
                dataIndex++;
            }
            labelIndex++;
        }

        DMatrix trainMat = new DMatrix(trainData, totalLength, scoreTypesCount);
        trainMat.setLabel(trainLabel);
        return trainMat;
    }

    private boolean checkRationality(PeakGroup peakGroupScore, List<String> scoreTypes) {
//        if (peakGroupScore.get(ScoreType.XcorrShape.getName(), scoreTypes) < 0.5) {
//            return false;
//        } else {
//            return true;
//        }
        return true;
    }
}
