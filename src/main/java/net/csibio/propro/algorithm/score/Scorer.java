package net.csibio.propro.algorithm.score;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.core.CoreFunc;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.*;
import net.csibio.propro.algorithm.score.features.*;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.common.AnyPair;
import net.csibio.propro.domain.bean.data.PeptideScore;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupListWrapper;
import net.csibio.propro.domain.bean.score.SelectedPeakGroupScore;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.ArrayUtil;
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
    ChromatogramPicker chromatogramPicker;
    @Autowired
    FeatureFinder featureFinder;
    @Autowired
    TaskService taskService;
    @Autowired
    ExperimentService experimentService;
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

    public DataDO scoreForOne(ExperimentDO exp, DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {

        if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return dataDO;
        }
//        dataDO.setIsUnique(peptide.getIsUnique());

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeakGroupListWrapper peakGroupListWrapper = peakPicker.searchPeakGroupsV2(dataDO, coord, params.getMethod().getIrt().getSs());
        if (!peakGroupListWrapper.isFeatureFound()) {
            //如果没有信号,首先扩大搜索范围
            coord.setRtRange(coord.getRtStart() - 200, coord.getRtEnd() + 200);
            dataDO = coreFunc.extractOne(coord, rtMap, params);
            if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
                dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
                return dataDO;
            }
            peakGroupListWrapper = peakPicker.searchPeakGroups(dataDO, coord, params.getMethod().getIrt().getSs());
            if (!peakGroupListWrapper.isFeatureFound()) {
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

        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormedIntMap(); //归一化的库强度值
        HashMap<String, Float> productMzMap = new HashMap<>(); //碎片mz map
        HashMap<String, Integer> productChargeMap = new HashMap<>(); //碎片带电量map
        dataDO.getCutInfoMap().forEach((key, value) -> {
            int charge = PeptideUtil.parseChargeFromCutInfo(key);
            productChargeMap.put(key, charge);
            productMzMap.put(key, value);
        });
        HashMap<Double, MzIntensityPairs> peakSpecMap = new HashMap<>();
        int maxIonsCount = Arrays.stream(dataDO.getIonsLow()).max().getAsInt();

//        calcNearestRtAndTotalIons(dataDO, coord, coord.getFragments().get(0).getCutInfo(), peakGroupList, rtMap);

        for (PeakGroup peakGroup : peakGroupList) {
            peakSpecMap.put(peakGroup.getApexRt(), rtMap.get(peakGroup.getSelectedRt().floatValue()));
        }

        //如果数组长度超过20,则筛选ions数目最大的20个碎片
        peakGroupList = peakGroupList.stream().sorted(Comparator.comparing(PeakGroup::getIonsLow).reversed()).toList();
        if (peakGroupList.size() > 20) {
            log.info("PeakGroup数组过多,仅保留IonsLow前20的峰组:" + dataDO.getPeptideRef());
            peakGroupList = peakGroupList.subList(0, 20);
        }
        peakGroupList = peakGroupList.stream().sorted(Comparator.comparing(PeakGroup::getApexRt)).collect(Collectors.toList());

        //开始对所有的PeakGroup进行打分
        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(scoreTypes.size());
            //根据RT时间和前体m/z获取最近的一个原始谱图
            MzIntensityPairs mzIntensityPairs = peakSpecMap.get(peakGroup.getApexRt());
            float[] spectrumMzArray = mzIntensityPairs.getMzArray();
            float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();

            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes);
            diaScorer.calculateIsotopeScores(peakGroup, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, scoreTypes);
            diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, peakGroup, scoreTypes);
            xicScorer.calculateLogSnScore(peakGroup, scoreTypes);
            libraryScorer.calculateIntensityScore(peakGroup, params.getMethod().getScore().getScoreTypes());
            libraryScorer.calculateNormRtScore(peakGroup, exp.getIrt().getSi(), dataDO.getLibRt(), scoreTypes);
            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, scoreTypes);
            peakGroup.put(ScoreType.IonsCountDeltaScore, (maxIonsCount - peakGroup.getIonsLow()) * 1d / maxIonsCount, scoreTypes);
            peakGroup.put(ScoreType.InitScore, peakGroup.getTotal(), scoreTypes);
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
        PeakGroupListWrapper peakGroupListWrapper = peakPicker.searchPeakGroups(dataDO, coord, ss);
        if (!peakGroupListWrapper.isFeatureFound()) {
            return;
        }

        List<PeakGroup> peakGroupList = peakGroupListWrapper.getList();
