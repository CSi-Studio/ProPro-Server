package net.csibio.propro.algorithm.peak;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.domain.bean.common.DoublePair;
import net.csibio.propro.domain.bean.data.RtIntensityPairsDouble;
import net.csibio.propro.domain.bean.data.UnSearchPeakGroup;
import net.csibio.propro.domain.bean.score.IonPeak;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.utils.ArrayUtil;
import net.csibio.propro.utils.MathUtil;
import net.csibio.propro.utils.PeakUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 22：42
 */
@Slf4j
@Component("peakGroupPicker")
public class PeakGroupPicker {

    @Autowired
    BlockIndexService blockIndexService;

    /**
     * 经典版本的选峰算法
     * 1）找出pickedChrom下的最高峰，得到对应rt和rtLeft、rtRight
     * 2）将pickedChrom最高峰intensity设为0
     * 3）将最高峰对应的chromatogram设为masterChromatogram
     * 4）对每个chromatogram映射到masterChromatogram，得到usedChromatogram
     * 5）对usedChromatogram求feature
     * 6）同时累计所有chromatogram（isDetecting）的intensity为totalXIC
     *
     * @param unSearchPeakGroup
     * @return
     */
    public List<PeakGroup> findPeakGroupsClassic(UnSearchPeakGroup unSearchPeakGroup) {

        //totalXIC
        double totalXic = 0.0d;

        for (String cutInfo : unSearchPeakGroup.getIntensitiesMap().keySet()) {
            Double[] intensityTmp = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
            for (double intensity : intensityTmp) {
                totalXic += intensity;
            }
        }

        //new function
        List<PeakGroup> peakGroupList = new ArrayList<>();
        Double[] peakDensity = new Double[unSearchPeakGroup.getRtArray().length];
        List<HashMap<String, IonPeak>> ionPeakPositionList = new ArrayList<>();
        for (int i = 0; i < unSearchPeakGroup.getRtArray().length; i++) {
            ionPeakPositionList.add(new HashMap<>());
        }
        for (String cutInfo : unSearchPeakGroup.getPeaks4Ions().keySet()) {
            for (IonPeak ionPeak : unSearchPeakGroup.getPeaks4Ions().get(cutInfo)) {
                if (ionPeak.getIntensity() != 0) {
                    ionPeakPositionList.get(ionPeak.getApexRtIndex()).put(cutInfo, ionPeak);
                }
            }
        }
        for (int i = 1; i < peakDensity.length - 1; i++) {
            peakDensity[i] = ionPeakPositionList.get(i).size() + Constants.SIDE_PEAK_DENSITY * (ionPeakPositionList.get(i - 1).size() + ionPeakPositionList.get(i + 1).size());
        }
        peakDensity[0] = 0d;
        peakDensity[peakDensity.length - 1] = 0d;
        List<Integer> topIndex = getTopIndex(peakDensity, FastMath.round(unSearchPeakGroup.getPeaks4Ions().size() * Constants.ION_PERCENT * 10) / 10d);
        int[] ionsLow = unSearchPeakGroup.getIonsLow();
        int[] ionsHigh = unSearchPeakGroup.getIonsHigh();
        for (int i = 0; i < topIndex.size(); i++) {
            int maxIndex = topIndex.get(i);
            if (maxIndex == 0) {
                continue;
            }
            //合并周围结果
            PeakGroup peakGroup = new PeakGroup();
            HashMap<String, IonPeak> concateMap = concatenate(ionPeakPositionList, maxIndex, 1);
            String maxIon = getMaxIntensityIndex(concateMap);
            int leftIndex = concateMap.get(maxIon).getLeftRtIndex();
            int rightIndex = concateMap.get(maxIon).getRightRtIndex();
            double apexRt = unSearchPeakGroup.getMaxPeaks4Ions().get(maxIon).getRtArray()[concateMap.get(maxIon).getIndex()];
            double bestLeft = unSearchPeakGroup.getRtArray()[leftIndex];
            double bestRight = unSearchPeakGroup.getRtArray()[rightIndex];

            peakGroup.setApexRt(apexRt);
            peakGroup.setLeftRt(bestLeft);
            peakGroup.setRightRt(bestRight);

            //如果PeakGroup不在IonCount最优峰范围内,直接忽略
            for (int j = 0; j < topIndex.size(); j++) {
                if (topIndex.get(j) <= rightIndex && topIndex.get(j) >= leftIndex) {
                    topIndex.set(j, 0);
                }
            }
            Double[] rtArray = unSearchPeakGroup.getRtArray();

            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, rightIndex - leftIndex + 1);
            Double[] ms1Ints = new Double[rightIndex - leftIndex + 1];
            Double[] selfInts = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(unSearchPeakGroup.getMs1Ints(), leftIndex, ms1Ints, 0, rightIndex - leftIndex + 1);
            System.arraycopy(unSearchPeakGroup.getSelfInts(), leftIndex, selfInts, 0, rightIndex - leftIndex + 1);
            int selectedRtIndex = PeakUtil.findNearestIndex(rasteredRt, apexRt);
            int maxSpectrumIndex = selectedRtIndex + leftIndex;
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            HashMap<String, Double> apexIonIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            for (String cutInfo : unSearchPeakGroup.getPeaks4Ions().keySet()) {
                //离子峰
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                Double[] rasteredInt = filteredCopy(intArray, leftIndex, rightIndex, Double.MAX_VALUE);
                ionHullInt.put(cutInfo, rasteredInt);
                //离子峰强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
                peakGroupInt += ionIntTemp;
                ionIntensity.put(cutInfo, ionIntTemp);
                apexIonIntensity.put(cutInfo, intArray[maxSpectrumIndex]);
                //信噪比
                signalToNoiseSum += unSearchPeakGroup.getNoise1000Map().get(cutInfo)[maxSpectrumIndex];
            }

            String maxIonInLib = unSearchPeakGroup.getCoord().getFragments().get(0).getCutInfo();

            if (unSearchPeakGroup.getIntensitiesMap().get(maxIonInLib) == null) {//排除最强Ion碎片为空的峰
                continue;
            }
            Double maxIonIntensityInApex = unSearchPeakGroup.getIntensitiesMap().get(maxIonInLib)[maxSpectrumIndex];
            if (maxIonIntensityInApex == 0) {
                continue;
            }
            if (unSearchPeakGroup.getMs1Ints()[maxSpectrumIndex] < maxIonIntensityInApex) { //排除最高强度Ion小于ms1前体强度的峰
                continue;
            }
            if (unSearchPeakGroup.getSelfInts()[maxSpectrumIndex] != null && unSearchPeakGroup.getMs1Ints()[maxSpectrumIndex] < unSearchPeakGroup.getSelfInts()[maxSpectrumIndex]) { //排除self大于ms1前体强度的峰
                continue;
            }
            if (peakGroupInt == 0d) {
                continue;
            }
            peakGroup.setIonsHigh(ionsHigh[maxSpectrumIndex]);
            peakGroup.setIonsLow(ionsLow[maxSpectrumIndex]);
            peakGroup.setSelectedRt(unSearchPeakGroup.getRtArray()[maxSpectrumIndex]);
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setMs1Ints(ms1Ints);
            peakGroup.setSelfInts(selfInts);
            peakGroup.setMs1Sum(unSearchPeakGroup.getMs1Ints()[maxSpectrumIndex]);
            peakGroup.setIonIntensity(ionIntensity);
            peakGroup.setApexRt(apexRt);
            peakGroup.setApexIonsIntensity(apexIonIntensity);
            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroupList.add(peakGroup);
        }

