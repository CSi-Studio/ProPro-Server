package net.csibio.propro.algorithm.peak;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.domain.bean.data.RtIntensityPairsDouble;
import net.csibio.propro.domain.bean.data.UnSearchPeakGroup;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.IonPeak;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.PeakGroupListWrapper;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.ArrayUtil;
import net.csibio.propro.utils.MathUtil;
import net.csibio.propro.utils.PeakUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 19-16
 */
@Slf4j
@Component("peakPicker")
public class PeakPicker {

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

    /**
     * @param data  XIC后的数据对象
     * @param coord 库肽段坐标
     * @param ss    sigma spacing
     * @return
     */
    public PeakGroupListWrapper searchByIonsShape(DataDO data, PeptideCoord coord, SigmaSpacing ss) {

        Map<String, Float> libIntMap = coord.buildIntensityMap();
        //将没有提取到信号的CutInfo过滤掉,同时将Float类型的参数调整为Double类型进行计算
        HashMap<String, Double[]> intensitiesMap = new HashMap<>();
        for (String cutInfo : libIntMap.keySet()) {
            float[] intensityArray = data.getIntMap().get(cutInfo); //获取对应的XIC数据
            //如果没有提取到信号,dataDO为null
            if (intensityArray == null) continue;
            intensitiesMap.put(cutInfo, ArrayUtil.floatToDouble(intensityArray));
        }

        //计算GaussFilter
        Double[] rtArray = ArrayUtil.floatToDouble(data.getRtArray());
        //使用ionsHigh进行平滑选峰
        Double[] ionsHighSmooth = gaussFilter.filter(rtArray, ArrayUtil.intToDouble(data.getIonsHigh()), ss);

        UnSearchPeakGroup unSearchPeakGroup = new UnSearchPeakGroup();
        unSearchPeakGroup.setIonsLow(data.getIonsLow());
        unSearchPeakGroup.setIonsHigh(data.getIonsHigh());
        unSearchPeakGroup.setIonsHighSmooth(ionsHighSmooth);

        RtIntensityPairsDouble maxPeaksForIonsHigh = peakPicker.pickMaxPeak(rtArray, ionsHighSmooth);
        if (maxPeaksForIonsHigh == null || maxPeaksForIonsHigh.getRtArray() == null) { //如果IonsCount没有找到任何峰,则直接认为没有鉴定成功
            log.warn("离子碎片定位峰没有找到任何信号,PeptideRef:" + data.getPeptideRef());
            return new PeakGroupListWrapper(false);
        }

        data.getIntMap().put("HS", ArrayUtil.doubleTofloat(ionsHighSmooth));
        data.getIntMap().put("H", ArrayUtil.intTofloat(data.getIonsHigh()));
        data.getIntMap().put("L", ArrayUtil.intTofloat(data.getIonsLow()));
        data.getCutInfoMap().put("HS", 0f);
        data.getCutInfoMap().put("H", 0f);
        data.getCutInfoMap().put("L", 0f);

        unSearchPeakGroup.setMaxPeaks4IonsHigh(maxPeaksForIonsHigh.toPairs());

        HashMap<String, Double[]> smoothIntensitiesMap = gaussFilter.filter(rtArray, intensitiesMap, ss);
        HashMap<String, RtIntensityPairsDouble> maxPeaks4Ions = new HashMap<>();
        HashMap<String, List<IonPeak>> peaks4Ions = new HashMap<>();

        //对每一个chromatogram进行运算,dataDO中不含有ms1
        HashMap<String, double[]> noise1000Map = new HashMap<>();


        //对每一个片段离子选峰
        double libIntSum = MathUtil.sum(libIntMap.values());
        HashMap<String, Double> normedLibIntMap = new HashMap<>();
        for (String cutInfo : intensitiesMap.keySet()) {
//            //计算两个信噪比
            double[] noises200 = noiseEstimator.computeSTN(rtArray, smoothIntensitiesMap.get(cutInfo), 200, 30);
            double[] noisesOri1000 = noiseEstimator.computeSTN(rtArray, intensitiesMap.get(cutInfo), 1000, 30);
            //根据信噪比和峰值形状选择最高峰,用降噪200及平滑过后的图去挑选Peak峰
            RtIntensityPairsDouble maxPeakPairs = peakPicker.pickMaxPeak(rtArray, smoothIntensitiesMap.get(cutInfo), noises200);
            //根据信噪比和最高峰选择谱图
            if (maxPeakPairs == null) {
                log.info("Error: MaxPeakPairs were null!" + rtArray.length);
                break;
            }
            List<IonPeak> ionPeakList = pickChromatogram(rtArray, intensitiesMap.get(cutInfo), smoothIntensitiesMap.get(cutInfo), noisesOri1000, maxPeakPairs);
            maxPeaks4Ions.put(cutInfo, maxPeakPairs);
            peaks4Ions.put(cutInfo, ionPeakList);

            normedLibIntMap.put(cutInfo, libIntMap.get(cutInfo) / libIntSum);
            noise1000Map.put(cutInfo, noisesOri1000);
        }
        if (peaks4Ions.size() == 0) {
            return new PeakGroupListWrapper(false);
        }
        unSearchPeakGroup.setMaxPeaks4Ions(maxPeaks4Ions);

        unSearchPeakGroup.setFloatRtArray(data.getRtArray());
        unSearchPeakGroup.setRtArray(rtArray);
        unSearchPeakGroup.setMs1Ints(ArrayUtil.floatToDouble(data.getMs1Ints()));
        unSearchPeakGroup.setSelfInts(ArrayUtil.floatToDouble(data.getSelfInts()));
        unSearchPeakGroup.setIntensitiesMap(intensitiesMap);

        unSearchPeakGroup.setPeaks4Ions(peaks4Ions);
        unSearchPeakGroup.setNoise1000Map(noise1000Map);
        unSearchPeakGroup.setCoord(coord);
        List<PeakGroup> peakGroups = peakGroupPicker.findPeakGroupsClassic(unSearchPeakGroup);
        if (peakGroups.size() == 0) {
            return new PeakGroupListWrapper(false);
        }

        return new PeakGroupListWrapper(peakGroups, normedLibIntMap);
    }

