package net.csibio.propro.algorithm.learner;

import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.learner.classifier.Xgboost;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.FeatureScores;
import net.csibio.propro.domain.bean.score.PeptideScores;
import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.utils.ProProUtil;
import net.csibio.propro.utils.SortUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Component
public class SemiSupervise {

    public final Logger logger = LoggerFactory.getLogger(SemiSupervise.class);

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
    OverviewService overviewService;

    public FinalResult doSemiSupervise(String overviewId, LearningParams params) {
        FinalResult finalResult = new FinalResult();

        //Step1. 数据预处理
        logger.info("数据预处理");
        OverviewDO overview = overviewService.getById(overviewId);
        if (overview == null) {
            finalResult.setErrorInfo(ResultCode.OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }

        //Step2. 从数据库读取全部打分数据
        logger.info("开始获取打分数据");

        List<PeptideScores> scores = dataService.getAll(new DataQuery().setOverviewId(overviewId), PeptideScores.class, overview.getProjectId());
//        if (learningParams.getType().equals(ExpTypeConst.PRM)) {
//            cleanScore(scores, overview.getParams().getMethod().getScore().getScoreTypes());
//        }

        //Step3. 开始训练数据集
        HashMap<String, Double> weightsMap = new HashMap<>();
        switch (params.getClassifier()) {
            case lda:
                weightsMap = lda.classifier(scores, params, overview.getParams().getMethod().getScore().getScoreTypes());
                lda.score(scores, weightsMap, params.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);
                break;

            case xgboost:
                xgboost.classifier(scores, overview.getParams().getMethod().getScore().getScoreTypes(), params);
                break;

            default:
                break;
        }

        List<SimpleFeatureScores> featureScoresList = ProProUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getName(), overview.getParams().getMethod().getScore().getScoreTypes(), false);
        int count = 0;
//        if (params.getType().equals(ExpTypeConst.PRM)) {
//            double maxDecoy = Double.MIN_VALUE;
//            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
//                if (simpleFeatureScores.getDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
//                    maxDecoy = simpleFeatureScores.getMainScore();
//                }
//            }
//            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
//                if (!simpleFeatureScores.getDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
//                    simpleFeatureScores.setFdr(0d);
//                    count++;
//                } else {
//                    simpleFeatureScores.setFdr(1d);
//                }
//            }
//
//        } else {
        ErrorStat errorStat = statistics.errorStatistics(featureScoresList, params);
        finalResult.setAllInfo(errorStat);
        count = ProProUtil.checkFdr(finalResult, params.getFdr());
//        }

        //Step4. 对于最终的打分结果和选峰结果保存到数据库中
        logger.info("将合并打分及定量结果反馈更新到数据库中,总计:" + featureScoresList.size() + "条数据");
        giveDecoyFdr(featureScoresList);
        targetDecoyDistribution(featureScoresList, overview);
        dataService.removeUnuseData(overviewId, featureScoresList, params.getFdr(), overview.getProjectId());
        long start = System.currentTimeMillis();
        dataService.batchUpdate(overview.getId(), featureScoresList, overview.getProjectId());
        logger.info("更新数据" + featureScoresList.size() + "条一共用时：" + (System.currentTimeMillis() - start) + "毫秒");

        logger.info("最终鉴定肽段数目为:" + count + ",打分反馈更新完毕");
        int matchedProteinsCount = dataService.countIdentifiedProteins(overviewId, overview.getProjectId());
        logger.info("最终鉴定蛋白数目(包含非Unique)为:" + matchedProteinsCount);
        finalResult.setMatchedPeptideCount(count);
        finalResult.setMatchedProteinCount(matchedProteinsCount);

        overview.setWeights(weightsMap);
        overview.getStatistic().put(StatConst.MATCHED_PEPTIDE_COUNT, count);
        overview.getStatistic().put(StatConst.MATCHED_PROTEIN_COUNT, matchedProteinsCount);
        overviewService.update(overview);

        logger.info("合并打分完成,共找到新肽段" + count + "个");
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
    private void giveDecoyFdr(List<SimpleFeatureScores> featureScoresList) {
        List<SimpleFeatureScores> sortedAll = SortUtil.sortByMainScore(featureScoresList, false);
        SimpleFeatureScores leftFeatureScore = null;
        SimpleFeatureScores rightFeatureScore;
        List<SimpleFeatureScores> decoyPartList = new ArrayList<>();
        for (SimpleFeatureScores simpleFeatureScores : sortedAll) {
            if (simpleFeatureScores.getDecoy()) {
                decoyPartList.add(simpleFeatureScores);
            } else {
                rightFeatureScore = simpleFeatureScores;
                if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
                    for (SimpleFeatureScores decoy : decoyPartList) {
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
            for (SimpleFeatureScores decoy : decoyPartList) {
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
    private void targetDecoyDistribution(List<SimpleFeatureScores> featureScoresList, OverviewDO overviewDO) {
        HashMap<String, Integer> targetDistributions = ProProUtil.buildDistributionMap();
        HashMap<String, Integer> decoyDistributions = ProProUtil.buildDistributionMap();
        for (SimpleFeatureScores sfs : featureScoresList) {
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
            for (FeatureScores featureScores : peptideScores.getFeatureScoresList()) {
                int count = 0;
                if (featureScores.get(ScoreType.NormRtScore, scoreTypes) != null && featureScores.get(ScoreType.NormRtScore, scoreTypes) > 8) {
                    count++;
                }
                if (featureScores.get(ScoreType.LogSnScore, scoreTypes) != null && featureScores.get(ScoreType.LogSnScore, scoreTypes) < 3) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) != null && featureScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) < 0.8) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) != null && featureScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) > 0.2) {
                    count++;
                }
                if (featureScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) != null && featureScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) > 15) {
                    count++;
                }
                if (featureScores.get(ScoreType.BseriesScore, scoreTypes) != null && featureScores.get(ScoreType.BseriesScore, scoreTypes) < 1) {
                    count++;
                }
                if (featureScores.get(ScoreType.YseriesScore, scoreTypes) != null && featureScores.get(ScoreType.YseriesScore, scoreTypes) < 5) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) != null && featureScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) < 0.6) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShape, scoreTypes) != null && featureScores.get(ScoreType.XcorrShape, scoreTypes) < 0.5) {
                    count++;
                }

                if (count > 3) {
                    featureScores.setThresholdPassed(false);
                }
            }
        }
    }
}
