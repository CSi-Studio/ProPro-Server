package net.csibio.propro.algorithm.core;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.extract.IonStat;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.service.SimulateService;
import net.csibio.propro.utils.ConvolutionUtil;
import net.csibio.propro.utils.DataUtil;
import net.csibio.propro.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component("coreFunc")
public class CoreFunc {

    @Autowired
    Scorer scorer;
    @Autowired
    SimulateService simulateService;
    @Autowired
    FragmentFactory fragmentFactory;

    /**
     * EIC Core Function
     * 核心EIC函数
     * <p>
     * 本函数为整个分析过程中最耗时的步骤
     *
     * @param coord
     * @param rtMap
     * @param params
     * @return
     */
    public DataDO extractOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        float mzStart = 0;
        float mzEnd = -1;
        //所有的碎片共享同一个RT数组
        ArrayList<Float> rtList = new ArrayList<>();
        for (Float rt : rtMap.keySet()) {
            if (params.getMethod().getEic().getRtWindow() != -1 && rt > coord.getRtEnd()) {
                break;
            }
            if (params.getMethod().getEic().getRtWindow() == -1 || (rt >= coord.getRtStart() && rt <= coord.getRtEnd())) {
                rtList.add(rt);
            }
        }

        float[] rtArray = new float[rtList.size()];
        for (int i = 0; i < rtList.size(); i++) {
            rtArray[i] = rtList.get(i);
        }

        DataDO data = new DataDO(coord);
        data.setRtArray(rtArray);
        if (StringUtils.isNotEmpty(params.getOverviewId())) {
            data.setOverviewId(params.getOverviewId());
        }

        boolean isHit = false;
        float window = params.getMethod().getEic().getMzWindow().floatValue();
        Boolean adaptiveMzWindow = params.getMethod().getEic().getAdaptiveMzWindow();

        for (FragmentInfo fi : coord.getFragments()) {
            float mz = fi.getMz().floatValue();
            mzStart = mz - window;
            mzEnd = mz + window;
            float[] intArray = new float[rtArray.length];
            boolean isAllZero = true;

            //本函数极其注重性能,为整个流程最关键的耗时步骤,每提升10毫秒都可以带来巨大的性能提升  --陆妙善
            if (adaptiveMzWindow) {
                for (int i = 0; i < rtArray.length; i++) {
                    float acc = ConvolutionUtil.adaptiveAccumulation(rtMap.get(rtArray[i]), mz);
                    if (acc != 0) {
                        isAllZero = false;
                    }
                    intArray[i] = acc;
                }
            } else {
                for (int i = 0; i < rtArray.length; i++) {
                    float acc = ConvolutionUtil.accumulation(rtMap.get(rtArray[i]), mzStart, mzEnd);
                    if (acc != 0) {
                        isAllZero = false;
                    }
                    intArray[i] = acc;
                }
            }
            if (isAllZero) {
                //如果该cutInfo没有XIC到任何数据,则不存入IntMap中,这里专门写这个if逻辑是为了帮助后续阅读代码的时候更加容易理解.我们在这边是特地没有将未检测到的碎片放入map的
                continue;
            } else {
                isHit = true;
                data.getIntMap().put(fi.getCutInfo(), intArray); //记录每一个碎片的光谱图
            }
        }

        //如果所有的片段均没有提取到XIC的结果,则直接返回null
        if (!isHit) {
            return null;
        }

        return data;
    }

    /**
     * EIC Predict Peptide
     * 核心EIC预测函数
     * 通过动态替换碎片用来预测
     * <p>
     *
     * @param coord
     * @param rtMap
     * @param params
     * @return
     */
    public DataDO predictOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, ExperimentDO exp, OverviewDO overview, AnalyzeParams params) {
        log.info("实验:" + exp.getAlias() + "开始预测:" + coord.getPeptideRef());
        //Step1.对库中的碎片进行排序
        Map<String, FragmentInfo> libFragMap = coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, Function.identity()));
        List<FragmentInfo> sortedLibFrags = new ArrayList<>(coord.getFragments()).stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList());
        List<String> libIons = sortedLibFrags.stream().map(FragmentInfo::getCutInfo).toList();
        //Step2.生成BY碎片离子,碎片最大带电量为母离子的带电量,最小碎片长度为3
        Set<FragmentInfo> proproFiList = fragmentFactory.buildFragmentMap(coord, 3);
        proproFiList.forEach(fi -> fi.setIntensity(1000d)); //给到一个任意的初始化强度
        Map<String, FragmentInfo> predictFragmentMap = proproFiList.stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, Function.identity()));
        libFragMap.keySet().forEach(cutInfo -> {
            predictFragmentMap.put(cutInfo, libFragMap.get(cutInfo));
        });
        //Step3.对所有碎片进行EIC计算
        coord.setFragments(proproFiList);
        DataDO data = extractOne(coord, rtMap, params);
        Map<String, float[]> intMap = data.getIntMap();

        //Step4.获取所有碎片的统计分,并按照CV值进行排序,记录前15的碎片
        List<IonStat> statList = buildIonStat(intMap);