    /**
     * @param data  XIC后的数据对象
     * @param coord 库肽段坐标
     * @param ss    sigma spacing
     * @return
     */
    public PeakGroupListWrapper searchByIonsCount(DataDO data, PeptideCoord coord, SigmaSpacing ss) {

        Map<String, Float> libIntMap = coord.buildIntensityMap();
        //将没有提取到信号的CutInfo过滤掉,同时将Float类型的参数调整为Double类型进行计算
        HashMap<String, Double[]> intensitiesMap = new HashMap<>();
        for (String cutInfo : libIntMap.keySet()) {
            float[] intensityArray = data.getIntMap().get(cutInfo); //获取单Fragment的XIC数据
            //如果没有提取到信号,dataDO为null
            if (intensityArray == null) continue;
            intensitiesMap.put(cutInfo, ArrayUtil.floatToDouble(intensityArray));
        }

        //计算GaussFilter
        Double[] rtArray = ArrayUtil.floatToDouble(data.getRtArray());
        //使用ionsHigh进行平滑选峰
        Double[] ionsHighSmooth = gaussFilter.filter(rtArray, ArrayUtil.intToDouble(data.getIonsHigh()), ss);

        UnSearchPeakGroup unSearchPeakGroup = new UnSearchPeakGroup();
        unSearchPeakGroup.setIonsLow(data.getIonsLow());
        unSearchPeakGroup.setIonsHigh(data.getIonsHigh());
        unSearchPeakGroup.setIonsHighSmooth(ionsHighSmooth);

        data.getIntMap().put("HS", ArrayUtil.doubleTofloat(ionsHighSmooth));
        data.getIntMap().put("H", ArrayUtil.intTofloat(data.getIonsHigh()));
        data.getIntMap().put("L", ArrayUtil.intTofloat(data.getIonsLow()));
        data.getCutInfoMap().put("HS", 0f);
        data.getCutInfoMap().put("H", 0f);
        data.getCutInfoMap().put("L", 0f);

        //对每一个片段离子选峰
        double libIntSum = MathUtil.sum(libIntMap.values());
        HashMap<String, Double> normedLibIntMap = new HashMap<>();
        HashMap<String, double[]> noise1000Map = new HashMap<>();
        for (String cutInfo : intensitiesMap.keySet()) {
            double[] noisesOri1000 = noiseEstimator.computeSTN(rtArray, intensitiesMap.get(cutInfo), 1000, 30);
            normedLibIntMap.put(cutInfo, libIntMap.get(cutInfo) / libIntSum);
            noise1000Map.put(cutInfo, noisesOri1000);
        }

        RtIntensityPairsDouble maxPeaks4IonsHigh = peakPicker.pickMaxPeak(rtArray, ionsHighSmooth);
        if (maxPeaks4IonsHigh == null) { //如果IonsCount没有找到任何峰,则直接认为没有鉴定成功
            return new PeakGroupListWrapper(false);
        }
        unSearchPeakGroup.setMaxPeaks4IonsHigh(maxPeaks4IonsHigh.toPairs());

        unSearchPeakGroup.setFloatRtArray(data.getRtArray());
        unSearchPeakGroup.setRtArray(rtArray);
        unSearchPeakGroup.setMs1Ints(ArrayUtil.floatToDouble(data.getMs1Ints()));
        unSearchPeakGroup.setSelfInts(ArrayUtil.floatToDouble(data.getSelfInts()));
        unSearchPeakGroup.setIntensitiesMap(intensitiesMap);

        unSearchPeakGroup.setNoise1000Map(noise1000Map);
        unSearchPeakGroup.setCoord(coord);
        List<PeakGroup> peakGroups = peakGroupPicker.findPeakGroupsByIonsCount(unSearchPeakGroup);
        if (peakGroups.size() == 0) {
            return new PeakGroupListWrapper(false);
        }

        return new PeakGroupListWrapper(peakGroups, normedLibIntMap);
    }

