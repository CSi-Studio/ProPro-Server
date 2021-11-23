package net.csibio.propro.algorithm.score.scorer;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.core.CoreFunc;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.GaussFilter;
import net.csibio.propro.algorithm.peak.PeakGroupPicker;
import net.csibio.propro.algorithm.peak.PeakPicker;
import net.csibio.propro.algorithm.peak.SignalToNoiseEstimator;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.features.*;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupListWrapper;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.PeptideUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("scorer")
public class Scorer {

    @Autowired
    DataService dataService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    PeakGroupPicker peakGroupPicker;
    @Autowired
    TaskService taskService;
    @Autowired
    RunService runService;
    @Autowired
    XicScorer xicScorer;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    ElutionScorer elutionScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    InitScorer initScorer;
    @Autowired
    LinearFitter linearFitter;
    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    Lda lda;
    @Autowired
    Scorer scorer;
    @Autowired
    CoreFunc coreFunc;

    public DataDO score(RunDO run, DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {

        if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return dataDO;
        }

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeakGroupListWrapper peakGroupListWrapper = peakPicker.searchByIonsCount(dataDO, coord, params.getMethod().getIrt().getSs());
        if (!peakGroupListWrapper.isFound()) {
            //如果没有信号,首先扩大搜索范围
            coord.setRtRange(coord.getRtStart() - 200, coord.getRtEnd() + 200);
            dataDO = coreFunc.extractOne(coord, rtMap, params);
            if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
                dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
                return dataDO;
            }
            peakGroupListWrapper = peakPicker.searchByIonsCount(dataDO, coord, params.getMethod().getIrt().getSs());
            if (!peakGroupListWrapper.isFound()) {
                dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
                if (!dataDO.getDecoy()) {
//                log.info("作为库中真肽段居然一个峰组都没有找到,也是牛逼：PeptideRef: " + dataDO.getPeptideRef());
                }
                return dataDO;
            }
        }

        List<String> scoreTypes = params.getMethod().getScore().getScoreTypes();

        //开始为每一个PeakGroup打分
        //Step1.准备相关的打分需要使用的前验数据
        List<PeakGroup> peakGroupList = peakGroupListWrapper.getList();
        if (peakGroupList.size() == 0) {
            dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
            return dataDO;
        }

        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormIntMap(); //归一化的库强度值
        HashMap<String, Float> productMzMap = new HashMap<>(); //碎片mz map
        HashMap<String, Integer> productChargeMap = new HashMap<>(); //碎片带电量map
        dataDO.getCutInfoMap().forEach((key, value) -> {
            int charge = PeptideUtil.parseChargeFromCutInfo(key);
            productChargeMap.put(key, charge);
            productMzMap.put(key, value);
        });
        HashMap<Double, MzIntensityPairs> selectedSpectMap = new HashMap<>();
        int maxIonsCount = Arrays.stream(dataDO.getIonsLow()).max().getAsInt();

        for (PeakGroup peakGroup : peakGroupList) {
            selectedSpectMap.put(peakGroup.getSelectedRt(), rtMap.get(peakGroup.getSelectedRt().floatValue()));
        }

        peakGroupList = peakGroupList.stream().sorted(Comparator.comparing(PeakGroup::getSelectedRt)).collect(Collectors.toList());

        //开始对所有的PeakGroup进行打分
        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(scoreTypes.size());
            //根据RT时间和前体m/z获取最近的一个原始谱图
            MzIntensityPairs mzIntensityPairs = selectedSpectMap.get(peakGroup.getSelectedRt());
            float[] spectrumMzArray = mzIntensityPairs.getMzArray();
            float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();

            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes);
            xicScorer.calculateLogSnScore(peakGroup, scoreTypes);
            diaScorer.calculateIsotopeScores(peakGroup, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, scoreTypes);
            diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, peakGroup, scoreTypes);
            libraryScorer.calculateNormRtScore(peakGroup, run.getIrt().getSi(), dataDO.getLibRt(), scoreTypes);
            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, scoreTypes);
            peakGroup.put(ScoreType.IonsDelta, (maxIonsCount - peakGroup.getIonsLow()) * 1d / maxIonsCount, scoreTypes);
