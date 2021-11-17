package net.csibio.propro.algorithm.learner;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.learner.classifier.Xgboost;
import net.csibio.propro.algorithm.peak.PeakIdentifier;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.RunService;
import net.csibio.propro.utils.ProProUtil;
import net.csibio.propro.utils.SortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @Autowired
    RunService runService;
    @Autowired
    PeakIdentifier peakIdentifier;

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
        long temp = System.currentTimeMillis();
        List<DataScore> dataList = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()), DataScore.class, overview.getProjectId());
        log.info("获取打分数据耗时:" + (System.currentTimeMillis() - temp));
        if (dataList == null || dataList.size() == 0) {
            log.info("没有合适的数据");
            return finalResult;
        }
        log.info("总计有待鉴定态肽段" + dataList.size() + "个");
        //Step3. 开始训练数据集
        HashMap<String, Double> weightsMap = new HashMap<>();
        switch (params.getClassifier()) {
            case lda -> {
                weightsMap = lda.classifier(dataList, params, overview.fetchScoreTypes());
                lda.score(dataList, weightsMap, params.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);
            }
            case xgboost -> xgboost.classifier(dataList, overview.fetchScoreTypes(), params);
            default -> {
            }
        }

        //进行第一轮严格意义的初筛
        log.info("开始第一轮严格意义上的初筛");
        List<SelectedPeakGroup> selectedPeakGroupListV1 = scorer.findBestPeakGroupByTargetScoreType(dataList, ScoreType.TotalScore.getName(), overview.fetchScoreTypes());
        statistics.errorStatistics(selectedPeakGroupListV1, params);
        giveDecoyFdr(selectedPeakGroupListV1);

        //获取第一轮严格意义上的最小总分阈值
        double minTotalScore = selectedPeakGroupListV1.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroup::getFdr)).get().getTotalScore();
        log.info("初筛下的最小总分值为:" + minTotalScore + ";开始第二轮筛选");

        //将PeptideList转换为Map
        Map<String, SelectedPeakGroup> selectedDataMap = selectedPeakGroupListV1.stream().filter(peakGroup -> !peakGroup.getDecoy()).collect(Collectors.toMap(SelectedPeakGroup::getPeptideRef, Function.identity()));
        RunDO run = runService.getById(overview.getRunId());
        List<WindowRange> ranges = run.getWindowRanges();

        peakIdentifier.identify(overview.getRunId(), dataList, selectedDataMap, ranges, overview.getAnaLibId(), minTotalScore); //后置优化算法1->选择了相同rt的近似肽段做一个区分
        List<SelectedPeakGroup> selectedPeakGroupListV2 = scorer.findBestPeakGroupByTargetScoreType(dataList, ScoreType.TotalScore.getName(), overview.fetchScoreTypes());
        //重新统计
        ErrorStat errorStat = statistics.errorStatistics(selectedPeakGroupListV2, params);
        giveDecoyFdr(selectedPeakGroupListV2);

        long start = System.currentTimeMillis();
        //Step4. 对于最终的打分结果和选峰结果保存到数据库中, 插入最终的DataSum表的数据为所有的鉴定结果以及 fdr小于0.01的伪肽段
        log.info("将合并打分及定量结果反馈更新到数据库中,总计:" + selectedPeakGroupListV2.size() + "条数据,开始统计相关数据,FDR:" + params.getFdr());
        minTotalScore = selectedPeakGroupListV2.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroup::getFdr)).get().getTotalScore();

        log.info("最小阈值总分为:" + minTotalScore);
        dataSumService.buildDataSumList(selectedPeakGroupListV2, params.getFdr(), overview, overview.getProjectId());
        log.info("插入Sum数据" + selectedPeakGroupListV2.size() + "条一共用时：" + (System.currentTimeMillis() - start) + "毫秒");
        overview.setWeights(weightsMap);

        targetDecoyDistribution(selectedPeakGroupListV2, overview); //统计Target Decoy分布的函数
        overviewService.update(overview);
        overviewService.statistic(overview);

        finalResult.setAllInfo(errorStat);
        int count = ProProUtil.checkFdr(finalResult, params.getFdr());
        log.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    private Result check(List<DataScore> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (DataScore score : scores) {
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
    public void giveDecoyFdr(List<SelectedPeakGroup> featureScoresList) {
        List<SelectedPeakGroup> sortedAll = SortUtil.sortByMainScore(featureScoresList, false);
        SelectedPeakGroup leftFeatureScore = null;
        SelectedPeakGroup rightFeatureScore;
        List<SelectedPeakGroup> decoyPartList = new ArrayList<>();
        for (SelectedPeakGroup selectedPeakGroup : sortedAll) {
            if (selectedPeakGroup.getDecoy()) {
                decoyPartList.add(selectedPeakGroup);
            } else {
                rightFeatureScore = selectedPeakGroup;
                if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
                    for (SelectedPeakGroup decoy : decoyPartList) {
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
            for (SelectedPeakGroup decoy : decoyPartList) {
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
    public void targetDecoyDistribution(List<SelectedPeakGroup> featureScoresList, OverviewDO overviewDO) {
        HashMap<String, Integer> targetDistributions = ProProUtil.buildDistributionMap();
        HashMap<String, Integer> decoyDistributions = ProProUtil.buildDistributionMap();
        for (SelectedPeakGroup sfs : featureScoresList) {
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
}