//        if (statList.size() > 20) {
//            statList = statList.subList(0, 15);
//        }
        List<String> totalIonList = statList.stream().map(IonStat::cutInfo).toList();

        //Step5.开始全枚举所有的组合分
//        List<List<String>> allPossibleIonsGroup = Generator.combination(totalIons).simple(6).stream().collect(Collectors.toList());

        double bestScore = -99999d;
        Double bestRt = null;
        DataDO bestData = null;
        Set<FragmentInfo> bestIonGroup = null;
        List<String> types = ScoreType.getAllTypesName();
//        for (int i = 0; i < allPossibleIonsGroup.size(); i++) {
//            List<String> ionsGroup = allPossibleIonsGroup.get(i);
//            DataDO buildData = buildData(data, ionsGroup);
//            coord.setFragments(selectFragments(predictFragmentMap, ionsGroup));
//            scorer.baseScoreForOne(buildData, coord);
//            if (buildData.getScoreList() != null) {
//                PeakGroupScores peakScore = scorer.getBestTargetPeak(buildData, ScoreType.XcorrShape.getName());
//                double shapeScore = peakScore.get(ScoreType.XcorrShape.getName(), types);
//                if (shapeScore > bestShapeScore) {
//                    bestShapeScore = shapeScore;
//                    bestPeak = peakScore;
//                    bestData = buildData;
//                }
//            }
//        }
//        List<String> subIonList = libIons.subList(0, libIons.size());
        for (int i = 0; i < totalIonList.size(); i++) {
            String ion = totalIonList.get(i);
            List<String> ions = new ArrayList<>(libIons.subList(0, libIons.size() - 1));
            ions.add(ion);
            DataDO buildData = buildData(data, ions);
            Set<FragmentInfo> selectFragments = selectFragments(predictFragmentMap, ions);
            coord.setFragments(selectFragments);
            scorer.scoreForOne(exp, buildData, coord, rtMap, params);
            if (buildData.getScoreList() != null) {
                DataSumDO dataSum = scorer.getBestTotalScore(buildData, overview);
                if (dataSum.getTotalScore() > bestScore) {
                    bestScore = dataSum.getTotalScore();
                    bestRt = dataSum.getRealRt();
                    bestData = buildData;
                    bestIonGroup = selectFragments;
                }
            }
        }

        if (bestData == null) {
            log.info("居然一个可能的组都没有");
            return null;
        }
        coord.setFragments(bestIonGroup); //这里必须要将coord置为最佳峰组
//        log.info("库中的碎片组为:" + libIons);
        log.info("最终选中的碎片组为:" + coord.getFragments().stream().map(FragmentInfo::getCutInfo).collect(Collectors.toSet()));
        log.info("最终的打分:" + bestScore);
        log.info("选择的峰RT为:" + bestRt);

//        int minIndex = Integer.MAX_VALUE;
//
//        for (FragmentInfo fragment : sortedLibFrags) {
//            IonStat maxIonStat = statList.stream().filter(stat -> stat.cutInfo().equals(fragment.getCutInfo())).findFirst().get();
//            int index = statList.indexOf(maxIonStat);
//            if (index < minIndex) {
//                minIndex = index;
//            }
//        }

