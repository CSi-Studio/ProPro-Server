package net.csibio.propro.algorithm.score;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.peak.*;
import net.csibio.propro.algorithm.score.features.*;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.peptide.SimplePeptide;
import net.csibio.propro.domain.bean.score.FeatureScores;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeptideFeature;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.FeatureUtil;
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

    public void scoreForOne(ExperimentDO exp, DataDO dataDO, SimplePeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {

        if (dataDO.getIntensityMap() == null || dataDO.getIntensityMap().size() <= peptide.getFragments().size() / 2) {
            dataDO.setStatus(IdentifyStatus.NO_FIT.getCode());
            return;
        }
//        dataDO.setIsUnique(peptide.getIsUnique());

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(dataDO, peptide.buildIntensityMap(), params.getMethod().getIrt().getSs());
        if (!peptideFeature.isFeatureFound()) {
            dataDO.setStatus(IdentifyStatus.FAILED.getCode());
            log.info("肽段没有被选中的特征：PeptideRef: " + dataDO.getPeptideRef());
            return;
        }
        List<FeatureScores> featureScoresList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peptideFeature.getPeakGroupList();
        HashMap<String, Double> normedLibIntMap = peptideFeature.getNormedLibIntMap();
        HashMap<String, Float> productMzMap = new HashMap<>();
        HashMap<String, Integer> productChargeMap = new HashMap<>();

        for (String cutInfo : dataDO.getMzMap().keySet()) {
            try {
                if (cutInfo.contains("^")) {
                    String temp = cutInfo;
                    if (cutInfo.contains("[")) {
                        temp = cutInfo.substring(0, cutInfo.indexOf("["));
                    }
                    if (temp.contains("i")) {
                        temp = temp.replace("i", "");
                    }
                    productChargeMap.put(cutInfo, Integer.parseInt(temp.split("\\^")[1]));
                } else {
                    productChargeMap.put(cutInfo, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.info("cutInfo:" + cutInfo + ";data:" + JSON.toJSONString(dataDO));
            }

            float mz = dataDO.getMzMap().get(cutInfo);
            productMzMap.put(cutInfo, mz);
        }

        HashMap<Integer, String> unimodHashMap = peptide.getUnimodMap();
        String sequence = peptide.getSequence();

        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            FeatureScores featureScores = new FeatureScores(params.getMethod().getScore().getScoreTypes().size());
            chromatographicScorer.calculateChromatographicScores(peakGroupFeature, normedLibIntMap, featureScores, params.getMethod().getScore().getScoreTypes());
            Double shapeScore = featureScores.get(ScoreType.XcorrShape, params.getMethod().getScore().getScoreTypes());
            Double shapeScoreWeighted = featureScores.get(ScoreType.XcorrShapeWeighted, params.getMethod().getScore().getScoreTypes());
            if (!dataDO.getDecoy()
                    && ((shapeScoreWeighted != null && shapeScoreWeighted < params.getMethod().getQuickFilter().getMinShapeWeightScore())
                    || (shapeScore != null && shapeScore < params.getMethod().getQuickFilter().getMinShapeScore()))) {
                continue;
            }
            //根据RT时间和前体m/z获取最近的一个原始谱图
            if (params.getMethod().getScore().isDiaScores()) {
                MzIntensityPairs mzIntensityPairs = blockIndexService.getNearestSpectrumByRt(rtMap, peakGroupFeature.getApexRt());
                if (mzIntensityPairs != null) {
                    float[] spectrumMzArray = mzIntensityPairs.getMzArray();
                    float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();
                    if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.IsotopeCorrelationScore.getName()) || params.getMethod().getScore().getScoreTypes().contains(ScoreType.IsotopeOverlapScore.getName())) {
                        diaScorer.calculateDiaIsotopeScores(peakGroupFeature, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, featureScores, params.getMethod().getScore().getScoreTypes());
                    }
                    if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.BseriesScore.getName()) || params.getMethod().getScore().getScoreTypes().contains(ScoreType.YseriesScore.getName())) {
                        diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, featureScores, params.getMethod().getScore().getScoreTypes());
                    }
                    diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, featureScores, params.getMethod().getScore().getScoreTypes());

                }
            }
            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.LogSnScore.getName())) {
                chromatographicScorer.calculateLogSnScore(peakGroupFeature, featureScores, params.getMethod().getScore().getScoreTypes());
            }

            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.IntensityScore.getName())) {
                libraryScorer.calculateIntensityScore(peakGroupFeature, featureScores, params.getMethod().getScore().getScoreTypes());
            }

            libraryScorer.calculateLibraryScores(peakGroupFeature, normedLibIntMap, featureScores, params.getMethod().getScore().getScoreTypes());
            if (params.getMethod().getScore().getScoreTypes().contains(ScoreType.NormRtScore.getName())) {
                libraryScorer.calculateNormRtScore(peakGroupFeature, exp.getIrt().getSi(), dataDO.getLibRt(), featureScores, params.getMethod().getScore().getScoreTypes());
            }
            swathLDAScorer.calculateSwathLdaPrescore(featureScores, params.getMethod().getScore().getScoreTypes());
            featureScores.setRt(peakGroupFeature.getApexRt());
            featureScores.setRtRangeFeature(FeatureUtil.toString(peakGroupFeature.getBestLeftRt(), peakGroupFeature.getBestRightRt()));
            featureScores.setIntensitySum(peakGroupFeature.getPeakGroupInt());
            featureScores.setFragIntFeature(FeatureUtil.toString(peakGroupFeature.getIonIntensity()));
            featureScoresList.add(featureScores);
        }

        if (featureScoresList.size() == 0) {
            dataDO.setStatus(IdentifyStatus.NO_FIT.getCode());
            return;
        }

        dataDO.setFeatureScoresList(featureScoresList);
    }

    public void strictScoreForOne(DataDO dataDO, SimplePeptide peptide, double shapeScoreThreshold) {
        if (dataDO.getIntensityMap() == null || dataDO.getIntensityMap().size() < peptide.getFragments().size()) {
            dataDO.setStatus(IdentifyStatus.NO_FIT.getCode());
            return;
        }

        SigmaSpacing ss = SigmaSpacing.create();
        PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(dataDO, peptide.buildIntensityMap(), ss);
        if (!peptideFeature.isFeatureFound()) {
            return;
        }
        List<FeatureScores> featureScoresList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peptideFeature.getPeakGroupList();
        HashMap<String, Double> normedLibIntMap = peptideFeature.getNormedLibIntMap();
        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            FeatureScores featureScores = new FeatureScores(2);
            List<String> scoreTypes = new ArrayList<>();
            scoreTypes.add(ScoreType.XcorrShape.getName());
            scoreTypes.add(ScoreType.XcorrShapeWeighted.getName());
            chromatographicScorer.calculateChromatographicScores(peakGroupFeature, normedLibIntMap, featureScores, scoreTypes);
            if (featureScores.get(ScoreType.XcorrShapeWeighted.getName(), scoreTypes) < shapeScoreThreshold
                    || featureScores.get(ScoreType.XcorrShape.getName(), scoreTypes) < shapeScoreThreshold) {
                continue;
            }
            featureScores.setRt(peakGroupFeature.getApexRt());
            featureScoresList.add(featureScores);
        }

        if (featureScoresList.size() == 0) {
            return;
        }

        dataDO.setFeatureScoresList(featureScoresList);
    }

}