//            peakGroup.put(ScoreType.InitScore, peakGroup.getTotal(), scoreTypes);
            peakGroup.put(ScoreType.InitScore, 0d, scoreTypes);
        }

        dataDO.setStatus(IdentifyStatus.WAIT.getCode());
        dataDO.setPeakGroupList(peakGroupList);
        return dataDO;
    }

    public void strictScoreForOne(DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, double shapeScoreThreshold) {
        if (dataDO.getIntMap() == null || dataDO.getIntMap().size() < coord.getFragments().size()) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return;
        }

        SigmaSpacing ss = SigmaSpacing.create();
        PeakGroupListWrapper peakGroupListWrapper = peakPicker.searchByIonsShape(dataDO, coord, ss);
        if (!peakGroupListWrapper.isFound()) {
            return;
        }

        List<PeakGroup> peakGroupList = peakGroupListWrapper.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormIntMap();
        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(2);
            List<String> scoreTypes = new ArrayList<>();
            scoreTypes.add(ScoreType.CorrShape.getName());
            scoreTypes.add(ScoreType.CorrShapeW.getName());
            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes);
            if (peakGroup.get(ScoreType.CorrShapeW.getName(), scoreTypes) < shapeScoreThreshold || peakGroup.get(ScoreType.CorrShape.getName(), scoreTypes) < shapeScoreThreshold) {
                continue;
            }
            peakGroup.setApexRt(peakGroup.getApexRt());
            peakGroupList.add(peakGroup);
        }

        if (peakGroupList.size() == 0) {
            return;
        }

        dataDO.setPeakGroupList(peakGroupList);
    }

    /**
     * 计算最好的峰的总分,并且加上鉴定态
     *
     * @param data
     * @param overview
     * @return
     */
    public DataSumDO calcBestTotalScore(DataDO data, OverviewDO overview) {
        if (data.getPeakGroupList() == null) {
            return null;
        }

        DataScore dataScore = new DataScore(data);
        List<String> scoreTypes = overview.getParams().getMethod().getScore().getScoreTypes();
        lda.scoreForPeakGroups(dataScore.getPeakGroupList(), overview.getWeights(), scoreTypes);

        double maxScore = -Double.MAX_VALUE;
        PeakGroup topPeakGroup = null;
        for (PeakGroup peakGroup : dataScore.getPeakGroupList()) {
            if (peakGroup.getNotMine()) {
                continue;
            }
            Double targetScore = peakGroup.getTotalScore();
            if (targetScore != null && targetScore > maxScore) {
                maxScore = targetScore;
                topPeakGroup = peakGroup;
            }
        }
        if (topPeakGroup == null) {
            return null;
        }

        DataSumDO dataSum = DataSumDO.buildByPeakGroupScore(data.getProteins(), data.getPeptideRef(), topPeakGroup);
        if (topPeakGroup.getTotalScore() > overview.getMinTotalScore()) {
            dataSum.setStatus(IdentifyStatus.SUCCESS.getCode());
        } else {
            dataSum.setStatus(IdentifyStatus.FAILED.getCode());
        }
        dataSum.setTotalScore(topPeakGroup.getTotalScore());
        return dataSum;
    }

    public double calcBestIonsCount(DataDO data) {
        if (data.getPeakGroupList() == null) {
            return -1;
        }
        double maxIonsCount = -1;
        for (PeakGroup peakGroupScore : data.getPeakGroupList()) {
            double currentIonsCount = peakGroupScore.getIonsLow();
            if (currentIonsCount > maxIonsCount) {
                maxIonsCount = currentIonsCount;
            }
        }
        return maxIonsCount;
    }

    /**
     * 以scoreType为主分数挑选出所有主分数最高的峰
     *
     * @param dataScoreList
     * @return
     */
    public List<SelectedPeakGroup> findBestPeakGroup(List<DataScore> dataScoreList) {
        List<SelectedPeakGroup> bestFeatureScoresList = new ArrayList<>();
        for (DataScore dataScore : dataScoreList) {
            if (dataScore.getPeakGroupList() == null || dataScore.getPeakGroupList().size() == 0) {
                continue;
            }
            SelectedPeakGroup bestPeakGroup = new SelectedPeakGroup(dataScore);
            double maxScore = -Double.MAX_VALUE;
            PeakGroup topPeakGroup = null;
            for (PeakGroup peakGroup : dataScore.getPeakGroupList()) {
                if (peakGroup.getNotMine()) {
                    continue;
                }
                Double targetScore = peakGroup.getTotalScore();
                if (targetScore != null && targetScore > maxScore) {
                    maxScore = targetScore;
                    topPeakGroup = peakGroup;
                }
            }
            if (topPeakGroup != null) {
                bestPeakGroup.setTotalScore(topPeakGroup.getTotalScore());
                bestPeakGroup.setScores(topPeakGroup.getScores());
                bestPeakGroup.setApexRt(topPeakGroup.getApexRt());
                bestPeakGroup.setSelectedRt(topPeakGroup.getSelectedRt());
                bestPeakGroup.setIntensitySum(topPeakGroup.getIntensitySum());
                bestFeatureScoresList.add(bestPeakGroup);
            }
        }
        return bestFeatureScoresList;
    }

    public List<SelectedPeakGroup> findBestPeakGroupByMinTotalScore(List<DataScore> dataScoreList, List<String> scoreTypes, Double minTotalScore) {
        List<SelectedPeakGroup> selectedPeakGroups = new ArrayList<>();
        for (DataScore dataScore : dataScoreList) {
            if (dataScore.getPeakGroupList() == null || dataScore.getPeakGroupList().size() == 0) {
                continue;
            }
            SelectedPeakGroup selectedPeakGroup = new SelectedPeakGroup(dataScore);

            //核心代码
            PeakGroup topPeakGroup = scorer.getBestPeakGroup(dataScore.getPeakGroupList(), minTotalScore, scoreTypes, null);
            if (topPeakGroup != null) {
                selectedPeakGroup.setIonsLow(topPeakGroup.getIonsLow());
                selectedPeakGroup.setTotalScore(topPeakGroup.getTotalScore());
                selectedPeakGroup.setScores(topPeakGroup.getScores());
                selectedPeakGroup.setApexRt(topPeakGroup.getApexRt());
                selectedPeakGroup.setSelectedRt(topPeakGroup.getSelectedRt());
                selectedPeakGroup.setIntensitySum(topPeakGroup.getIntensitySum());
                selectedPeakGroups.add(selectedPeakGroup);
            }
        }
        return selectedPeakGroups;
    }

    /**
     * 在同一个组内,如果有两个峰组均满足最小总分阈值,那么选择其中BY离子数更多的一个
     *
     * @param peakGroupList
     * @param minTotalScore
     * @param scoreTypes
     * @param maxLibIonLimit
     * @return
     */
    public PeakGroup getBestPeakGroup(List<PeakGroup> peakGroupList, double minTotalScore, List<String> scoreTypes, String maxLibIonLimit) {
        peakGroupList = peakGroupList.stream().filter(peakGroup -> !peakGroup.getNotMine()).toList();
        if (peakGroupList == null || peakGroupList.size() == 0) {
            return null;
        }
        double bestTotalScore = -1d;
        int bestIndex = -1;

        List<Integer> candidateIndexList = new ArrayList<>();
        for (int i = 0; i < peakGroupList.size(); i++) {
            PeakGroup peakGroup = peakGroupList.get(i);
            if (peakGroup.getNotMine()) {
                continue;
            }
            if (maxLibIonLimit != null) {
                //增加限制条件1. 如果库中最大强度碎片不存在,那么直接跳过
                if (peakGroup.getIonIntensity().get(maxLibIonLimit) == null) {
                    continue;
                }
                //增加限制条件2. 填充的碎片不能超过库中最大强度碎片的强度值
                if (!peakGroup.getMaxIon().equals(maxLibIonLimit)) {
                    continue;
                }
            }

            Double currentTotalScore = peakGroup.getTotalScore();
            if ((currentTotalScore != null && currentTotalScore > minTotalScore)) {
                candidateIndexList.add(i);
            }

            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
                bestTotalScore = currentTotalScore;
                bestIndex = i;
            }
        }

        int selectPeakGroupIndex = bestIndex;
        if (candidateIndexList.size() > 0 && bestIndex != -1) {
            //BY离子分与isotope分均高的才切换
            double bestTotal = peakGroupList.get(bestIndex).get(ScoreType.InitScore, scoreTypes) + peakGroupList.get(bestIndex).getTotalScore();
            for (Integer index : candidateIndexList) {
                //按照total分数进行排序
                if (peakGroupList.get(index).get(ScoreType.InitScore, scoreTypes) + peakGroupList.get(index).getTotalScore() > bestTotal) {
                    bestTotal = peakGroupList.get(index).get(ScoreType.InitScore, scoreTypes) + peakGroupList.get(index).getTotalScore();
                    selectPeakGroupIndex = index;
                }
            }
        }

        if (selectPeakGroupIndex == -1) {
            return null;
        }

        return peakGroupList.get(selectPeakGroupIndex);
    }

    public void removeIons(PeakGroupListWrapper peakGroupListWrapper, String cutInfo) {
        peakGroupListWrapper.getNormIntMap().remove(cutInfo);
        for (PeakGroup peakGroup : peakGroupListWrapper.getList()) {
            peakGroup.getIonIntensity().remove(cutInfo);
            peakGroup.getIonHullInt().remove(cutInfo);
        }
    }
}
