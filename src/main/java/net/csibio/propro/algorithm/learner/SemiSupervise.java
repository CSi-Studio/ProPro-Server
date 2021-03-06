package net.csibio.propro.algorithm.learner;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.learner.classifier.Xgboost;
import net.csibio.propro.algorithm.peak.PeakFitter;
import net.csibio.propro.algorithm.peak.SimilarPeakOptimizer;
import net.csibio.propro.algorithm.score.scorer.Scorer;
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
    SimilarPeakOptimizer similarPeakOptimizer;
    @Autowired
    PeakFitter peakFitter;

    public FinalResult doSemiSupervise(String overviewId, LearningParams params) {
        FinalResult finalResult = new FinalResult();

        //Step1. ???????????????
        log.info("???????????????");
        OverviewDO overview = overviewService.getById(overviewId);
        if (overview == null) {
            finalResult.setErrorInfo(ResultCode.OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }
        params.setType(overview.getType());
        //Step2. ????????????????????????????????????????????????
        log.info("????????????????????????");
        long temp = System.currentTimeMillis();
        List<DataScore> dataList = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()), DataScore.class, overview.getProjectId());
        log.info("????????????????????????:" + (System.currentTimeMillis() - temp));
        if (dataList == null || dataList.size() == 0) {
            log.info("?????????????????????");
            return finalResult;
        }
        log.info("???????????????????????????" + dataList.size() + "???");
        //Step3. ?????????????????????
        HashMap<String, Double> weightsMap = new HashMap<>();
        switch (params.getClassifier()) {
            case "LDA" -> {
                weightsMap = lda.classifier(dataList, params);
                if (weightsMap == null) {
                    return finalResult;
                }
                lda.score(dataList, weightsMap, params.getScoreTypes()); //????????????PeakGroup???TotalScore??????
                finalResult.setWeightsMap(weightsMap);
            }
            case "XGBoost" -> {
                xgboost.classifier(dataList, params);
            }
            default -> {
            }
        }

        //????????????????????????????????????
        log.info("???????????????????????????????????????");
        List<SelectedPeakGroup> selectedPeakGroupListV1 = scorer.findBestPeakGroup(dataList);
        statistics.errorStatistics(selectedPeakGroupListV1, params);
        giveDecoyFdr(selectedPeakGroupListV1);

        double minTotalScore = Double.MIN_VALUE;
        //???????????????????????????????????????????????????
        if (selectedPeakGroupListV1.stream().anyMatch(s -> s.getFdr() != null && s.getFdr() < params.getFdr())) {
            minTotalScore = selectedPeakGroupListV1.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroup::getFdr)).get().getTotalScore();
            log.info("??????????????????????????????:" + minTotalScore + ";?????????????????????");
        } else {
            return finalResult;
        }

        //???PeptideList?????????Map
        Map<String, SelectedPeakGroup> selectedDataMap = selectedPeakGroupListV1.stream().filter(peakGroup -> !peakGroup.getDecoy()).collect(Collectors.toMap(SelectedPeakGroup::getPeptideRef, Function.identity()));
        RunDO run = runService.getById(overview.getRunId());
        List<WindowRange> ranges = run.getWindowRanges();

        //??????1. ???????????????????????????
        similarPeakOptimizer.optimizer(overview.getRunId(), dataList, selectedDataMap, ranges, overview.getAnaLibId(), minTotalScore); //??????????????????1->???????????????rt??????????????????????????????
        //??????2. ??????IonsCount?????????????????????
        List<SelectedPeakGroup> selectedPeakGroupListV2 = scorer.findBestPeakGroup(dataList);

        //????????????
        ErrorStat errorStat = statistics.errorStatistics(selectedPeakGroupListV2, params);
        giveDecoyFdr(selectedPeakGroupListV2);

        long start = System.currentTimeMillis();
        //Step4. ???????????????????????????????????????????????????????????????, ???????????????DataSum?????????????????????????????????????????? fdr??????0.01????????????
        log.info("?????????????????????????????????????????????????????????,??????:" + selectedPeakGroupListV2.size() + "?????????,????????????????????????,FDR:" + params.getFdr());
        minTotalScore = selectedPeakGroupListV2.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroup::getFdr)).get().getTotalScore();

        log.info("?????????????????????:" + minTotalScore);
        dataSumService.buildDataSumList(selectedPeakGroupListV2, params.getFdr(), overview, overview.getProjectId());
        log.info("??????Sum??????" + selectedPeakGroupListV2.size() + "??????????????????" + (System.currentTimeMillis() - start) + "??????");
        overview.setWeights(weightsMap);

        targetDecoyDistribution(selectedPeakGroupListV2, overview); //??????Target Decoy???????????????
        overviewService.update(overview);
        overviewService.statistic(overview);

        finalResult.setAllInfo(errorStat);
        int count = ProProUtil.checkFdr(finalResult, params.getFdr());
        log.info("??????????????????,??????????????????" + count + "???");
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

    //????????????target??????decoy??????Fdr???, ??????????????????decoy??????, fdr???null
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
                        if (decoy.getTotalScore() - leftFeatureScore.getTotalScore() < rightFeatureScore.getTotalScore() - decoy.getTotalScore()) {
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
     * ??????FDR<=0.01,???0.001????????????????????????
     * ??????FDR>0.01,???0.1?????????????????????
     *
     * @param featureScoresList
     * @param overviewDO
     */
    //TODO WJW ???????????????
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
