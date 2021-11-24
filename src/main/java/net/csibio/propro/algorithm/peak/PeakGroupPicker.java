package net.csibio.propro.algorithm.peak;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.domain.bean.common.DoublePair;
import net.csibio.propro.domain.bean.data.RtIntensityPairsDouble;
import net.csibio.propro.domain.bean.data.UnSearchPeakGroup;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
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

import java.util.*;
import java.util.stream.Collectors;

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
     * 1）找出pickedChrom下的最高峰，得到对应rt和rtLeft、rtRight
     * 2）将pickedChrom最高峰intensity设为0
     * 3）将最高峰对应的chromatogram设为masterChromatogram
     * 4）对每个chromatogram映射到masterChromatogram，得到usedChromatogram
     * 5）对usedChromatogram求feature
     * 6）同时累计所有chromatogram（isDetecting）的intensity为totalXIC
     *
     * @param unSearchPeakGroup origin chromatogram
     * @param ionPeaks          maxPeaks picked and recalculated
     * @param ionPeakParams     left right borders
     *                          totalXic : intensity sum of all chromatogram of peptideRef(not rastered and all interval)
     *                          HullPoints : rt intensity pairs of rastered chromatogram between rtLeft, rtRight;
     *                          RunFeature::intensity: intensity sum of hullPoints' intensity
     * @return list of mrmFeature (mrmFeature is list of chromatogram feature)
     */
    public List<PeakGroup> findFeatures(UnSearchPeakGroup unSearchPeakGroup, HashMap<String, RtIntensityPairsDouble> ionPeaks, HashMap<String, List<IonPeak>> ionPeakParams, HashMap<String, double[]> noise1000Map) {

        //totalXIC
        double totalXic = 0.0d;
        for (Double[] intensityTmp : unSearchPeakGroup.getIntensitiesMap().values()) {
            for (double intensity : intensityTmp) {
                totalXic += intensity;
            }
        }

        List<PeakGroup> peakGroupList = new ArrayList<>();
        while (true) {
            PeakGroup peakGroup = new PeakGroup();
            Pair<String, Integer> maxPeakLocation = findLargestPeak(ionPeaks);
            if (maxPeakLocation.getKey().equals("null")) {
                break;
            }
            String maxCutInfo = maxPeakLocation.getKey();
            int maxIndex = maxPeakLocation.getValue();
            int leftIndex = ionPeakParams.get(maxCutInfo).get(maxIndex).getLeftRtIndex();
            int rightIndex = ionPeakParams.get(maxCutInfo).get(maxIndex).getRightRtIndex();
            double apexRt = ionPeaks.get(maxCutInfo).getRtArray()[maxIndex];
            double bestLeft = unSearchPeakGroup.getRtArray()[leftIndex];
            double bestRight = unSearchPeakGroup.getRtArray()[rightIndex];

            peakGroup.setApexRt(apexRt);
            peakGroup.setLeftRt(bestLeft);
            peakGroup.setRightRt(bestRight);

            RtIntensityPairsDouble rtInt = ionPeaks.get(maxCutInfo);
            rtInt.getIntensityArray()[maxIndex] = 0.0d;

            removeOverlappingPeakGroups(ionPeaks, leftIndex, rightIndex, ionPeakParams);

            Double[] rtArray = unSearchPeakGroup.getRtArray();

            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, rightIndex - leftIndex + 1);
            int maxSpectrumIndex = PeakUtil.findNearestIndex(rasteredRt, apexRt) + leftIndex;
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            for (String cutInfo : ionPeakParams.keySet()) {
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                //离子峰
                Double[] rasteredInt = new Double[rightIndex - leftIndex + 1];
                System.arraycopy(intArray, leftIndex, rasteredInt, 0, rightIndex - leftIndex + 1);
                ionHullInt.put(cutInfo, rasteredInt);
                //peakGroup强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
                peakGroupInt += ionIntTemp;
                //离子峰强度
                ionIntensity.put(cutInfo, ionIntTemp);
                //信噪比
                signalToNoiseSum += noise1000Map.get(cutInfo)[maxSpectrumIndex];
            }
//            List<Double> ionIntList = new ArrayList<>(ionIntensity.values());
            if (peakGroupInt == 0D) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
//            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroup.setMaxIon(maxCutInfo);
            peakGroup.setMaxIonIntensity(peakGroup.getIonIntensity().get(maxCutInfo));
            peakGroupList.add(peakGroup);
            if (peakGroupInt > 0 && peakGroupInt / totalXic < Constants.STOP_AFTER_INTENSITY_RATIO) {
                break;
            }
        }

        return peakGroupList;
    }

    public List<PeakGroup> findFeaturesNew(UnSearchPeakGroup unSearchPeakGroup, HashMap<String, RtIntensityPairsDouble> ionPeaks, HashMap<String, List<IonPeak>> ionPeakParams, HashMap<String, double[]> noise1000Map) {

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
        for (String cutInfo : ionPeakParams.keySet()) {
            for (IonPeak ionPeak : ionPeakParams.get(cutInfo)) {
                if (ionPeak.getIntensity() != 0) {
                    ionPeakPositionList.get(ionPeak.getApexRtIndex()).put(cutInfo, ionPeak);
                }
            }
        }
        for (int i = 1; i < peakDensity.length - 1; i++) {
//            peakDensity[i] = density(ionPeakPositionList, i);
            peakDensity[i] = ionPeakPositionList.get(i).size() + Constants.SIDE_PEAK_DENSITY * (ionPeakPositionList.get(i - 1).size() + ionPeakPositionList.get(i + 1).size());
        }
        peakDensity[0] = 0d;
        peakDensity[peakDensity.length - 1] = 0d;
        List<Integer> topIndex = getTopIndex(peakDensity, FastMath.round(ionPeakParams.size() * Constants.ION_PERCENT * 10) / 10d);

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
            double apexRt = ionPeaks.get(maxIon).getRtArray()[concateMap.get(maxIon).getIndex()];
            double bestLeft = unSearchPeakGroup.getRtArray()[leftIndex];
            double bestRight = unSearchPeakGroup.getRtArray()[rightIndex];

            peakGroup.setApexRt(apexRt);
            peakGroup.setLeftRt(bestLeft);
            peakGroup.setRightRt(bestRight);

            for (int j = 0; j < topIndex.size(); j++) {
                if (topIndex.get(j) <= rightIndex && topIndex.get(j) >= leftIndex) {
                    topIndex.set(j, 0);
                }
            }
            Double[] rtArray = unSearchPeakGroup.getRtArray();

            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, rightIndex - leftIndex + 1);
            int nearestRtIndex = PeakUtil.findNearestIndex(rasteredRt, apexRt);
            int maxSpectrumIndex = nearestRtIndex + leftIndex;
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            for (String cutInfo : ionPeakParams.keySet()) {
                //离子峰
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                //设定局部最大值（峰型控制备用）
                double localMaxIntensity = intArray[maxSpectrumIndex];
//                if(intArray[maxSpectrumIndex] < intArray[maxSpectrumIndex-1] && intArray[maxSpectrumIndex] < intArray[maxSpectrumIndex+1]){
//                    localMaxIntensity = FastMath.max(intArray[maxSpectrumIndex-1], intArray[maxSpectrumIndex+1]);
//                }
                localMaxIntensity = FastMath.max(intArray[maxSpectrumIndex - 1], FastMath.max(intArray[maxSpectrumIndex], intArray[maxSpectrumIndex + 1]));

                //获得峰值部分的Intensity序列
//                Double[] rasteredInt = new Double[rightIndex - leftIndex + 1];
//                System.arraycopy(intArray, leftIndex, rasteredInt, 0, rightIndex - leftIndex + 1);
                Double[] rasteredInt = filteredCopy(intArray, leftIndex, rightIndex, Double.MAX_VALUE);
                ionHullInt.put(cutInfo, rasteredInt);

                //离子峰强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
//                Double ionIntTemp = getIonIntensity(rasteredInt, localMaxIntensity * 2);
//                Double ionIntTemp = (intArray[maxSpectrumIndex]+1) * Math.min(maxSpectrumIndex - leftIndex, rightIndex - maxSpectrumIndex) * Constants.SQRT_2PI / 2d;
                peakGroupInt += ionIntTemp;
//                if (quantifyIons.contains(cutInfo)) {
//                    peakGroupInt += ionIntTemp;
//                }
                ionIntensity.put(cutInfo, ionIntTemp);

                //信噪比
                signalToNoiseSum += noise1000Map.get(cutInfo)[maxSpectrumIndex];
            }
            if (peakGroupInt == 0d) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
