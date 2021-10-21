package net.csibio.propro.algorithm.score;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.*;
import net.csibio.propro.algorithm.score.features.*;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.data.PeptideScore;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupList;
import net.csibio.propro.domain.bean.score.PeakGroupScore;
import net.csibio.propro.domain.bean.score.SelectedPeakGroupScore;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.FeatureUtil;
import net.csibio.propro.utils.PeptideUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

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
    RtNormalizerScorer rtNormalizerScorer;
    @Autowired
    TaskService taskService;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    ElutionScorer elutionScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    SwathLDAScorer swathLDAScorer;
    @Autowired
    LinearFitter linearFitter;
    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    Lda lda;
    @Autowired
    Scorer scorer;

    public void scoreForOne(ExperimentDO exp, DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {

        if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return;
        }
//        dataDO.setIsUnique(peptide.getIsUnique());

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeakGroupList peakGroupList = featureExtractor.getExperimentFeature(dataDO, coord.buildIntensityMap(), params.getMethod().getIrt().getSs());
        if (!peakGroupList.isFeatureFound()) {
            dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
//            log.info("肽段没有被选中的特征：PeptideRef: " + dataDO.getPeptideRef());
            return;
        }

        List<String> scoreTypes = params.getMethod().getScore().getScoreTypes();
        List<PeakGroupScore> peakGroupScoreList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peakGroupList.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupList.getNormedIntMap();
        HashMap<String, Float> productMzMap = new HashMap<>();
        HashMap<String, Integer> productChargeMap = new HashMap<>();

        dataDO.getCutInfoMap().forEach((key, value) -> {
            int charge = PeptideUtil.parseChargeFromCutInfo(key);
            productChargeMap.put(key, charge);
            productMzMap.put(key, value);
        });

        HashMap<Integer, String> unimodHashMap = coord.getUnimodMap();
        String sequence = coord.getSequence();

        HashMap<Double, MzIntensityPairs> peakSpecMap = new HashMap<>();
        for (PeakGroup peakGroup : peakGroupFeatureList) {
            MzIntensityPairs mzIntensityPairs = blockIndexService.getNearestSpectrumByRt(rtMap, peakGroup.getApexRt());
            peakSpecMap.put(peakGroup.getApexRt(), mzIntensityPairs);
        }

        for (PeakGroup peakGroup : peakGroupFeatureList) {
            PeakGroupScore peakGroupScore = new PeakGroupScore(scoreTypes.size());
            chromatographicScorer.calcXICScores(peakGroup, normedLibIntMap, peakGroupScore, scoreTypes);
            //根据RT时间和前体m/z获取最近的一个原始谱图
            if (params.getMethod().getScore().isDiaScores()) {
                MzIntensityPairs mzIntensityPairs = peakSpecMap.get(peakGroup.getApexRt());
                if (mzIntensityPairs != null) {
                    float[] spectrumMzArray = mzIntensityPairs.getMzArray();
                    float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();
                    if (scoreTypes.contains(ScoreType.IsotopeCorrelationScore.getName()) || scoreTypes.contains(ScoreType.IsotopeOverlapScore.getName())) {
                        diaScorer.calculateDiaIsotopeScores(peakGroup, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, peakGroupScore, scoreTypes);
                    }
                    int ionCount = diaScorer.calcTotalIons(mzIntensityPairs.getMzArray(), mzIntensityPairs.getIntensityArray(), unimodHashMap, sequence, 1, peakGroup);
                    peakGroupScore.setTotalIons(ionCount);
                    if (scoreTypes.contains(ScoreType.IonsCountWeightScore.getName())) {
                        diaScorer.calculateIonsScore(ionCount, sequence, peakGroupScore, scoreTypes);
                    }
                    diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, peakGroupScore, scoreTypes);
                }
            }
            if (scoreTypes.contains(ScoreType.LogSnScore.getName())) {
                chromatographicScorer.calculateLogSnScore(peakGroup, peakGroupScore, scoreTypes);
            }

            if (scoreTypes.contains(ScoreType.IntensityScore.getName())) {
                libraryScorer.calculateIntensityScore(peakGroup, peakGroupScore, params.getMethod().getScore().getScoreTypes());
            }

            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, peakGroupScore, scoreTypes);
            if (scoreTypes.contains(ScoreType.NormRtScore.getName())) {
                libraryScorer.calculateNormRtScore(peakGroup, exp.getIrt().getSi(), dataDO.getLibRt(), peakGroupScore, scoreTypes);
            }
            swathLDAScorer.calculateSwathLdaPrescore(peakGroupScore, scoreTypes);
            peakGroupScore.setRt(peakGroup.getApexRt());
            peakGroupScore.setRtRangeFeature(FeatureUtil.toString(peakGroup.getBestLeftRt(), peakGroup.getBestRightRt()));
            peakGroupScore.setIntensitySum(peakGroup.getPeakGroupInt());
            peakGroupScore.setFragIntFeature(FeatureUtil.toString(peakGroup.getIonIntensity()));
            peakGroupScore.setMaxIon(peakGroup.getMaxIon());
            peakGroupScore.setMaxIonIntensity(peakGroup.getMaxIonIntensity());
            peakGroupScore.setIonIntensity(peakGroup.getIonIntensity());
            peakGroupScoreList.add(peakGroupScore);
        }

        if (peakGroupScoreList.size() == 0) {
            dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
            return;
        }

