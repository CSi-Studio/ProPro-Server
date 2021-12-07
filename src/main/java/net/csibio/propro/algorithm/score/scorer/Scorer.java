package net.csibio.propro.algorithm.score.scorer;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.core.CoreFunc;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.*;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.features.DIAScorer;
import net.csibio.propro.algorithm.score.features.ElutionScorer;
import net.csibio.propro.algorithm.score.features.LibraryScorer;
import net.csibio.propro.algorithm.score.features.XicScorer;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.PeakFindingMethod;
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
    NoiseEstimator noiseEstimator;
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
    LinearFitter linearFitter;
    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    Lda lda;
    @Autowired
    Scorer scorer;
    @Autowired
    CoreFunc coreFunc;
    @Autowired
    PeakFitter peakFitter;

    public DataDO score(RunDO run, DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {

        if (dataDO.getIntMap() == null || dataDO.getIntMap().size() <= coord.getFragments().size() / 2) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return dataDO;
        }

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        SigmaSpacing ss = params.getMethod().getIrt().getSs();
        PeakGroupListWrapper peakGroupListWrapper = null;

        if (params.getMethod().getPeakFinding().getPeakFindingMethod().equals(PeakFindingMethod.IONS_COUNT.getName())) {
            peakGroupListWrapper = peakPicker.searchByIonsCount(dataDO, coord, ss);
        } else {
            peakGroupListWrapper = peakPicker.searchByIonsShape(dataDO, coord, ss);
        }

        if (!peakGroupListWrapper.isFound()) {
            //重试机制:扩大RT搜索范围并使用IonsShape重新计算XIC
            coord.setRtRange(coord.getRtStart() - 200, coord.getRtEnd() + 200);
            dataDO = coreFunc.extractOne(coord, rtMap, params, true, 100f);
            if (dataDO.getIntMap() == null || dataDO.getIntMap().size() <= coord.getFragments().size() / 2) {
                dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
                return dataDO;
            }

            if (params.getMethod().getPeakFinding().getPeakFindingMethod().equals(PeakFindingMethod.IONS_COUNT.getName())) {
                peakGroupListWrapper = peakPicker.searchByIonsCount(dataDO, coord, ss);
            } else {
                peakGroupListWrapper = peakPicker.searchByIonsShape(dataDO, coord, ss);
            }
            if (!peakGroupListWrapper.isFound()) {
                dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
                return dataDO;
            }
        }

        //准备流程参数
        List<String> scoreTypes = params.getMethod().getScore().getScoreTypes();

        //开始为每一个PeakGroup打分
        //Step1.准备相关的打分需要使用的前验数据
        List<PeakGroup> peakGroupList = peakGroupListWrapper.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormIntMap(); //归一化的库强度值
        HashMap<String, Float> productMzMap = new HashMap<>(); //碎片mz map
        HashMap<String, Integer> productChargeMap = new HashMap<>(); //碎片带电量map
        dataDO.getCutInfoMap().forEach((key, value) -> {
            int charge = PeptideUtil.parseChargeFromCutInfo(key);
            productChargeMap.put(key, charge);
            productMzMap.put(key, value);
        });
        HashMap<Double, MzIntensityPairs> selectedSpectMap = new HashMap<>();
        int maxIonsCount = Arrays.stream(dataDO.getIonsHigh()).max().getAsInt();

        for (PeakGroup peakGroup : peakGroupList) {
            selectedSpectMap.put(peakGroup.getSelectedRt(), rtMap.get(peakGroup.getSelectedRt().floatValue()));
        }

        peakGroupList = peakGroupList.stream().sorted(Comparator.comparing(PeakGroup::getSelectedRt)).collect(Collectors.toList());

        //开始对所有的PeakGroup进行打分
        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(scoreTypes.size());
            //根据RT时间和前体m/z获取最近的一个原始谱图
            MzIntensityPairs mzIntensityPairs = selectedSpectMap.get(peakGroup.getSelectedRt());
            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, scoreTypes);
            xicScorer.calculateLogSnScore(peakGroup, scoreTypes);
            diaScorer.calculateDiaMassDiffScore(productMzMap, mzIntensityPairs, normedLibIntMap, peakGroup, scoreTypes);
            libraryScorer.calculateNormRtScore(peakGroup, run.getIrt().getSi(), dataDO.getLibRt(), scoreTypes);
            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes);
            xicScorer.calcPearsonMatrixScore(peakGroup, normedLibIntMap, coord, scoreTypes);
            diaScorer.calculateIsotopeScores(peakGroup, productMzMap, productChargeMap, mzIntensityPairs, scoreTypes);
            peakGroup.put(ScoreType.IonsDelta, (maxIonsCount - peakGroup.getIonsHigh()) * 1d / maxIonsCount, scoreTypes);

            peakFitter.fit(peakGroup, coord); //拟合定量结果
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
                bestPeakGroup.setBestIon(topPeakGroup.getBestIon());
                bestPeakGroup.setSelectedRt(topPeakGroup.getSelectedRt());
                bestPeakGroup.setIntensitySum(topPeakGroup.getIntensitySum());
                bestPeakGroup.setFitIntSum(topPeakGroup.getFitIntSum());
                bestFeatureScoresList.add(bestPeakGroup);
            }
        }
        return bestFeatureScoresList;
    }

    public List<SelectedPeakGroup> findBestPeakGroupByMinTotalScore(List<DataScore> dataScoreList, Double minTotalScore) {
        List<SelectedPeakGroup> selectedPeakGroups = new ArrayList<>();
        for (DataScore dataScore : dataScoreList) {
            if (dataScore.getPeakGroupList() == null || dataScore.getPeakGroupList().size() == 0) {
                continue;
            }
            SelectedPeakGroup selectedPeakGroup = new SelectedPeakGroup(dataScore);

            //核心代码
            PeakGroup topPeakGroup = scorer.getBestPeakGroup(dataScore.getPeakGroupList(), minTotalScore);
            if (topPeakGroup != null) {
                selectedPeakGroup.setIonsLow(topPeakGroup.getIonsLow());
                selectedPeakGroup.setTotalScore(topPeakGroup.getTotalScore());
                selectedPeakGroup.setScores(topPeakGroup.getScores());
                selectedPeakGroup.setApexRt(topPeakGroup.getApexRt());
                selectedPeakGroup.setBestIon(topPeakGroup.getBestIon());
                selectedPeakGroup.setSelectedRt(topPeakGroup.getSelectedRt());
                selectedPeakGroup.setIntensitySum(topPeakGroup.getIntensitySum());
                selectedPeakGroup.setFitIntSum(topPeakGroup.getFitIntSum());
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
     * @return
     */
    public PeakGroup getBestPeakGroup(List<PeakGroup> peakGroupList, double minTotalScore) {
        peakGroupList = peakGroupList.stream().filter(peakGroup -> !peakGroup.getNotMine()).toList();
        if (peakGroupList == null || peakGroupList.size() == 0) {
            return null;
        }
        double bestTotalScore = -1d;
        int bestIndex = -1;

        List<Integer> candidateIndexList = new ArrayList<>();
        for (int i = 0; i < peakGroupList.size(); i++) {
            PeakGroup peakGroup = peakGroupList.get(i);
            Double currentTotalScore = peakGroup.getTotalScore();
            //凡是高于最低阈值的均进入候选峰列表
            if ((currentTotalScore != null && currentTotalScore > minTotalScore)) {
                candidateIndexList.add(i);
            }
            //选出最高总分的峰
            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
                bestTotalScore = currentTotalScore;
                bestIndex = i;
            }
        }

        int selectPeakGroupIndex = bestIndex;
        if (candidateIndexList.size() > 0 && bestIndex != -1) {
            //优先采用IonsLow的数目进行筛选
            double ionsLow = peakGroupList.get(bestIndex).getTotalScore();
            for (Integer index : candidateIndexList) {
                //按照total分数进行排序
                if (peakGroupList.get(index).getIonsLow() > ionsLow) {
                    ionsLow = peakGroupList.get(index).getIonsLow();
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