//            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroup.setMaxIon(maxIon);
            peakGroup.setMaxIonIntensity(peakGroup.getIonIntensity().get(maxIon));
            peakGroupList.add(peakGroup);
        }
        return peakGroupList;
    }

    public List<PeakGroup> findPeakGroups(UnSearchPeakGroup unSearchPeakGroup) {

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
            boolean hit = false;
            List<DoublePair> pairs = unSearchPeakGroup.getMaxPeaks4IonsHigh();
            for (int k = 0; k < pairs.size(); k++) {
                if (pairs.get(k).left() <= bestRight && pairs.get(k).left() >= bestLeft) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                continue;
            }
            for (int j = 0; j < topIndex.size(); j++) {
                if (topIndex.get(j) <= rightIndex && topIndex.get(j) >= leftIndex) {
                    topIndex.set(j, 0);
                }
            }
            Double[] rtArray = unSearchPeakGroup.getRtArray();

            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, rightIndex - leftIndex + 1);
            int nearestRtIndex = PeakUtil.findNearestIndex(rasteredRt, apexRt);
            int maxSpectrumIndex = nearestRtIndex + leftIndex;
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
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

                //信噪比
//                signalToNoiseSum += unSearchPeakGroup.getNoise1000Map().get(cutInfo)[maxSpectrumIndex];
            }
            if (peakGroupInt == 0d) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
