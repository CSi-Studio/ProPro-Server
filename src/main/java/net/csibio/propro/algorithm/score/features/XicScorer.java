package net.csibio.propro.algorithm.score.features;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.utils.MathUtil;
import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * xcorr_coelution_score 互相关偏移的mean + std
 * weighted_coelution_score 带权重的相关偏移sum
 * xcorr_shape_score 互相关序列最大值的平均值
 * weighted_xcorr_shape 带权重的互相关序列最大值的平均值
 * log_sn_score log(距离ApexRt最近点的stn值之和)
 * var_intensity_score 同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
 */
@Component("chromatographicScorer")
@Slf4j
public class XicScorer {

    /**
     * xcorrCoelutionScore
     * xcorrCoelutionScoreWeighted
     * xcorrShapeScore
     * xcorrShapeScoreWeighted
     *
     * @param peakGroup list of features in selected mrmfeature
     */
    public void calcXICScores(PeakGroup peakGroup, HashMap<String, Double> normedLibIntMap, List<String> scoreTypes) {
        Table<Integer, Integer, Double[]> xcorrMatrix = initializeXCorrMatrix(peakGroup);

        List<Double> normedLibIntList = new ArrayList<>(normedLibIntMap.values());
        List<Integer> deltas = new ArrayList<>();
        List<Double> deltasWeighted = new ArrayList<>();
        List<Double> intensities = new ArrayList<>();
        List<Double> intensitiesWeighted = new ArrayList<>();
        Double[] value;
        int maxIndex;
        int size = peakGroup.getIonHullInt().size();
        for (int i = 0; i < size; i++) {
            value = xcorrMatrix.get(i, i);
            if (value == null) {
                log.error("Peak Group Intensity List is null!!!");
                continue;
            }
            maxIndex = MathUtil.findMaxIndex(value);
            int midIndex = (value.length - 1) / 2;
            deltasWeighted.add(FastMath.abs(maxIndex - midIndex) * normedLibIntList.get(i) * normedLibIntList.get(i));
            intensitiesWeighted.add(value[maxIndex] * normedLibIntList.get(i) * normedLibIntList.get(i));
            for (int j = i; j < size; j++) {
                value = xcorrMatrix.get(i, j);
                maxIndex = MathUtil.findMaxIndex(value);
                deltas.add(Math.abs(maxIndex - midIndex)); //first: maxdelay //delta: 偏移量
                intensities.add(value[maxIndex]);//value[max] 吻合系数
                if (j != i) {
                    deltasWeighted.add(Math.abs(maxIndex - midIndex) * normedLibIntList.get(i) * normedLibIntList.get(j) * 2d);
                    intensitiesWeighted.add(value[maxIndex] * normedLibIntList.get(i) * normedLibIntList.get(j) * 2d);
                }
            }
        }
        double sumDelta = 0.0d, sumDeltaWeighted = 0.0d, sumIntensity = 0.0d, sumIntensityWeighted = 0.0d;
        for (int i = 0; i < deltas.size(); i++) {
            sumDelta += deltas.get(i);
            sumDeltaWeighted += deltasWeighted.get(i);
            sumIntensity += intensities.get(i);
            sumIntensityWeighted += intensitiesWeighted.get(i);
        }
        double meanDelta = sumDelta / deltas.size();
        double meanIntensity = sumIntensity / intensities.size();
        sumDelta = 0;
        for (int delta : deltas) {
            sumDelta += (delta - meanDelta) * (delta - meanDelta);
        }
        //TODO WRM 这里可能会出现deltas.size()==1的情况
        double stdDelta = 0d;
        if (deltas.size() != 1) {
            stdDelta = Math.sqrt(sumDelta / (deltas.size() - 1));
        }
        if (scoreTypes.contains(ScoreType.CorrCoe.getName())) {
            peakGroup.put(ScoreType.CorrCoe.getName(), meanDelta + stdDelta, scoreTypes); //时间偏差
        }
        if (scoreTypes.contains(ScoreType.CorrCoeW.getName())) {
            peakGroup.put(ScoreType.CorrCoeW.getName(), sumDeltaWeighted, scoreTypes);
        }
        if (scoreTypes.contains(ScoreType.CorrShape.getName())) {
            peakGroup.put(ScoreType.CorrShape.getName(), meanIntensity, scoreTypes); // 平均的吻合程度--> 新的吻合系数
        }
        if (scoreTypes.contains(ScoreType.CorrShapeW.getName())) {
            peakGroup.put(ScoreType.CorrShapeW.getName(), sumIntensityWeighted, scoreTypes);
        }
    }

    public void calculateLogSnScore(PeakGroup peakGroup, List<String> scoreTypes) {
        //logSnScore
        // log(mean of Apex sn s)
        double snScore = peakGroup.getSignalToNoiseSum();
        snScore /= peakGroup.getIonIntensity().size();
        if (snScore < 1) {
            peakGroup.put(ScoreType.LogSn.getName(), 0d, scoreTypes);
        } else {
            peakGroup.put(ScoreType.LogSn.getName(), FastMath.log(snScore), scoreTypes);
        }
    }


    /**
     * Get the XCorrMatrix with run Features
     * 对于一个 mrmFeature，算其中 chromatogramFeature 的 xcorrMatrix
     *
     * @param peakGroup features in mrmFeature
     *                  HullInt: redistributed chromatogram in range of (peptideRef constant) leftRt and rightRt
     * @return Table<Integer, Integer, Float [ ]> xcorrMatrix
     */
    private Table<Integer, Integer, Double[]> initializeXCorrMatrix(PeakGroup peakGroup) {
        List<Double[]> intensityList = new ArrayList<>(peakGroup.getIonHullInt().values());
        int listLength = intensityList.size();
        Table<Integer, Integer, Double[]> xcorrMatrix = HashBasedTable.create();
        double[] intensityi, intensityj;
        HashMap<Integer, double[]> standardizeDataMap = new HashMap<>();
        for (int i = 0; i < listLength; i++) {
            standardizeDataMap.put(i, MathUtil.standardizeData(intensityList.get(i)));
        }
        for (int i = 0; i < listLength; i++) {
            for (int j = i; j < listLength; j++) {
                intensityi = standardizeDataMap.get(i);
                intensityj = standardizeDataMap.get(j);
                xcorrMatrix.put(i, j, calculateCrossCorrelation(intensityi, intensityj));
            }
        }
        return xcorrMatrix;
    }

    /**
     * xcorrMatrix的意义：sum(反斜向的元素)/data.length(3)
     * 0   1   2
     * 0   |0  |1  |2
     * 1   |-1 |0  |1
     * 2   |-2 |-1 |0
     *
     * @param data1 chromatogram feature
     * @param data2 the same length as data1
     * @return value of xcorrMatrix element
     */
    private Double[] calculateCrossCorrelation(double[] data1, double[] data2) {
        int maxDelay = data1.length;
        Double[] output = new Double[maxDelay * 2 + 1];
        double sxy;
        int j;
        for (int delay = -maxDelay; delay <= maxDelay; delay++) {
            sxy = 0;
            for (int i = 0; i < maxDelay; i++) {
                j = i + delay;
                if (j < 0 || j >= maxDelay) {
                    continue;
                }
                sxy += (data1[i] * data2[j]);
            }
            output[delay + maxDelay] = sxy / maxDelay;
        }
        return output;
    }

}