        return peakGroupList;
    }

    //直接按照IC算法进行选峰
    public List<PeakGroup> findPeakGroupsByIonsCount(UnSearchPeakGroup unSearchPeakGroup) {

        int[] ionsHigh = unSearchPeakGroup.getIonsHigh();
        int[] ionsLow = unSearchPeakGroup.getIonsLow();
        Double[] rtArray = unSearchPeakGroup.getRtArray();
        List<PeakGroup> peakGroupList = new ArrayList<>();

        List<DoublePair> pairs = unSearchPeakGroup.getMaxPeaks4IonsHigh(); //所有的峰顶
        int maxIndex = ionsHigh.length - 1;
        for (DoublePair pair : pairs) {
            int apexRtIndex = ArrayUtil.binaryNearSearch(rtArray, pair.left());

            //如果搜到了两个极致
            if (apexRtIndex == 0 || apexRtIndex == maxIndex) {
                continue;
            }
            int leftIndex = apexRtIndex - 1;
            int rightIndex = apexRtIndex + 1;
            if (apexRtIndex > 1) {
                leftIndex--;
            }
            if (apexRtIndex < maxIndex - 1) {
                rightIndex++;
            }

            //允许的最大波动次数,如果强度最高峰强度大于6,则允许有一次波动
            int maxFluctuation = ionsHigh[apexRtIndex] >= 6 ? 1 : 0;

            while (true) {
                if (leftIndex == 0) {
                    break;
                }
                if (ionsHigh[leftIndex] == 0) {
                    break;
                }
                if (ionsHigh[leftIndex - 1] <= ionsHigh[leftIndex]) {
                    leftIndex--;
                } else if (ionsHigh[leftIndex - 1] - ionsHigh[leftIndex] == 1 && maxFluctuation == 1) {
                    leftIndex--;
                    maxFluctuation--;
                } else {
                    break;
                }
            }

            maxFluctuation = ionsHigh[apexRtIndex] >= 6 ? 1 : 0;
            while (true) {
                if (rightIndex == maxIndex) {
                    break;
                }
                if (ionsHigh[rightIndex] == 0) {
                    break;
                }
                if (ionsHigh[rightIndex + 1] <= ionsHigh[rightIndex]) {
                    rightIndex++;
                } else if (ionsHigh[rightIndex + 1] - ionsHigh[rightIndex] == 1 && maxFluctuation == 1) {
                    rightIndex++;
                    maxFluctuation--;
                } else {
                    break;
                }
            }

            //左右非对称峰型
            if (apexRtIndex - leftIndex != rightIndex - apexRtIndex) {

            }
            PeakGroup peakGroup = new PeakGroup(rtArray[leftIndex], rtArray[rightIndex]);
            peakGroup.setApexRt(pair.left());
            peakGroup.setIonsLow(ionsLow[apexRtIndex]);
            peakGroup.setIonsHigh(ionsHigh[apexRtIndex]);
            peakGroup.setSelectedRt(rtArray[apexRtIndex]);
            //totalXIC
            double totalXic = 0.0d;
            for (Double[] intensityTmp : unSearchPeakGroup.getIntensitiesMap().values()) {
                for (double intensity : intensityTmp) {
                    totalXic += intensity;
                }
            }
            int peakLength = rightIndex - leftIndex + 1;
            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[peakLength];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, peakLength);
            Double[] ms1Ints = new Double[peakLength];
            Double[] selfInts = new Double[peakLength];
            System.arraycopy(unSearchPeakGroup.getMs1Ints(), leftIndex, ms1Ints, 0, peakLength);
            System.arraycopy(unSearchPeakGroup.getSelfInts(), leftIndex, selfInts, 0, peakLength);
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            HashMap<String, Double> apexIonIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            for (String cutInfo : unSearchPeakGroup.getIntensitiesMap().keySet()) {
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                //离子峰
                Double[] rasteredInt = new Double[peakLength];
                System.arraycopy(intArray, leftIndex, rasteredInt, 0, peakLength);
                ionHullInt.put(cutInfo, rasteredInt);
                //peakGroup强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
                peakGroupInt += ionIntTemp;
                //离子峰强度
                ionIntensity.put(cutInfo, ionIntTemp);
                apexIonIntensity.put(cutInfo, intArray[apexRtIndex]);
                //信噪比
                signalToNoiseSum += unSearchPeakGroup.getNoise1000Map().get(cutInfo)[apexRtIndex];
            }
            String maxIonInLib = unSearchPeakGroup.getCoord().getFragments().get(0).getCutInfo();

            if (unSearchPeakGroup.getIntensitiesMap().get(maxIonInLib) == null) {//排除最强Ion碎片为空的峰
                continue;
            }
            Double maxIonIntensityInApex = unSearchPeakGroup.getIntensitiesMap().get(maxIonInLib)[apexRtIndex];
            if (maxIonIntensityInApex == 0) {
                continue;
            }
            if (unSearchPeakGroup.getMs1Ints()[apexRtIndex] < maxIonIntensityInApex) { //排除最高强度Ion大于ms1前体强度的峰
                continue;
            }
            if (unSearchPeakGroup.getSelfInts()[apexRtIndex] != null && unSearchPeakGroup.getMs1Ints()[apexRtIndex] < unSearchPeakGroup.getSelfInts()[apexRtIndex]) { //排除self大于ms1前体强度的峰
                continue;
            }
            if (peakGroupInt == 0D) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setMs1Ints(ms1Ints);
            peakGroup.setSelfInts(selfInts);
            peakGroup.setMs1Sum(unSearchPeakGroup.getMs1Ints()[apexRtIndex]);
            peakGroup.setIonIntensity(ionIntensity);
            peakGroup.setApexIonsIntensity(apexIonIntensity);
            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroupList.add(peakGroup);
        }

        return peakGroupList;
    }

    /**
     * 从maxPeak list中选取最大peak对应的index
     *
     * @param peaksCoord maxPeaks
     * @return list index, pairs index
     */
    private Pair<String, Integer> findLargestPeak(HashMap<String, RtIntensityPairsDouble> peaksCoord) {
        double largest = 0.0d;
        Pair<String, Integer> maxPeakLoc = Pair.of("null", -1);

        for (String cutInfo : peaksCoord.keySet()) {
            for (int i = 0; i < peaksCoord.get(cutInfo).getRtArray().length; i++) {
                if (peaksCoord.get(cutInfo).getIntensityArray()[i] > largest) {
                    largest = peaksCoord.get(cutInfo).getIntensityArray()[i];
                    maxPeakLoc = Pair.of(cutInfo, i);
                }
            }
        }
        return maxPeakLoc;
    }


    /**
     * 过滤区间内的峰值，也可以理解成：以已经选取的高峰划分peak group
     * <p>
     * 中心落在更高峰闭区间内的会被过滤掉
     * 边界落在更高峰开区间内的会被过滤掉
     *
     * @param ionPeaks
     * @param bestLeft      按从高到低顺序选择的最高峰的RT范围
     * @param bestRight     同上
     * @param ionPeakParams
     */
    private void removeOverlappingPeakGroups(HashMap<String, RtIntensityPairsDouble> ionPeaks, int bestLeft, int bestRight, HashMap<String, List<IonPeak>> ionPeakParams) {
        for (String cutInfo : ionPeaks.keySet()) {
            Double[] intensity = ionPeaks.get(cutInfo).getIntensityArray();
            for (int j = 0; j < intensity.length; j++) {
                if (intensity[j] <= 0d) {
                    continue;
                }
                try {
                    int mid = ionPeakParams.get(cutInfo).get(j).getApexRtIndex();
                    if (mid >= bestLeft && mid <= bestRight) {
                        intensity[j] = 0d;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int left = ionPeakParams.get(cutInfo).get(j).getLeftRtIndex();
                int right = ionPeakParams.get(cutInfo).get(j).getRightRtIndex();
                if ((left > bestLeft && left < bestRight) || (right > bestLeft && right < bestRight)) {
                    intensity[j] = 0d;
                }
            }
        }
    }

    private List<Integer> getTopIndex(Double[] value, double minIon) {
        Integer[] indexAfterSort = ArrayUtil.indexAfterSort(value);
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < value.length; i++) {
            int indexTemp = indexAfterSort[indexAfterSort.length - 1 - i];
            if (value[indexTemp] < minIon) {
                break;
            }
            index.add(indexTemp);
        }
        return index;
    }

    private String getMaxIntensityIndex(HashMap<String, IonPeak> ionPeakList) {
        double maxIntensity = -1d;
        String maxIon = "";
        for (String cutInfo : ionPeakList.keySet()) {
            if (ionPeakList.get(cutInfo).getIntensity() > maxIntensity) {
                maxIntensity = ionPeakList.get(cutInfo).getIntensity();
                maxIon = cutInfo;
            }
        }
        return maxIon;
    }

    private HashMap<String, IonPeak> concatenate(List<HashMap<String, IonPeak>> ionPeakList, int maxIndex, int range) {
        HashMap<String, IonPeak> result = new HashMap<>();
        for (int i = maxIndex - range; i <= maxIndex + range && i < ionPeakList.size(); i++) {
            for (String cutInfo : ionPeakList.get(i).keySet()) {
                if (!result.containsKey(cutInfo)) {
                    result.put(cutInfo, ionPeakList.get(i).get(cutInfo));
                } else if (result.get(cutInfo).getIntensity() < ionPeakList.get(i).get(cutInfo).getIntensity()) {
                    result.put(cutInfo, ionPeakList.get(i).get(cutInfo));
                }
            }
        }
        return result;
    }

    private double density(List<HashMap<String, IonPeak>> ionPeakList, int maxIndex) {
        HashSet<String> set = new HashSet<>();
        for (int i = maxIndex - 1; i <= maxIndex + 1; i++) {
            set.addAll(ionPeakList.get(i).keySet());
        }
        int midSize = ionPeakList.get(maxIndex).size();
        return midSize + Constants.SIDE_PEAK_DENSITY * (midSize - set.size());
    }

    private Double[] filteredCopy(Double[] array, int leftIndex, int rightIndex, double maxValue) {
        Double[] result = new Double[rightIndex - leftIndex + 1];
        for (int i = 0; i < result.length; i++) {
            if (array[leftIndex + i] <= maxValue) {
                result[i] = array[leftIndex + i];
            } else {
                result[i] = maxValue;
            }
        }
        return result;
    }

    private Double getIonIntensity(Double[] intArray, double localMax) {
        Double result = 0d;
        for (Double intensity : intArray) {
            if (intensity > localMax) {
                result += localMax;
            } else {
                result += intensity;
            }
        }
        return result;
    }
}