//            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroup.setMaxIon(maxIon);
            peakGroup.setMaxIonIntensity(peakGroup.getIonIntensity().get(maxIon));
            peakGroupList.add(peakGroup);
        }

        return peakGroupList;
    }

    public List<PeakGroup> findPeakGroupsV2(UnSearchPeakGroup unSearchPeakGroup) {

        int[] ions300 = unSearchPeakGroup.getIonsHigh();
        int[] ions50 = unSearchPeakGroup.getIonsLow();
        Double[] ions300Smooth = unSearchPeakGroup.getIonsHighSmooth();
        Double[] rtArray = unSearchPeakGroup.getRtArray();
        Set<String> max6Ions = unSearchPeakGroup.getCoord().getFragments().subList(0, 6).stream().map(FragmentInfo::getCutInfo).collect(Collectors.toSet());
        List<PeakGroup> peakGroupList = new ArrayList<>();
        List<DoublePair> pairs = unSearchPeakGroup.getMaxPeaks4IonsHigh();

        while (true) {
            PeakGroup peakGroup = new PeakGroup();
            Pair<String, Integer> maxPeakLocation = findLargestPeak(unSearchPeakGroup.getMaxPeaks4Ions());
            if (maxPeakLocation.getKey().equals("null")) {
                break;
            }
            String maxCutInfo = maxPeakLocation.getKey();
            int maxIndex = maxPeakLocation.getValue();
            int leftIndex = unSearchPeakGroup.getPeaks4Ions().get(maxCutInfo).get(maxIndex).getLeftRtIndex();
            int rightIndex = unSearchPeakGroup.getPeaks4Ions().get(maxCutInfo).get(maxIndex).getRightRtIndex();
            double apexRt = unSearchPeakGroup.getMaxPeaks4Ions().get(maxCutInfo).getRtArray()[maxIndex];
            double bestLeftRt = rtArray[leftIndex];
            double bestRightRt = rtArray[rightIndex];

            peakGroup.setApexRt(apexRt);
            peakGroup.setLeftRt(bestLeftRt);
            peakGroup.setRightRt(bestRightRt);

            //计算完毕,准备抛弃
            RtIntensityPairsDouble rtInt = unSearchPeakGroup.getMaxPeaks4Ions().get(maxCutInfo);
            rtInt.getIntensityArray()[maxIndex] = 0.0d;

            removeOverlappingPeakGroups(unSearchPeakGroup.getMaxPeaks4Ions(), leftIndex, rightIndex, unSearchPeakGroup.getPeaks4Ions());

            //如果PeakGroup不在IonCount最优峰范围内,直接忽略
            boolean hit = false;

            int bestRtIndex = -1;
            for (int k = 0; k < pairs.size(); k++) {
                double ionsApexRt = pairs.get(k).left();
                if (ionsApexRt <= bestRightRt && ionsApexRt >= bestLeftRt) {
                    hit = true;
                    peakGroup.setApexRt(ionsApexRt);
                    int maxIons300 = -1;
                    //搜索范围内Ions300最高的点,如果存在两个相同最高点,那么比对他们的Ions50的点,如果Ions50的点仍然相同,那么选择离ApexRT更近的点
                    for (int j = leftIndex; j <= rightIndex; j++) {
                        if (ions300[j] > maxIons300) {
                            maxIons300 = ions300[j];
                            bestRtIndex = j;
                        } else if (ions300[j] == maxIons300) { //如果ions300的值相同,则比较ions50的值
                            if (Math.abs(rtArray[bestRtIndex] - apexRt) > Math.abs(rtArray[j] - apexRt)) {
                                maxIons300 = ions300[j];
                                bestRtIndex = j;
                            }
                        }
                    }
                    if (bestRtIndex == rightIndex && rightIndex < unSearchPeakGroup.getFloatRtArray().length - 1) {
                        rightIndex++;
                    }
                    if (bestRtIndex == leftIndex && leftIndex != 0) {
                        leftIndex--;
                    }
                    peakGroup.setIonsLow(ions50[bestRtIndex]); //特别注意,最高点的碎片值使用的是Ions50而不是Ions300,Ions300仅用于选峰
                    peakGroup.setSelectedRt(unSearchPeakGroup.getRtArray()[bestRtIndex]);
                    peakGroup.setLeftRt(rtArray[leftIndex]);
                    peakGroup.setRightRt(rtArray[rightIndex]);
                    break;
                }
            }
            if (!hit) {
                continue;
            }

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
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            for (String cutInfo : unSearchPeakGroup.getPeaks4Ions().keySet()) {
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                //库中排名前3的碎片离子在最高峰处的信号不能为0,否则直接忽略
                if (max6Ions.contains(cutInfo)) {
                    if (intArray[bestRtIndex] == 0d) {
//                        log.info("精彩的判定:" + unSearchPeakGroup.getCoord().getPeptideRef());
                        hit = false;
                        break;
                    }
                }
                //离子峰
                Double[] rasteredInt = new Double[peakLength];
                System.arraycopy(intArray, leftIndex, rasteredInt, 0, peakLength);
                ionHullInt.put(cutInfo, rasteredInt);
                //peakGroup强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
                peakGroupInt += ionIntTemp;
                //离子峰强度
                ionIntensity.put(cutInfo, ionIntTemp);
                //信噪比
//                signalToNoiseSum += unSearchPeakGroup.getNoise1000Map().get(cutInfo)[bestRtIndex];
            }

            if (peakGroupInt == 0D || !hit) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
//            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroup.setMaxIon(maxCutInfo);
            peakGroup.setMaxIonIntensity(peakGroup.getIonIntensity().get(maxCutInfo));
            peakGroupList.add(peakGroup);
            if (peakGroupInt > 0 && peakGroupInt / totalXic < Constants.STOP_AFTER_INTENSITY_RATIO) {
                break;
            }
        }

        return peakGroupList;
    }

    //直接按照IC算法进行选峰
    public List<PeakGroup> findPeakGroupsV3(UnSearchPeakGroup unSearchPeakGroup) {

        int[] ionsHigh = unSearchPeakGroup.getIonsHigh();
        int[] ionsLow = unSearchPeakGroup.getIonsLow();
        Double[] ions300Smooth = unSearchPeakGroup.getIonsHighSmooth();
        Double[] rtArray = unSearchPeakGroup.getRtArray();
        Set<String> allIons = unSearchPeakGroup.getCoord().getFragments().stream().map(FragmentInfo::getCutInfo).collect(Collectors.toSet());
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

            //允许的最大波动值
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
            peakGroup.setSelectedRt(rtArray[apexRtIndex]);

            boolean hit = true;
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
            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            HashMap<String, Double> apexIonIntensity = new HashMap<>();
            Double peakGroupInt = 0D;
            double signalToNoiseSum = 0d;
            String maxCutInfo = null;
            double maxCutInfoIntensity = -1d;
            for (String cutInfo : unSearchPeakGroup.getIntensitiesMap().keySet()) {
                Double[] intArray = unSearchPeakGroup.getIntensitiesMap().get(cutInfo);
                //库中排名前3的碎片离子在最高峰处的信号不能为0,否则直接忽略
                if (allIons.contains(cutInfo)) {
                    if (intArray[apexRtIndex] == 0d) {
//                        log.info("精彩的判定:" + unSearchPeakGroup.getCoord().getPeptideRef());
                        hit = false;
                        break;
                    }
                    if (intArray[apexRtIndex] > maxCutInfoIntensity) {
                        maxCutInfoIntensity = intArray[apexRtIndex];
                        maxCutInfo = cutInfo;
                    }
                }
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

            if (peakGroupInt == 0D || !hit) {
                continue;
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIntensitySum(peakGroupInt);
            peakGroup.setTic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
            peakGroup.setApexIonsIntensity(apexIonIntensity);
            peakGroup.setSignalToNoiseSum(signalToNoiseSum);
            peakGroup.setMaxIon(maxCutInfo);
            peakGroup.setMaxIonIntensity(maxCutInfoIntensity);
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