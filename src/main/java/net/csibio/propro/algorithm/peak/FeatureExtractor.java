package net.csibio.propro.algorithm.peak;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.bean.common.DoublePair;
import net.csibio.propro.domain.bean.data.RtIntensityPairsDouble;
import net.csibio.propro.domain.bean.data.UnSearchPeakGroup;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("featureExtractor")
public class FeatureExtractor {

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

    /**
     * @param data         XIC后的数据对象
     * @param intensityMap 得到标准库中peptideRef对应的碎片和强度的键值对,理论强度值
     * @param ss           sigma spacing
     * @return
     */
    public PeakGroupListWrapper searchPeakGroups(DataDO data, HashMap<String, Float> intensityMap, SigmaSpacing ss) {

        PeakGroupListWrapper featureResult = new PeakGroupListWrapper(true);
        if (data.getIntMap().isEmpty()) {
            return new PeakGroupListWrapper(false);
        }

        HashMap<String, RtIntensityPairsDouble> maxPeaksForIons = new HashMap<>();
        HashMap<String, List<IonPeak>> peaksForIons = new HashMap<>();

        //对每一个chromatogram进行运算,dataDO中不含有ms1
        HashMap<String, double[]> noise1000Map = new HashMap<>();
        HashMap<String, Double[]> intensitiesMap = new HashMap<>();

        //将没有提取到信号的CutInfo过滤掉,同时将Float类型的参数调整为Double类型进行计算
        for (String cutInfo : intensityMap.keySet()) {
            float[] intensityArray = data.getIntMap().get(cutInfo); //获取对应的XIC数据
            //如果没有提取到信号,dataDO为null
            if (intensityArray == null) {
                continue;
            }
            intensitiesMap.put(cutInfo, ArrayUtil.floatToDouble(intensityArray));
        }

        if (intensitiesMap.size() == 0) {
            return new PeakGroupListWrapper(false);
        }

        //计算GaussFilter
        Double[] rtArray = ArrayUtil.floatToDouble(data.getRtArray());
        HashMap<String, Double[]> smoothIntensitiesMap = gaussFilter.filter(rtArray, intensitiesMap, ss);

        UnSearchPeakGroup unSearchPeakGroup = new UnSearchPeakGroup();
        //计算IonCount对应的值
        unSearchPeakGroup.setIonsCount(data.getIonsCounts());
        Double[] ionCountIntensity = ArrayUtil.intToDouble(data.getIonsCounts());
        float[] ionCountIntensityFloat = ArrayUtil.intTofloat(data.getIonsCounts());
        Double[] ionCountSmoothIntensity = gaussFilter.filter(rtArray, ionCountIntensity, ss);
        RtIntensityPairsDouble maxPeaksForIonCount = peakPicker.pickMaxPeak(rtArray, ionCountSmoothIntensity);
//        RtIntensityPairsDouble maxPeaksForIonCount2 = peakPicker.pickMaxPeak(rtArray, ionCountIntensity);
        if (maxPeaksForIonCount == null || maxPeaksForIonCount.getRtArray() == null) { //如果IonsCount没有找到任何峰,则直接认为没有鉴定成功
            log.warn("离子碎片定位峰没有找到任何信号,PeptideRef:" + data.getPeptideRef());
            return new PeakGroupListWrapper(false);
        }
        float[] ionCountFloat = new float[ionCountSmoothIntensity.length];
        for (int i = 0; i < ionCountSmoothIntensity.length; i++) {
            ionCountFloat[i] = ionCountSmoothIntensity[i].floatValue();
        }
        data.getIntMap().put("S", ionCountFloat);
        data.getIntMap().put("O", ionCountIntensityFloat);
        data.getCutInfoMap().put("S", 0f);
        data.getCutInfoMap().put("O", 0f);

        List<DoublePair> pairs = maxPeaksForIonCount.toPairs();
        pairs = pairs.stream().sorted(Comparator.comparingDouble(DoublePair::right).reversed()).collect(Collectors.toList()); //按照强度比从大到小排序
        if (pairs.size() > 1) {
            if (pairs.get(0).right() >= 3) {
                for (int i = 0; i < pairs.size() - 1; i++) {
                    double delta = pairs.get(i).right() - pairs.get(i + 1).right();
                    double weight = delta / pairs.get(i).right();

                    if (pairs.get(i).right() > 3 && (weight > 0.5 && pairs.get(i).right() > 4) ||
                            (pairs.get(i).right() <= 4 && delta > 2.5)) {
                        pairs = pairs.subList(0, i + 1);
                        break;
                    }
                }
            } else {
                return new PeakGroupListWrapper(false);
            }
        } else if (pairs.size() == 1) {
            if (pairs.get(0).right() < 6) {
                return new PeakGroupListWrapper(false);
            }
        } else {
            return new PeakGroupListWrapper(false);
        }

        unSearchPeakGroup.setMaxPeaksForIonCount(pairs);

        //对每一个片段离子选峰
        double libIntSum = MathUtil.sum(intensityMap.values());
        HashMap<String, Double> normedLibIntMap = new HashMap<>();
        for (String cutInfo : intensitiesMap.keySet()) {
            //计算两个信噪比
            double[] noises200 = signalToNoiseEstimator.computeSTN(rtArray, smoothIntensitiesMap.get(cutInfo), 200, 30);
            double[] noisesOri1000 = signalToNoiseEstimator.computeSTN(rtArray, intensitiesMap.get(cutInfo), 1000, 30);
            //根据信噪比和峰值形状选择最高峰,用降噪200及平滑过后的图去挑选Peak峰
            RtIntensityPairsDouble maxPeakPairs = peakPicker.pickMaxPeak(rtArray, smoothIntensitiesMap.get(cutInfo), noises200);
            //根据信噪比和最高峰选择谱图
            if (maxPeakPairs == null) {
                log.info("Error: MaxPeakPairs were null!" + rtArray.length);
                break;
            }
            List<IonPeak> ionPeakList = chromatogramPicker.pickChromatogram(rtArray, intensitiesMap.get(cutInfo), smoothIntensitiesMap.get(cutInfo), noisesOri1000, maxPeakPairs);
            maxPeaksForIons.put(cutInfo, maxPeakPairs);
            peaksForIons.put(cutInfo, ionPeakList);
            noise1000Map.put(cutInfo, noisesOri1000);
            normedLibIntMap.put(cutInfo, intensityMap.get(cutInfo) / libIntSum);
        }
        if (peaksForIons.size() == 0) {
            return new PeakGroupListWrapper(false);
        }

        unSearchPeakGroup.setFloatRtArray(data.getRtArray());
        unSearchPeakGroup.setRtArray(rtArray);
        unSearchPeakGroup.setIntensitiesMap(intensitiesMap);
        unSearchPeakGroup.setMaxPeaksForIons(maxPeaksForIons);
        unSearchPeakGroup.setNoise1000Map(noise1000Map);
        unSearchPeakGroup.setPeaksForIons(peaksForIons);

        List<PeakGroup> peakGroups = featureFinder.findPeakGroupsV2(unSearchPeakGroup);
        if (pairs.size() == 1 && peakGroups.size() > 0) {
            data.setOnly(true);
        }
        if (pairs.size() == 1 && peakGroups.size() == 0) {
            log.error("居然没有匹配到,蛋疼:" + data.getPeptideRef());
        }

        featureResult.setList(peakGroups);
        featureResult.setNormedIntMap(normedLibIntMap);

        return featureResult;
    }
}