//        calcNearestRtAndTotalIons(dataDO, coord, maxLibIon, peakGroupList, rtMap);

        HashMap<String, Double> normedLibIntMap = peakGroupListWrapper.getNormedIntMap();
        for (PeakGroup peakGroup : peakGroupList) {
            peakGroup.initScore(2);
            List<String> scoreTypes = new ArrayList<>();
            scoreTypes.add(ScoreType.XcorrShape.getName());
            scoreTypes.add(ScoreType.XcorrShapeWeighted.getName());
            xicScorer.calcXICScores(peakGroup, normedLibIntMap, scoreTypes);
            if (peakGroup.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes) < shapeScoreThreshold || peakGroup.get(ScoreType.XcorrShape.getName(), scoreTypes) < shapeScoreThreshold) {
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
     * @param libMaxIon 库中理论最大强度碎片,在实际获得的EIC峰组中,该碎片应该仍然为最大强度碎片
     * @return
     */
    public DataSumDO calcBestTotalScore(DataDO data, OverviewDO overview, String libMaxIon) {
        if (data.getPeakGroupList() == null) {
            return null;
        }

        PeptideScore ps = new PeptideScore(data);
        List<String> scoreTypes = overview.getParams().getMethod().getScore().getScoreTypes();
        lda.scoreForPeakGroups(ps.getPeakGroupList(), overview.getWeights(), scoreTypes);

        PeakGroup selectPeakGroup = getBestPeakGroup(data.getPeakGroupList(), overview.getMinTotalScore(), scoreTypes, libMaxIon);
        if (selectPeakGroup == null) {
            return null;
        }
        DataSumDO dataSum = DataSumDO.buildByPeakGroupScore(data.getProteins(), data.getPeptideRef(), selectPeakGroup);
        if (selectPeakGroup.getTotalScore(scoreTypes) > overview.getMinTotalScore()) {
            dataSum.setStatus(IdentifyStatus.SUCCESS.getCode());
        } else {
            dataSum.setStatus(IdentifyStatus.FAILED.getCode());
        }
        dataSum.setTotalScore(selectPeakGroup.getTotalScore(scoreTypes));
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
     * @param peptideScoreList
     * @param targetScoreType  需要作为主分数的分数
     * @param scoreTypes       打分开始的时候所有参与打分的子分数快照列表
     * @return
     */
    public List<SelectedPeakGroupScore> findBestPeakGroupByTargetScoreType(List<PeptideScore> peptideScoreList, String targetScoreType, List<String> scoreTypes, boolean strict) {
        List<SelectedPeakGroupScore> bestFeatureScoresList = new ArrayList<>();
        for (PeptideScore peptideScore : peptideScoreList) {
            if (peptideScore.getPeakGroupList() == null || peptideScore.getPeakGroupList().size() == 0) {
                continue;
            }
            SelectedPeakGroupScore bestFeatureScores = new SelectedPeakGroupScore(peptideScore.getId(), peptideScore.getProteins(), peptideScore.getPeptideRef(), peptideScore.getDecoy());
            double maxScore = -Double.MAX_VALUE;
            PeakGroup topFeatureScore = null;
            for (PeakGroup peakGroupScore : peptideScore.getPeakGroupList()) {
                Double targetScore = peakGroupScore.get(targetScoreType, scoreTypes);
                if (targetScore != null && targetScore > maxScore) {
                    maxScore = targetScore;
                    topFeatureScore = peakGroupScore;
                }
            }

            if (topFeatureScore != null) {
                bestFeatureScores.setMainScore(topFeatureScore.get(targetScoreType, scoreTypes));
                bestFeatureScores.setScores(topFeatureScore.getScores());
                bestFeatureScores.setApexRt(topFeatureScore.getApexRt());
                bestFeatureScores.setSelectedRt(topFeatureScore.getSelectedRt());
                bestFeatureScores.setIntensitySum(topFeatureScore.getIntensitySum());
//                bestFeatureScores.setFragIntFeature(topFeatureScore.getFragIntFeature());
                bestFeatureScoresList.add(bestFeatureScores);
            }
        }
        return bestFeatureScoresList;
    }

    public List<SelectedPeakGroupScore> findBestPeakGroupByTargetScoreTypeAndMinTotalScore(List<PeptideScore> peptideScoreList, String targetScoreType, List<String> scoreTypes, Double minTotalScore) {
        List<SelectedPeakGroupScore> bestFeatureScoresList = new ArrayList<>();
        List<String> markedPeptideRefList = new ArrayList<>();
        List<String> changedPeptideRefList = new ArrayList<>();
        for (PeptideScore peptideScore : peptideScoreList) {
            if (peptideScore.getPeakGroupList() == null || peptideScore.getPeakGroupList().size() == 0) {
                continue;
            }
            SelectedPeakGroupScore bestFeatureScores = new SelectedPeakGroupScore(peptideScore.getId(), peptideScore.getProteins(), peptideScore.getPeptideRef(), peptideScore.getDecoy());

            //核心代码
            PeakGroup topFeatureScore = scorer.getBestPeakGroup(peptideScore.getPeakGroupList(), minTotalScore, scoreTypes, null);
            if (topFeatureScore != null) {
                if (topFeatureScore.getMark() && !peptideScore.getDecoy() && topFeatureScore.get(targetScoreType, scoreTypes) > minTotalScore) {
                    markedPeptideRefList.add(peptideScore.getPeptideRef());
                }
                if (topFeatureScore.getChanged() && !peptideScore.getDecoy()) {
                    changedPeptideRefList.add(peptideScore.getPeptideRef());
                }
                bestFeatureScores.setIonsLow(topFeatureScore.getIonsLow());
                bestFeatureScores.setMainScore(topFeatureScore.get(targetScoreType, scoreTypes));
                bestFeatureScores.setScores(topFeatureScore.getScores());
                bestFeatureScores.setApexRt(topFeatureScore.getApexRt());
                bestFeatureScores.setSelectedRt(topFeatureScore.getSelectedRt());
                bestFeatureScores.setIntensitySum(topFeatureScore.getIntensitySum());
                bestFeatureScoresList.add(bestFeatureScores);
            }
        }
        log.info("总计有问题的肽段有:" + markedPeptideRefList.size() + "个");
        log.info(JSON.toJSONString(markedPeptideRefList));
        log.info("总计组内切换的肽段有:" + changedPeptideRefList.size() + "个");
        log.info(JSON.toJSONString(changedPeptideRefList));
        return bestFeatureScoresList;
    }

    /**
     * 在同一个组内,如果有两个峰组均满足最小总分阈值,那么选择其中BY离子数更多的一个
     *
     * @param peakGroupScoreList
     * @param minTotalScore
     * @param scoreTypes
     * @param maxLibIonLimit
     * @return
     */
    public PeakGroup getBestPeakGroup(List<PeakGroup> peakGroupScoreList, double minTotalScore, List<String> scoreTypes, String maxLibIonLimit) {
        if (peakGroupScoreList == null || peakGroupScoreList.size() == 0) {
            return null;
        }
        double bestTotalScore = -1d;
        int bestIndex = -1;

        List<Integer> candidateIndexList = new ArrayList<>();
        for (int i = 0; i < peakGroupScoreList.size(); i++) {
            PeakGroup peakGroup = peakGroupScoreList.get(i);
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

            Double currentTotalScore = peakGroup.getTotalScore(scoreTypes);
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
            double bestTotal = peakGroupScoreList.get(bestIndex).get(ScoreType.InitScore, scoreTypes) + peakGroupScoreList.get(bestIndex).getTotalScore();
            for (Integer index : candidateIndexList) {
                //按照total分数进行排序
                if (peakGroupScoreList.get(index).get(ScoreType.InitScore, scoreTypes) + peakGroupScoreList.get(index).getTotalScore() > bestTotal) {
                    bestTotal = peakGroupScoreList.get(index).get(ScoreType.InitScore, scoreTypes) + peakGroupScoreList.get(index).getTotalScore();
                    selectPeakGroupIndex = index;
                }
            }
//            if (selectPeakGroupIndex != bestIndex) {
//
//                double iosBefore = peakGroupScoreList.get(bestIndex).get(ScoreType.IsotopeCorrelationScore, scoreTypes);
//                double iosAfter = peakGroupScoreList.get(selectPeakGroupIndex).get(ScoreType.IsotopeCorrelationScore, scoreTypes);
//
//                double byCountBefore = peakGroupScoreList.get(bestIndex).getTotalIons();
//                double byCountAfter = peakGroupScoreList.get(selectPeakGroupIndex).getTotalIons();
////                log.info("切换前/后BYIon打分:" + byCountBefore + "/" + byCountAfter + ";" + (byCountAfter > byCountBefore ? "切换靠谱" : "不靠谱"));
////                log.info("切换前/后Isotope打分:" + iosBefore + "/" + iosAfter + ";" + ((iosAfter > iosBefore || (iosBefore - iosAfter < 0.1)) ? "切换靠谱" : "不靠谱"));
//                log.info("组内切换了对应峰组,最高得分Index:" + bestIndex + ";" + "选中Index:" + selectPeakGroupIndex);
//            }
        }

        if (selectPeakGroupIndex == -1) {
            return null;
        }

        PeakGroup pgs = peakGroupScoreList.get(selectPeakGroupIndex);
        List<Integer> sorted = peakGroupScoreList.stream().map(PeakGroup::getIonsLow).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        int index = sorted.indexOf(pgs.getIonsLow());

        //排名连前五都没有混到
        if (index > 5 && pgs.getTotalScore() >= minTotalScore) {
            pgs.setMark(true);
        }
        if (selectPeakGroupIndex != bestIndex) {
            pgs.setChanged(true);
        }
        return pgs;
    }

    public void calcNearestRtAndTotalIons(DataDO data, PeptideCoord coord, String maxLibIon, List<PeakGroup> peakGroupList, TreeMap<Float, MzIntensityPairs> rtMap) {
        List<Float> rtList = ArrayUtil.toList(data.getRtArray());
        float[] maxIonsIntArray = data.getIntMap().get(maxLibIon);
        HashMap<Integer, String> unimodHashMap = coord.getUnimodMap();
        String sequence = coord.getSequence();
        for (PeakGroup peakGroup : peakGroupList) {
            AnyPair<Float, Float> nearestRtPair = blockIndexService.getNearestSpectrumByRt(rtMap, peakGroup.getApexRt());
            float maxIntensityLeft = maxIonsIntArray == null ? Float.MAX_VALUE : maxIonsIntArray[rtList.indexOf(nearestRtPair.getLeft())];
            float maxIntensityRight = maxIonsIntArray == null ? Float.MAX_VALUE : maxIonsIntArray[rtList.indexOf(nearestRtPair.getRight())];

            int left = diaScorer.calcTotalIons(rtMap.get(nearestRtPair.getLeft()).getMzArray(), rtMap.get(nearestRtPair.getLeft()).getIntensityArray(), unimodHashMap, sequence, coord.getCharge(), 50, 300, maxIntensityLeft).left();
            int right = diaScorer.calcTotalIons(rtMap.get(nearestRtPair.getRight()).getMzArray(), rtMap.get(nearestRtPair.getRight()).getIntensityArray(), unimodHashMap, sequence, coord.getCharge(), 50, 300, maxIntensityRight).left();
            float bestRt = right > left ? nearestRtPair.getRight() : nearestRtPair.getLeft();
            int ionCount = Math.max(right, left);
            peakGroup.setIonsLow(ionCount);
            peakGroup.setSelectedRt((double) bestRt);
        }
    }

    public void removeIons(PeakGroupListWrapper peakGroupListWrapper, String cutInfo) {
        peakGroupListWrapper.getNormedIntMap().remove(cutInfo);
        for (PeakGroup peakGroup : peakGroupListWrapper.getList()) {
            peakGroup.getIonIntensity().remove(cutInfo);
            peakGroup.getIonHullInt().remove(cutInfo);
        }
    }
}