//        if (params.getMethod().getScore().isDiaScores() && scoreTypes.contains(ScoreType.IonsDeltaScore.getName())) {
//            for (PeakGroupScore peakGroupScore : peakGroupScoreList) {
//                peakGroupScore.put(ScoreType.IonsDeltaScore, maxIonCount - peakGroupScore.getTotalIons(), scoreTypes);
//                swathLDAScorer.calculateSwathLdaPrescore(peakGroupScore, scoreTypes);
//            }
//        }
        dataDO.setStatus(IdentifyStatus.WAIT.getCode());
        dataDO.setScoreList(peakGroupScoreList);
    }

    public void strictScoreForOne(DataDO dataDO, PeptideCoord peptide, double shapeScoreThreshold) {
        if (dataDO.getIntMap() == null || dataDO.getIntMap().size() < peptide.getFragments().size()) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return;
        }

        SigmaSpacing ss = SigmaSpacing.create();
        PeakGroupList peakGroupList = featureExtractor.getExperimentFeature(dataDO, peptide.buildIntensityMap(), ss);
        if (!peakGroupList.isFeatureFound()) {
            return;
        }
        List<PeakGroupScore> peakGroupScoreList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peakGroupList.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupList.getNormedIntMap();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            PeakGroupScore peakGroupScore = new PeakGroupScore(2);
            List<String> scoreTypes = new ArrayList<>();
            scoreTypes.add(ScoreType.XcorrShape.getName());
            scoreTypes.add(ScoreType.XcorrShapeWeighted.getName());
            chromatographicScorer.calcXICScores(peakGroupFeature, normedLibIntMap, peakGroupScore, scoreTypes);
            if (peakGroupScore.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes) < shapeScoreThreshold
                    || peakGroupScore.get(ScoreType.XcorrShape.getName(), scoreTypes) < shapeScoreThreshold) {
                continue;
            }
            peakGroupScore.setRt(peakGroupFeature.getApexRt());
            peakGroupScoreList.add(peakGroupScore);
        }

        if (peakGroupScoreList.size() == 0) {
            return;
        }

        dataDO.setScoreList(peakGroupScoreList);
    }

    public void baseScoreForOne(DataDO data, PeptideCoord coord) {
        if (data.getIntMap() == null || data.getIntMap().size() < coord.getFragments().size()) {
            data.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return;
        }

        SigmaSpacing ss = SigmaSpacing.create();
        PeakGroupList peakGroupList = featureExtractor.getExperimentFeature(data, coord.buildIntensityMap(), ss);
        if (!peakGroupList.isFeatureFound()) {
            return;
        }
        List<String> types = ScoreType.getAllTypesName();
        List<PeakGroupScore> peakGroupScoreList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peakGroupList.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupList.getNormedIntMap();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            PeakGroupScore peakGroupScore = new PeakGroupScore(types.size());
            chromatographicScorer.calcXICScores(peakGroupFeature, normedLibIntMap, peakGroupScore, types);
            if (peakGroupScore.get(ScoreType.XcorrShape.getName(), types) < 0.6) {
                continue;
            }
            peakGroupScore.setRt(peakGroupFeature.getApexRt());
            peakGroupScoreList.add(peakGroupScore);
        }

        if (peakGroupScoreList.size() == 0) {
            return;
        }

        data.setScoreList(peakGroupScoreList);
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
        if (data.getScoreList() == null) {
            return null;
        }

        PeptideScore ps = new PeptideScore(data);
        List<String> scoreTypes = overview.getParams().getMethod().getScore().getScoreTypes();
        lda.scoreForPeakGroups(ps.getScoreList(), overview.getWeights(), scoreTypes);

        PeakGroupScore selectPeakGroup = getBestPeakGroup(data.getScoreList(), overview.getMinTotalScore(), scoreTypes, libMaxIon);
        if (selectPeakGroup == null) {
            return null;
        }
        DataSumDO dataSum = new DataSumDO();
        dataSum.setPeptideRef(data.getPeptideRef());
        dataSum.setProteins(data.getProteins());
        if (selectPeakGroup.getTotalScore(scoreTypes) > overview.getMinTotalScore()) {
            dataSum.setStatus(IdentifyStatus.SUCCESS.getCode());
        } else {
            dataSum.setStatus(IdentifyStatus.FAILED.getCode());
        }
        dataSum.setSum(selectPeakGroup.getIntensitySum());
        dataSum.setRealRt(selectPeakGroup.getRt());
        dataSum.setTotalScore(selectPeakGroup.getTotalScore(scoreTypes));
        return dataSum;
    }

    public double calcBestIonsCount(DataDO data) {
        if (data.getScoreList() == null) {
            return -1;
        }
        double maxIonsCount = -1;
        for (PeakGroupScore peakGroupScore : data.getScoreList()) {
            double currentIonsCount = peakGroupScore.getTotalIons();
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
            if (peptideScore.getScoreList() == null || peptideScore.getScoreList().size() == 0) {
                continue;
            }
            SelectedPeakGroupScore bestFeatureScores = new SelectedPeakGroupScore(peptideScore.getId(), peptideScore.getProteins(), peptideScore.getPeptideRef(), peptideScore.getDecoy());
            double maxScore = -Double.MAX_VALUE;
            PeakGroupScore topFeatureScore = null;
            for (PeakGroupScore peakGroupScore : peptideScore.getScoreList()) {
                if (strict && peakGroupScore.getThresholdPassed() != null && !peakGroupScore.getThresholdPassed()) {
                    continue;
                }
                Double targetScore = peakGroupScore.get(targetScoreType, scoreTypes);
                if (targetScore != null && targetScore > maxScore) {
                    maxScore = targetScore;
                    topFeatureScore = peakGroupScore;
                }
            }

            if (topFeatureScore != null) {
                bestFeatureScores.setMainScore(topFeatureScore.get(targetScoreType, scoreTypes));
                bestFeatureScores.setScores(topFeatureScore.getScores());
                bestFeatureScores.setRt(topFeatureScore.getRt());
                bestFeatureScores.setIntensitySum(topFeatureScore.getIntensitySum());
                bestFeatureScores.setFragIntFeature(topFeatureScore.getFragIntFeature());
                bestFeatureScoresList.add(bestFeatureScores);
            }
        }
        return bestFeatureScoresList;
    }

    /**
     * 以scoreType为主分数挑选出所有主分数最高的峰
     *
     * @param peptideScoreList
     * @param scoreTypes       打分开始的时候所有参与打分的子分数快照列表
     * @return
     */
    public List<SelectedPeakGroupScore> findBestPeakGroupByTotalScore(List<PeptideScore> peptideScoreList, List<String> scoreTypes) {
        List<SelectedPeakGroupScore> bestFeatureScoresList = new ArrayList<>();
        for (PeptideScore peptideScore : peptideScoreList) {
            if (peptideScore.getScoreList() == null || peptideScore.getScoreList().size() == 0) {
                continue;
            }
            SelectedPeakGroupScore bestFeatureScores = new SelectedPeakGroupScore(peptideScore.getId(), peptideScore.getProteins(), peptideScore.getPeptideRef(), peptideScore.getDecoy());
            double maxScore = -Double.MAX_VALUE;
            PeakGroupScore topFeatureScore = null;
            for (PeakGroupScore peakGroupScore : peptideScore.getScoreList()) {
                Double targetScore = peakGroupScore.getTotalScore(scoreTypes);
                if (targetScore != null && targetScore > maxScore) {
                    maxScore = targetScore;
                    topFeatureScore = peakGroupScore;
                }
            }

            if (topFeatureScore != null) {
                bestFeatureScores.setMainScore(topFeatureScore.get(ScoreType.WeightedTotalScore, scoreTypes));
                bestFeatureScores.setScores(topFeatureScore.getScores());
                bestFeatureScores.setRt(topFeatureScore.getRt());
                bestFeatureScores.setIntensitySum(topFeatureScore.getIntensitySum());
                bestFeatureScores.setFragIntFeature(topFeatureScore.getFragIntFeature());
                bestFeatureScoresList.add(bestFeatureScores);
            }
        }
        return bestFeatureScoresList;
    }

    public List<SelectedPeakGroupScore> findBestPeakGroupByTargetScoreTypeAndMinTotalScore(List<PeptideScore> peptideScoreList, String targetScoreType, List<String> scoreTypes, Double minTotalScore) {
        List<SelectedPeakGroupScore> bestFeatureScoresList = new ArrayList<>();
        for (PeptideScore peptideScore : peptideScoreList) {
            if (peptideScore.getScoreList() == null || peptideScore.getScoreList().size() == 0) {
                continue;
            }
            SelectedPeakGroupScore bestFeatureScores = new SelectedPeakGroupScore(peptideScore.getId(), peptideScore.getProteins(), peptideScore.getPeptideRef(), peptideScore.getDecoy());

            PeakGroupScore topFeatureScore = scorer.getBestPeakGroup(peptideScore.getScoreList(), minTotalScore, scoreTypes, null);
            if (topFeatureScore != null) {
                bestFeatureScores.setTotalIons(topFeatureScore.getTotalIons());
                bestFeatureScores.setMainScore(topFeatureScore.get(targetScoreType, scoreTypes));
                bestFeatureScores.setScores(topFeatureScore.getScores());
                bestFeatureScores.setRt(topFeatureScore.getRt());
                bestFeatureScores.setIntensitySum(topFeatureScore.getIntensitySum());
                bestFeatureScores.setFragIntFeature(topFeatureScore.getFragIntFeature());
                bestFeatureScoresList.add(bestFeatureScores);
            }
        }
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
    public PeakGroupScore getBestPeakGroup(List<PeakGroupScore> peakGroupScoreList, double minTotalScore, List<String> scoreTypes, String maxLibIonLimit) {
        if (peakGroupScoreList == null || peakGroupScoreList.size() == 0) {
            return null;
        }
        double bestTotalScore = -1d;
        int bestIndex = -1;
        List<Integer> candidateIndexList = new ArrayList<>();
        for (int i = 0; i < peakGroupScoreList.size(); i++) {
            PeakGroupScore peakGroup = peakGroupScoreList.get(i);
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
            if (currentTotalScore != null && currentTotalScore > minTotalScore) {
                candidateIndexList.add(i);
            }
            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
                bestTotalScore = currentTotalScore;
                bestIndex = i;
            }
        }

        int selectPeakGroupIndex = bestIndex;
        if (candidateIndexList.size() > 0) {
            //BY离子分与isotope分均高的才切换
            double bestBYIons = peakGroupScoreList.get(bestIndex).getTotalIons();
            double bestIsotope = peakGroupScoreList.get(bestIndex).get(ScoreType.IsotopeCorrelationScore, scoreTypes);
            for (Integer index : candidateIndexList) {
                double currentBYIons = peakGroupScoreList.get(index).getTotalIons();
                double currentIsotope = peakGroupScoreList.get(index).get(ScoreType.IsotopeCorrelationScore, scoreTypes);
                if (currentBYIons > bestBYIons && (currentIsotope > bestIsotope || bestIsotope - currentIsotope < 0.1)) {
                    bestBYIons = currentBYIons;
                    bestIsotope = currentIsotope;
                    selectPeakGroupIndex = index;
                }
            }
            if (selectPeakGroupIndex != bestIndex) {
                double iosBefore = peakGroupScoreList.get(bestIndex).get(ScoreType.IsotopeCorrelationScore, scoreTypes);
                double iosAfter = peakGroupScoreList.get(selectPeakGroupIndex).get(ScoreType.IsotopeCorrelationScore, scoreTypes);

                double byCountBefore = peakGroupScoreList.get(bestIndex).getTotalIons();
                double byCountAfter = peakGroupScoreList.get(selectPeakGroupIndex).getTotalIons();
                log.info("切换前/后BYIon打分:" + byCountBefore + "/" + byCountAfter + ";" + (byCountAfter > byCountBefore ? "切换靠谱" : "不靠谱"));
                log.info("切换前/后Isotope打分:" + iosBefore + "/" + iosAfter + ";" + ((iosAfter > iosBefore || (iosBefore - iosAfter < 0.1)) ? "切换靠谱" : "不靠谱"));
                log.info("组内切换了对应峰组,最高得分Index:" + bestIndex + ";" + "选中Index:" + selectPeakGroupIndex);
            }
        }

        if (selectPeakGroupIndex == -1) {
            return null;
        }
        return peakGroupScoreList.get(selectPeakGroupIndex);
    }

    public void removeIons(PeakGroupList peakGroupList, String cutInfo) {
        peakGroupList.getNormedIntMap().remove(cutInfo);
        for (PeakGroup peakGroup : peakGroupList.getList()) {
            peakGroup.getIonIntensity().remove(cutInfo);
            peakGroup.getIonHullInt().remove(cutInfo);
            peakGroup.setIonCount(peakGroup.getIonCount() - 1);
        }
    }
//
//    public PeakGroupScore getBestTargetPeak(DataDO data, String typeName) {
//        if (data.getScoreList() == null) {
//            return null;
//        }
//
//        PeptideScore ps = new PeptideScore(data);
//
//        double bestTotalScore = -1d;
//        PeakGroupScore bestPeak = null;
//        for (int i = 0; i < ps.getScoreList().size(); i++) {
//            Double currentTotalScore = ps.getScoreList().get(i).get(typeName, ScoreType.getAllTypesName());
//            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
//                bestTotalScore = currentTotalScore;
//                bestPeak = ps.getScoreList().get(i);
//            }
//        }
//
//        return bestPeak;
//    }
}
