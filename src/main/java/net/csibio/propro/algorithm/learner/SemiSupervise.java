package net.csibio.propro.algorithm.learner;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.learner.classifier.Xgboost;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.FinalPeakGroupScore;
import net.csibio.propro.domain.bean.score.PeakGroupScores;
import net.csibio.propro.domain.bean.score.PeptideScores;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.utils.ProProUtil;
import net.csibio.propro.utils.SortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Slf4j
@Component
public class SemiSupervise {

    @Autowired
    Lda lda;
    @Autowired
    Xgboost xgboost;
    @Autowired
    Statistics statistics;
    @Autowired
    Scorer scorer;
    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    OverviewService overviewService;

    public FinalResult doSemiSupervise(String overviewId, LearningParams params) {
        FinalResult finalResult = new FinalResult();

        //Step1. 数据预处理
        log.info("数据预处理");
        OverviewDO overview = overviewService.getById(overviewId);
        if (overview == null) {
            finalResult.setErrorInfo(ResultCode.OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }
        params.setType(overview.getType());
        //Step2. 从数据库读取全部含打分结果的数据
        log.info("开始获取打分数据");
        List<PeptideScores> scores = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()), PeptideScores.class, overview.getProjectId());

        //Step3. 开始训练数据集
        HashMap<String, Double> weightsMap = new HashMap<>();
        switch (params.getClassifier()) {
            case lda -> {
                weightsMap = lda.classifier(scores, params, overview.getParams().getMethod().getScore().getScoreTypes());
                lda.score(scores, weightsMap, params.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);
            }
            case xgboost -> xgboost.classifier(scores, overview.getParams().getMethod().getScore().getScoreTypes(), params);
            default -> {
            }
        }