    /**
     * 1）选取最高峰
     * 2）对最高峰进行样条插值
     * 3）根据插值判断maxPeak的rt位置
     * 4）用插值与rt计算intensity
     *
     * @param intensityArray smoothed rtIntensityPairs
     * @param signalToNoise  window width = 200
     * @return maxPeaks
     */
    public RtIntensityPairsDouble pickMaxPeak(Double[] rtArray, Double[] intensityArray, double[] signalToNoise) {
        if (rtArray.length < 5) {
            return null;
        }
        List<Double> maxPeakRtList = new ArrayList<>();
        List<Double> maxPeakIntList = new ArrayList<>();
        double centralPeakRt, leftNeighborRt, rightNeighborRt;
        double centralPeakInt, leftBoundaryInt, rightBoundaryInt;
        double stnLeft, stnMiddle, stnRight;
        int leftBoundary, rightBoundary;
        int missing;
        double maxPeakRt;
        double maxPeakInt;
        double leftHand, rightHand;
        double mid;
        double midDerivVal;

        for (int i = 2; i < rtArray.length - 2; i++) {
            leftNeighborRt = rtArray[i - 1];
            centralPeakRt = rtArray[i];
            rightNeighborRt = rtArray[i + 1];

            leftBoundaryInt = intensityArray[i - 1];
            centralPeakInt = intensityArray[i];
            rightBoundaryInt = intensityArray[i + 1];

            if (rightBoundaryInt < 0.000001) continue;
            if (leftBoundaryInt < 0.000001) continue;

            double leftToCentral, centralToRight, minSpacing = 0d;
            if (Constants.CHECK_SPACINGS) {
                leftToCentral = centralPeakRt - leftNeighborRt;
                centralToRight = rightNeighborRt - centralPeakRt;
                //选出间距较小的那个
                minSpacing = Math.min(leftToCentral, centralToRight);
            }

            stnLeft = signalToNoise[i - 1];
            stnMiddle = signalToNoise[i];
            stnRight = signalToNoise[i + 1];

            //如果中间的比两边的都大
            if (centralPeakInt > leftBoundaryInt &&
                    centralPeakInt > rightBoundaryInt &&
                    stnLeft >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                    stnMiddle >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                    stnRight >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                // 搜索左边的边界
                missing = 0;
                leftBoundary = i - 1;
                for (int left = 2; left < i + 1; left++) {
                    stnLeft = signalToNoise[i - left];
                    if (intensityArray[i - left] < leftBoundaryInt &&
                            (!Constants.CHECK_SPACINGS || (rtArray[leftBoundary] - rtArray[i - left] < Constants.SPACING_DIFFERENCE_GAP * minSpacing))) {
                        if (stnLeft >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                                (!Constants.CHECK_SPACINGS || rtArray[leftBoundary] - rtArray[i - left] < Constants.SPACING_DIFFERENCE * minSpacing)) {
                            leftBoundaryInt = intensityArray[i - left];
                            leftBoundary = i - left;
                        } else {
                            missing++;
                            if (missing <= Constants.MISSING_LIMIT) {
                                leftBoundaryInt = intensityArray[i - left];
                                leftBoundary = i - left;
                            } else {
                                leftBoundary = i - left + 1;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                    //zeroLeft
                    if (intensityArray[i - left] == 0) {
                        break;
                    }
                }

                // 搜索右边的边界
                missing = 0;
                rightBoundary = i + 1;
                for (int right = 2; right < rtArray.length - i; right++) {

                    stnRight = signalToNoise[i + right];


                    if (intensityArray[i + right] < rightBoundaryInt &&
                            (!Constants.CHECK_SPACINGS || (rtArray[i + right] - rtArray[rightBoundary] < Constants.SPACING_DIFFERENCE_GAP * minSpacing))) {
                        if (stnRight >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                                (!Constants.CHECK_SPACINGS || rtArray[i + right] - rtArray[rightBoundary] < Constants.SPACING_DIFFERENCE * minSpacing)) {
                            rightBoundaryInt = intensityArray[i + right];
                            rightBoundary = i + right;
                        } else {
                            missing++;
                            if (missing <= Constants.MISSING_LIMIT) {
                                rightBoundaryInt = intensityArray[i + right];
                                rightBoundary = i + right;
                            } else {
                                rightBoundary = i + right - 1;
                                break;
                            }
                        }
                    } else {
                        break;
                    }

                    //zeroLeft
                    if (intensityArray[i + right] == 0) {
                        break;
                    }
                }

                PeakSpline peakSpline = new PeakSpline();
                peakSpline.init(rtArray, intensityArray, leftBoundary, rightBoundary);
                leftHand = leftNeighborRt;
                rightHand = rightNeighborRt;

                while (rightHand - leftHand > Constants.THRESHOLD) {
                    mid = (leftHand + rightHand) / 2.0d;
                    midDerivVal = peakSpline.derivatives(mid);
                    if (Math.abs(midDerivVal) < 0.001) {
                        break;
                    }
                    if (midDerivVal < 0.0d) {
                        rightHand = mid;
                    } else {
                        leftHand = mid;
                    }
                }

                maxPeakRt = (leftHand + rightHand) / 2.0d;
                maxPeakInt = peakSpline.eval(maxPeakRt);
                maxPeakRtList.add(maxPeakRt);
                maxPeakIntList.add(maxPeakInt);
                i = rightBoundary;
            }
        }
        Double[] rt = maxPeakRtList.toArray(new Double[0]);
        Double[] intensity = maxPeakIntList.toArray(new Double[0]);
        return new RtIntensityPairsDouble(rt, intensity);
    }

    /**
     * 1）选取最高峰
     * 2）对最高峰进行样条插值
     * 3）根据插值判断maxPeak的rt位置
     * 4）用插值与rt计算intensity
     *
     * @param intensityArray smoothed rtIntensityPairs
     * @return maxPeaks
     */
    public RtIntensityPairsDouble pickMaxPeak(Double[] rtArray, Double[] intensityArray) {
        if (rtArray.length < 5) {
            return null;
        }
        List<Double> maxPeakRtList = new ArrayList<>();
        List<Double> maxPeakIntList = new ArrayList<>();
        double leftNeighborRt, rightNeighborRt;
        double centralPeakInt, leftBoundaryInt, rightBoundaryInt;
        int leftBoundary, rightBoundary;
        double maxPeakRt;
        double maxPeakInt;
        double leftHand, rightHand;
        double mid;
        double midDerivVal;

        for (int i = 2; i < rtArray.length - 2; i++) {
            leftNeighborRt = rtArray[i - 1];
            rightNeighborRt = rtArray[i + 1];

            leftBoundaryInt = intensityArray[i - 1];
            centralPeakInt = intensityArray[i];
            rightBoundaryInt = intensityArray[i + 1];

            if (rightBoundaryInt < 0.000001) continue;
            if (leftBoundaryInt < 0.000001) continue;

            //如果中间的比两边的都大
            if (centralPeakInt > leftBoundaryInt && centralPeakInt > rightBoundaryInt) {
                // 搜索左边的边界
                leftBoundary = i - 1;
                for (int left = 2; left < i + 1; left++) {
                    if (intensityArray[i - left] < leftBoundaryInt) {
                        leftBoundaryInt = intensityArray[i - left];
                        leftBoundary = i - left;
                    } else {
                        break;
                    }
                    //zeroLeft
                    if (intensityArray[i - left] == 0) {
                        break;
                    }
                }

                // 搜索右边的边界
                rightBoundary = i + 1;
                for (int right = 2; right < rtArray.length - i; right++) {
                    if (intensityArray[i + right] < rightBoundaryInt) {
                        rightBoundaryInt = intensityArray[i + right];
                        rightBoundary = i + right;
                    } else {
                        break;
                    }

                    //zeroLeft
                    if (intensityArray[i + right] == 0) {
                        break;
                    }
                }

                PeakSpline peakSpline = new PeakSpline();
//                peakSpline.init(rtArray, intensityArray, leftBoundary, rightBoundary);
                peakSpline.init(rtArray, intensityArray, leftBoundary, rightBoundary);
                leftHand = leftNeighborRt;
                rightHand = rightNeighborRt;

                while (rightHand - leftHand > Constants.THRESHOLD) {
                    mid = (leftHand + rightHand) / 2.0d;
                    midDerivVal = peakSpline.derivatives(mid);
                    if (Math.abs(midDerivVal) < 0.001) {
                        break;
                    }
                    if (midDerivVal < 0.0d) {
                        rightHand = mid;
                    } else {
                        leftHand = mid;
                    }
                }

                maxPeakRt = (leftHand + rightHand) / 2.0d;
                maxPeakInt = peakSpline.eval(maxPeakRt);
                maxPeakRtList.add(maxPeakRt);
                maxPeakIntList.add(maxPeakInt);
                i = rightBoundary;
            }
        }
        Double[] rt = maxPeakRtList.toArray(new Double[0]);
        Double[] intensity = maxPeakIntList.toArray(new Double[0]);
        return new RtIntensityPairsDouble(rt, intensity);
    }

    /**
     * 1）根据pickPicker选出的maxPeak的rt在smooth后的rtIntensity pairs中找到最接近的index
     * 2）选取出左右边界
     * 3）根据origin chromatogram 和左右边界对intensity求和
     *
     * @param intensityArray       origin rtIntensity pair
     * @param smoothIntensityArray rtIntensity pair after smooth
     * @param signalToNoise        window length = 1000
     * @param maxPeakPairs         picked max peak
     * @return 左右边界rt, chromatogram边界内intensity求和
     */
    public List<IonPeak> pickChromatogram(Double[] rtArray, Double[] intensityArray, Double[] smoothIntensityArray, double[] signalToNoise, RtIntensityPairsDouble maxPeakPairs) {
        int maxPeakSize = maxPeakPairs.getRtArray().length;
        int leftIndex, rightIndex;

        Double[] chromatogram;
        if (Constants.CHROMATOGRAM_PICKER_METHOD.equals("legacy")) {
            chromatogram = intensityArray;
        } else {
            chromatogram = smoothIntensityArray;
        }

        int closestPeakIndex;
        List<IonPeak> ionPeakList = new ArrayList<>();
        for (int i = 0; i < maxPeakSize; i++) {
            double centralPeakRt = maxPeakPairs.getRtArray()[i];
            closestPeakIndex = PeakUtil.findNearestIndex(rtArray, centralPeakRt);
            //to the left
            leftIndex = closestPeakIndex - 1;
            while (leftIndex > 0 &&
                    (chromatogram[leftIndex - 1] < chromatogram[leftIndex] || (
                            Constants.PEAK_WIDTH > 0 && centralPeakRt - rtArray[leftIndex - 1] < Constants.PEAK_WIDTH))) {
//                            chromatogram[leftIndex - 1] / chromatogram[closestPeakIndex] > Constants.MIN_INTENSITY_RATIO))){
                if (signalToNoise[leftIndex - 1] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                    leftIndex--;
                } else {
                    leftIndex--;
                    break;
                }
            }

            //to the right
            rightIndex = closestPeakIndex + 1;
            while (rightIndex < chromatogram.length - 1 &&
                    (chromatogram[rightIndex + 1] < chromatogram[rightIndex] || (
                            Constants.PEAK_WIDTH > 0 && rtArray[rightIndex + 1] - centralPeakRt < Constants.PEAK_WIDTH)) &&
//                            chromatogram[rightIndex + 1] / chromatogram[closestPeakIndex] > Constants.MIN_INTENSITY_RATIO)) &&
                    signalToNoise[rightIndex + 1] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                rightIndex++;
            }

            double intensity = integratePeaks(intensityArray, leftIndex, rightIndex);
            ionPeakList.add(new IonPeak(intensity, leftIndex, rightIndex, closestPeakIndex, i));
        }

        return ionPeakList;
    }

    private double integratePeaks(Double[] intensityArray, int leftIndex, int rightIndex) {
        double intensity = 0d;
        for (int i = leftIndex; i <= rightIndex; i++) {
            intensity += intensityArray[i];
        }
        return intensity;
    }


}