//        int minIndex = 0;
//        statList = statList.subList(minIndex, minIndex + 12);
//        Set<FragmentInfo> finalFiSet = new HashSet<>();
//        for (int i = 0; i < statList.size(); i++) {
//            IonStat ion = statList.get(i);
//            FragmentInfo predict = predictFragmentMap.get(ion.cutInfo());
//            if (i < sortedLibFrags.size() - 1) {
//                predict.setIntensity(sortedLibFrags.get(i).getIntensity());
//            } else {
//                predict.setIntensity(sortedLibFrags.get(sortedLibFrags.size() - 1).getIntensity() / 3);
//            }
//            FragmentInfo lastPredictPoint = null;
//            double lastPointIntensity = 0d;
//            //如果原标准库中已经包含了该预测碎片,则直接使用库中的碎片信息
//            if (libFragMap.containsKey(ion.cutInfo())) {
//                FragmentInfo info = libFragMap.get(ion.cutInfo());
//                info.setPredict(false);
//                finalFiSet.add(info);
//                //如果此时上一个预测锚点存在,则先将该锚点的值设置为本锚点与上一次锚点的平均值
//                if (lastPredictPoint != null) {
//                    lastPredictPoint.setIntensity((info.getIntensity() + lastPointIntensity) / 2);
//                    lastPredictPoint = null;
//                }
//                lastPointIntensity = libFragMap.get(ion.cutInfo()).getIntensity();
//            } else {
//                //如果原标准库中不存在该碎片,则将该点的预测强度填充为上一个预测点的一半
//                lastPredictPoint = predictFragmentMap.get(ion.cutInfo());
//                lastPredictPoint.setPredict(true);
//                lastPredictPoint.setIntensity(lastPointIntensity / 2);
//                finalFiSet.add(lastPredictPoint);
//            }
//            finalFiSet.add(predict);
//        }
//        HashMap<String, float[]> selectedIntMap = new HashMap<>();
//        HashMap<String, Float> selectedCutInfoMap = new HashMap<>();
//        statList.forEach(ionStat -> {
//            selectedIntMap.put(ionStat.cutInfo(), data.getIntMap().get(ionStat.cutInfo()));
//            selectedCutInfoMap.put(ionStat.cutInfo(), data.getCutInfoMap().get(ionStat.cutInfo()));
//        });
//
//        data.setIntMap(selectedIntMap);
//        data.setCutInfoMap(selectedCutInfoMap);
        return bestData;
    }

    /**
     * EIC+PEAK_PICKER+PEAK_SCORE 核心流程
     * 最终的提取XIC结果需要落盘数据库,一般用于正式XIC提取的计算
     *
     * @param coordinates
     * @param rtMap
     * @param params
     * @return
     */
    public List<DataDO> epps(ExperimentDO
                                     exp, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        List<DataDO> dataList = Collections.synchronizedList(new ArrayList<>());
        long start = System.currentTimeMillis();
        if (coordinates == null || coordinates.size() == 0) {
            log.error("肽段坐标为空");
            return null;
        }

        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段.如果先处理的真肽段没有被提取到任何信息,或者提取后的峰太差被忽略掉,都会同时删掉对应的伪肽段的XIC
        coordinates.parallelStream().forEach(coord -> {
            //Step1. 常规提取XIC,XIC结果不进行压缩处理,如果没有提取到任何结果,那么加入忽略列表
            DataDO dataDO = extractOne(coord, rtMap, params);
            if (dataDO == null) {
                log.info(coord.getPeptideRef() + ":EIC结果为空");
                return;
            }

            //Step2. 常规选峰及打分,未满足条件的直接忽略
            scorer.scoreForOne(exp, dataDO, coord, rtMap, params);
            dataList.add(dataDO);
            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(dataDO);

            //如果没有打分数据,那么对应的decoy也不再计算,以保持target与decoy 1:1的混合比例,这里需要注意的是,即便是scoreList是空,也需要将DataDO存储到数据库中,以便后续的重新统计和分析
            if (dataDO.getScoreList() == null) {
                return;
            }

            //Step4. 如果第一,二步均符合条件,那么开始对对应的伪肽段进行数据提取和打分
            coord.setDecoy(true);
            DataDO decoyData = extractOne(coord, rtMap, params);
            if (decoyData == null) {
                return;
            }

            //Step5. 对Decoy进行打分
            scorer.scoreForOne(exp, decoyData, coord, rtMap, params);
            dataList.add(decoyData);
            //Step6. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(decoyData);
        });

        LogUtil.log("XIC+选峰+打分耗时", start);
        if (dataList.stream().filter(data -> data.getStatus() == null).toList().size() > 0) {
            log.info("居然有问题");
        }
        return dataList;
    }

    private Set<FragmentInfo> selectFragments(Map<String, FragmentInfo> fragMap, List<String> selectedIons) {
        Set<FragmentInfo> fragmentInfos = new HashSet<>();
        for (String selectedIon : selectedIons) {
            fragmentInfos.add(fragMap.get(selectedIon));
        }
        return fragmentInfos;
    }

    private DataDO buildData(DataDO data, List<String> selectedIons) {
        HashMap<String, float[]> selectedIntMap = new HashMap<>();
        HashMap<String, Float> selectedCutInfoMap = new HashMap<>();
        selectedIons.forEach(cutInfo -> {
            selectedIntMap.put(cutInfo, data.getIntMap().get(cutInfo));
            selectedCutInfoMap.put(cutInfo, data.getCutInfoMap().get(cutInfo));
        });
        DataDO newData = data.clone();
        newData.setIntMap(selectedIntMap);
        newData.setCutInfoMap(selectedCutInfoMap);
        return newData;
    }

    private List<IonStat> buildIonStat(Map<String, float[]> intMap) {
        List<IonStat> statList = new ArrayList<>();
        List<IonStat> finalStatList = statList;
        intMap.forEach((key, fArray) -> {
            double[][] sumArray = new double[fArray.length][2];
            List<Double> dList = new ArrayList<>();
            for (int i = 0; i < fArray.length; i++) {
                sumArray[i] = (i == 0) ? new double[]{1d, (double) fArray[i]} : new double[]{(double) i + 1, sumArray[i - 1][1] + fArray[i]};
                if (fArray[i] != 0f) {
                    dList.add((double) fArray[i]);
                }
            }
            double[] dArray = new double[dList.size()];
            for (int i = 0; i < dList.size(); i++) {
                dArray[i] = dList.get(i);
            }
            DescriptiveStatistics stat = new DescriptiveStatistics(dArray);
            //计算强度的偏差值,在RT范围内的偏差值越大说明峰的显著程度越高
            double cv = stat.getStandardDeviation() / stat.getMean();
            finalStatList.add(new IonStat(key, cv));
        });
        //按照cv从大到小排序
        statList = statList.stream().sorted(Comparator.comparing(IonStat::stat).reversed()).toList();
        return statList;
    }
}