        List<FinalPeakGroupScore> featureScoresList = ProProUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getName(), overview.getParams().getMethod().getScore().getScoreTypes(), false);
        ErrorStat errorStat = statistics.errorStatistics(featureScoresList, params);
        finalResult.setAllInfo(errorStat);
        int count = ProProUtil.checkFdr(finalResult, params.getFdr());
        //Step4. 对于最终的打分结果和选峰结果保存到数据库中
        log.info("将合并打分及定量结果反馈更新到数据库中,总计:" + featureScoresList.size() + "条数据,开始统计相关数据");
        giveDecoyFdr(featureScoresList);
        targetDecoyDistribution(featureScoresList, overview);
        if (params.getRemoveUnmatched()) {
            log.info("统计分布完毕,开始移出无用数据");
            for (int i = featureScoresList.size() - 1; i >= 0; i--) {
                //如果fdr为空或者fdr小于指定的值,那么删除它
                if (featureScoresList.get(i).getFdr() == null || featureScoresList.get(i).getFdr() > params.getFdr()) {
                    featureScoresList.remove(i);
                }
            }
            log.info("无用数据移除完毕,开始生成最终鉴定数据");
        } else {
            log.info("不需要移出无用数据");
        }

        long start = System.currentTimeMillis();
        //插入最终的DataSum表的数据为所有的鉴定结果以及 fdr小于0.01的伪肽段
        dataSumService.buildDataSumList(featureScoresList, params.getFdr(), overviewId, overview.getProjectId());
        log.info("插入Sum数据" + featureScoresList.size() + "条一共用时：" + (System.currentTimeMillis() - start) + "毫秒");
        overview.setWeights(weightsMap);
        overviewService.update(overview);
        overviewService.statistic(overview);
        log.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    private Result check(List<PeptideScores> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (PeptideScores score : scores) {
            if (score.getDecoy()) {
                isAllReal = false;
            } else {
                isAllDecoy = false;
            }
        }
        if (isAllDecoy) {
            return Result.Error(ResultCode.ALL_SCORE_DATA_ARE_DECOY);
        }
        if (isAllReal) {
            return Result.Error(ResultCode.ALL_SCORE_DATA_ARE_REAL);
        }
        return new Result(true);
    }

    //给分布在target中的decoy赋以Fdr值, 最末尾部分的decoy忽略, fdr为null
    private void giveDecoyFdr(List<FinalPeakGroupScore> featureScoresList) {
        List<FinalPeakGroupScore> sortedAll = SortUtil.sortByMainScore(featureScoresList, false);
        FinalPeakGroupScore leftFeatureScore = null;
        FinalPeakGroupScore rightFeatureScore;
        List<FinalPeakGroupScore> decoyPartList = new ArrayList<>();
        for (FinalPeakGroupScore finalPeakGroupScore : sortedAll) {
            if (finalPeakGroupScore.getDecoy()) {
                decoyPartList.add(finalPeakGroupScore);
            } else {
                rightFeatureScore = finalPeakGroupScore;
                if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
                    for (FinalPeakGroupScore decoy : decoyPartList) {
                        if (decoy.getMainScore() - leftFeatureScore.getMainScore() < rightFeatureScore.getMainScore() - decoy.getMainScore()) {
                            decoy.setFdr(leftFeatureScore.getFdr());
                            decoy.setQValue(leftFeatureScore.getQValue());
                        } else {
                            decoy.setFdr(rightFeatureScore.getFdr());
                            decoy.setQValue(rightFeatureScore.getQValue());
                        }
                    }
                }
                leftFeatureScore = rightFeatureScore;
                decoyPartList.clear();
            }
        }
        if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
            for (FinalPeakGroupScore decoy : decoyPartList) {
                decoy.setFdr(leftFeatureScore.getFdr());
                decoy.setQValue(leftFeatureScore.getQValue());
            }
        }
    }

    /**
     * 对于FDR<=0.01,每0.001个间隔存储为一组
     * 对于FDR>0.01,每0.1间隔存储为一组
     *
     * @param featureScoresList
     * @param overviewDO
     */
    private void targetDecoyDistribution(List<FinalPeakGroupScore> featureScoresList, OverviewDO overviewDO) {
        HashMap<String, Integer> targetDistributions = ProProUtil.buildDistributionMap();
        HashMap<String, Integer> decoyDistributions = ProProUtil.buildDistributionMap();
        for (FinalPeakGroupScore sfs : featureScoresList) {
            if (sfs.getFdr() != null) {
                if (sfs.getDecoy()) {
                    ProProUtil.addOneForFdrDistributionMap(sfs.getFdr(), decoyDistributions);
                } else {
                    ProProUtil.addOneForFdrDistributionMap(sfs.getFdr(), targetDistributions);
                }
            }
        }

        overviewDO.getStatistic().put(StatConst.TARGET_DIST, targetDistributions);
        overviewDO.getStatistic().put(StatConst.DECOY_DIST, decoyDistributions);
    }

    private void cleanScore(List<PeptideScores> scoresList, List<String> scoreTypes) {
        for (PeptideScores peptideScores : scoresList) {
            if (peptideScores.getDecoy()) {
                continue;
            }
            for (PeakGroupScores peakGroupScores : peptideScores.getScoreList()) {
                int count = 0;
                if (peakGroupScores.get(ScoreType.NormRtScore, scoreTypes) != null && peakGroupScores.get(ScoreType.NormRtScore, scoreTypes) > 8) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.LogSnScore, scoreTypes) != null && peakGroupScores.get(ScoreType.LogSnScore, scoreTypes) < 3) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) != null && peakGroupScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) < 0.8) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) != null && peakGroupScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) > 0.2) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) != null && peakGroupScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) > 15) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.BseriesScore, scoreTypes) != null && peakGroupScores.get(ScoreType.BseriesScore, scoreTypes) < 1) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.YseriesScore, scoreTypes) != null && peakGroupScores.get(ScoreType.YseriesScore, scoreTypes) < 5) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) != null && peakGroupScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) < 0.6) {
                    count++;
                }
                if (peakGroupScores.get(ScoreType.XcorrShape, scoreTypes) != null && peakGroupScores.get(ScoreType.XcorrShape, scoreTypes) < 0.5) {
                    count++;
                }

                if (count > 3) {
                    peakGroupScores.setThresholdPassed(false);
                }
            }
        }
    }
}
