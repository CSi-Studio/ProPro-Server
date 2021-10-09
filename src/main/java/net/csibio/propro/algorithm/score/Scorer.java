package net.csibio.propro.algorithm.score;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.*;
import net.csibio.propro.algorithm.score.features.*;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.data.PeptideScores;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupList;
import net.csibio.propro.domain.bean.score.PeakGroupScores;
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
        List<PeakGroupScores> peakGroupScoresList = new ArrayList<>();
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

        for (PeakGroup peakGroup : peakGroupFeatureList) {
            PeakGroupScores peakGroupScores = new PeakGroupScores(params.getMethod().getScore().getScoreTypes().size());
            chromatographicScorer.calculateChromatographicScores(peakGroup, normedLibIntMap, peakGroupScores, params.getMethod().getScore().getScoreTypes());
//            Double shapeScore = peakGroupScores.get(ScoreType.XcorrShape, params.getMethod().getScore().getScoreTypes());
//            Double shapeScoreWeighted = peakGroupScores.get(ScoreType.XcorrShapeWeighted, params.getMethod().getScore().getScoreTypes());
//            if (!dataDO.getDecoy() && ((shapeScore != null && shapeScore < params.getMethod().getQuickFilter().getMinShapeScore()))) {
//                continue;
//            }
            //根据RT时间和前体m/z获取最近的一个原始谱图
            if (params.getMethod().getScore().isDiaScores()) {
                MzIntensityPairs mzIntensityPairs = blockIndexService.getNearestSpectrumByRt(rtMap, peakGroup.getApexRt());
                if (mzIntensityPairs != null) {
                    float[] spectrumMzArray = mzIntensityPairs.getMzArray();
                    float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();
                    if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.IsotopeCorrelationScore.getName()) || params.getMethod().getScore().getScoreTypes().contains(ScoreType.IsotopeOverlapScore.getName())) {
                        diaScorer.calculateDiaIsotopeScores(peakGroup, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, peakGroupScores, params.getMethod().getScore().getScoreTypes());
                    }
                    if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.BseriesScore.getName()) || params.getMethod().getScore().getScoreTypes().contains(ScoreType.YseriesScore.getName())) {
                        diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, peakGroupScores, params.getMethod().getScore().getScoreTypes());
                    }
                    diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, peakGroupScores, params.getMethod().getScore().getScoreTypes());
                }
            }
            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.LogSnScore.getName())) {
                chromatographicScorer.calculateLogSnScore(peakGroup, peakGroupScores, params.getMethod().getScore().getScoreTypes());
            }

            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.IntensityScore.getName())) {
                libraryScorer.calculateIntensityScore(peakGroup, peakGroupScores, params.getMethod().getScore().getScoreTypes());
            }

            libraryScorer.calculateLibraryScores(peakGroup, normedLibIntMap, peakGroupScores, params.getMethod().getScore().getScoreTypes());
            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.NormRtScore.getName())) {
                libraryScorer.calculateNormRtScore(peakGroup, exp.getIrt().getSi(), dataDO.getLibRt(), peakGroupScores, params.getMethod().getScore().getScoreTypes());
            }
            swathLDAScorer.calculateSwathLdaPrescore(peakGroupScores, params.getMethod().getScore().getScoreTypes());
            peakGroupScores.setRt(peakGroup.getApexRt());
            peakGroupScores.setRtRangeFeature(FeatureUtil.toString(peakGroup.getBestLeftRt(), peakGroup.getBestRightRt()));
            peakGroupScores.setIntensitySum(peakGroup.getPeakGroupInt());
            peakGroupScores.setFragIntFeature(FeatureUtil.toString(peakGroup.getIonIntensity()));
            peakGroupScoresList.add(peakGroupScores);
        }

        if (peakGroupScoresList.size() == 0) {
            dataDO.setStatus(IdentifyStatus.NO_PEAK_GROUP_FIND.getCode());
            return;
        }
        dataDO.setStatus(IdentifyStatus.WAIT.getCode());
        dataDO.setScoreList(peakGroupScoresList);
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
        List<PeakGroupScores> peakGroupScoresList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peakGroupList.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupList.getNormedIntMap();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            PeakGroupScores peakGroupScores = new PeakGroupScores(2);
            List<String> scoreTypes = new ArrayList<>();
            scoreTypes.add(ScoreType.XcorrShape.getName());
            scoreTypes.add(ScoreType.XcorrShapeWeighted.getName());
            chromatographicScorer.calculateChromatographicScores(peakGroupFeature, normedLibIntMap, peakGroupScores, scoreTypes);
            if (peakGroupScores.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes) < shapeScoreThreshold
                    || peakGroupScores.get(ScoreType.XcorrShape.getName(), scoreTypes) < shapeScoreThreshold) {
                continue;
            }
            peakGroupScores.setRt(peakGroupFeature.getApexRt());
            peakGroupScoresList.add(peakGroupScores);
        }

        if (peakGroupScoresList.size() == 0) {
            return;
        }

        dataDO.setScoreList(peakGroupScoresList);
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
        List<PeakGroupScores> peakGroupScoresList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peakGroupList.getList();
        HashMap<String, Double> normedLibIntMap = peakGroupList.getNormedIntMap();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            PeakGroupScores peakGroupScores = new PeakGroupScores(types.size());
            chromatographicScorer.calculateChromatographicScores(peakGroupFeature, normedLibIntMap, peakGroupScores, types);
            if (peakGroupScores.get(ScoreType.XcorrShape.getName(), types) < 0.6) {
                continue;
            }
            peakGroupScores.setRt(peakGroupFeature.getApexRt());
            peakGroupScoresList.add(peakGroupScores);
        }

        if (peakGroupScoresList.size() == 0) {
            return;
        }

        data.setScoreList(peakGroupScoresList);
    }

    /**
     * 计算最好的峰的总分,并且加上鉴定态
     *
     * @param data
     * @param overview
     * @return
     */
    public DataSumDO calcBestTotalScore(DataDO data, OverviewDO overview) {
        DataSumDO dataSum = null;
        if (data.getScoreList() == null) {
            return null;
        }

        PeptideScores ps = new PeptideScores(data);

        List<String> scoreTypes = overview.getParams().getMethod().getScore().getScoreTypes();
        lda.score(ps, overview.getWeights(), scoreTypes);
        double bestTotalScore = -1d;
        Double bestRt = null;
        int bestIndex = 0;
        for (int i = 0; i < ps.getScoreList().size(); i++) {
            Double currentTotalScore = ps.getScoreList().get(i).get(ScoreType.WeightedTotalScore, scoreTypes);
            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
                bestIndex = i;
                bestTotalScore = currentTotalScore;
                bestRt = ps.getScoreList().get(i).getRt();
            }
        }
        dataSum = new DataSumDO();
        if (bestTotalScore > overview.getMinTotalScore()) {
            dataSum.setStatus(IdentifyStatus.SUCCESS.getCode());
        } else {
            dataSum.setStatus(IdentifyStatus.FAILED.getCode());
        }
        dataSum.setSum(ps.getScoreList().get(bestIndex).getIntensitySum());
        dataSum.setRealRt(bestRt);
        dataSum.setTotalScore(bestTotalScore);
        return dataSum;
    }

    public PeakGroupScores getBestTargetPeak(DataDO data, String typeName) {
        if (data.getScoreList() == null) {
            return null;
        }

        PeptideScores ps = new PeptideScores(data);

        double bestTotalScore = -1d;
        PeakGroupScores bestPeak = null;
        for (int i = 0; i < ps.getScoreList().size(); i++) {
            Double currentTotalScore = ps.getScoreList().get(i).get(typeName, ScoreType.getAllTypesName());
            if (currentTotalScore != null && currentTotalScore > bestTotalScore) {
                bestTotalScore = currentTotalScore;
                bestPeak = ps.getScoreList().get(i);
            }
        }

        return bestPeak;
    }
}
