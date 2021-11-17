package net.csibio.propro.algorithm.core;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.extract.IonStat;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.peak.GaussFilter;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.score.features.DIAScorer;
import net.csibio.propro.constants.constant.CutInfoConst;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.common.AnyPair;
import net.csibio.propro.domain.bean.common.IntegerPair;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.SimulateService;
import net.csibio.propro.utils.ConvolutionUtil;
import net.csibio.propro.utils.DataUtil;
import net.csibio.propro.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.paukov.combinatorics3.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    @Autowired
    OverviewService overviewService;
    @Autowired
    Lda lda;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    DataService dataService;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    Extractor extractor;

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
            //在此处直接设置data的Id
            data.setId(params.getOverviewId() + coord.getPeptideRef() + data.getDecoy());
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
            log.info(coord.getPeptideRef() + ":EIC结果为空");
            return null;
        }
        //计算每一帧的离子碎片总数
        extractor.calcIonsCount(data, coord, rtMap);
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
    public AnyPair<DataDO, DataSumDO> predictOneDelete(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, RunDO run, OverviewDO overview, AnalyzeParams params) {

        List<FragmentInfo> libFrags = new ArrayList<>(coord.getFragments()).stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList());
        int size = coord.getFragments().size();

        //初始化原始数据对象集
        DataDO data = extractOne(coord, rtMap, params);
        if (data == null) {
            log.info("XIC没有检测到任何数据,小概率事件:" + coord.getPeptideRef());
            return null;
        }
        String maxIons = libFrags.get(0).getCutInfo();
        HashMap<Double, List<AnyPair<DataDO, DataSumDO>>> hitPairMap = new HashMap<>();

        int[] ions50 = new int[rtMap.size()];
        AtomicInteger iter = new AtomicInteger(0);
        DataDO finalData = data;
        rtMap.forEach((key, value) -> {
            float maxIntensity = finalData.getIntMap().get(maxIons)[iter.get()];
            IntegerPair pair = diaScorer.calcTotalIons(value.getMzArray(), value.getIntensityArray(), coord.getUnimodMap(), coord.getSequence(), coord.getCharge(), 50, 300, maxIntensity);
            ions50[iter.get()] = pair.left();
            iter.getAndIncrement();
        });
        data.setIonsLow(ions50);
        double bestScore = -99999d;
        AnyPair<DataDO, DataSumDO> bestPair = null;
        AnyPair<DataDO, DataSumDO> bestIonsPair = null;

        String removeCutInfo = "null";

        //当i=0的时候,不切换,也就是说跳过了强度比最高的碎片,强度比最高的碎片不进行离子替换
        for (int i = -1; i < size; i++) {
            String currentRemoveCutInfo = "null";
            //i==-1的时候检测的就是自己本身
            if (i != -1) {
                FragmentInfo temp = libFrags.get(i);
                libFrags.remove(i);
                currentRemoveCutInfo = temp.getCutInfo();
                coord.setFragments(libFrags);
                libFrags.add(i, temp);
                data = extractOne(coord, rtMap, params);
                if (data == null) {
                    continue;
                }
                data.setIonsLow(ions50);
            }

            try {
                data = scorer.scoreForOne(run, data, coord, rtMap, params);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Peptide打分异常:" + coord.getPeptideRef());
            }

            if (data.getPeakGroupList() != null) {
                DataSumDO dataSum = scorer.calcBestTotalScore(data, overview, null);
                double currentBYCount = scorer.calcBestIonsCount(data);
//                if (currentBYCount > maxBYCount) {
//                    maxBYCount = currentBYCount;
//                }
                if (dataSum != null) {
                    if (!hitPairMap.containsKey(dataSum.getSelectedRt())) {
                        List<AnyPair<DataDO, DataSumDO>> pairs = new ArrayList<>();
                        pairs.add(new AnyPair<DataDO, DataSumDO>(data, dataSum));
                        hitPairMap.put(dataSum.getSelectedRt(), pairs);
                    } else {
                        List<AnyPair<DataDO, DataSumDO>> currentPairs = hitPairMap.get(dataSum.getSelectedRt());
                        currentPairs.add(new AnyPair<DataDO, DataSumDO>(data, dataSum));
                    }
                }
            }
        }
        AtomicDouble maxHitRt = new AtomicDouble();
        AtomicInteger maxHits = new AtomicInteger();
        hitPairMap.forEach((key, value) -> {
            if (value.size() > maxHits.get()) {
                maxHitRt.set(key);
                maxHits.set(value.size());
            }
        });

        //获取Max Hit的pair组
        List<AnyPair<DataDO, DataSumDO>> maxHitGroup = hitPairMap.get(maxHitRt.get());
        if (maxHitGroup == null) {
            return null;
        }
        for (AnyPair<DataDO, DataSumDO> anyPair : maxHitGroup) {
            if (anyPair.getRight().getTotalScore() > bestScore) {
                bestPair = anyPair;
                bestScore = anyPair.getRight().getTotalScore();
            }
        }

        if (bestPair == null) {
            log.info("没有产生数据:" + run.getAlias() + ":Max Hit RT:" + maxHitRt.get() + ";Hits:" + maxHits.get());
            return null;
        }

        bestPair.getLeft().getCutInfoMap().put(CutInfoConst.ION_COUNT, 0f);
        float[] ionsFloatArray = new float[ions50.length];
        for (int i = 0; i < ions50.length; i++) {
            ionsFloatArray[i] = ions50[i];
        }
        bestPair.getLeft().getIntMap().put(CutInfoConst.ION_COUNT, ionsFloatArray);
        if (maxHitRt.get() != bestPair.getRight().getSelectedRt()) {
            log.info("有问题:" + run.getAlias() + ":Max Hit RT:" + maxHitRt.get() + ";Hits:" + maxHits.get());
        } else {
            log.info("RT吻合:" + run.getAlias() + ":Max Hit RT:" + maxHitRt.get() + ";Hits:" + maxHits.get());
        }

//        double finalIonsCount = scorer.calcBestIonsCount(bestData);
        //Max BYCount Limit
//        if (maxBYCount != finalIonsCount && finalIonsCount <= 5) {
////            log.info("未预测到严格意义下的新碎片组合:" + coord.getPeptideRef() + ",IonsCount:" + finalBYCount);
//            return null;
//        }

//        log.info(JSON.toJSONString(libFrags.stream().map(FragmentInfo::getCutInfo).collect(Collectors.toList())));
//        log.info("预测到严格意义下的新碎片组合:" + coord.getPeptideRef() + ",IonsCount:" + finalBYCount + ",Remove CutInfo=" + removeCutInfo);
        return bestPair;
    }

    public AnyPair<DataDO, DataSumDO> predictOneNiubi(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, RunDO run, AnalyzeParams params) {
//        coord.setFragments(coord.getFragments().stream().filter(f -> !f.getCutInfo().equals("y7")).collect(Collectors.toSet()));
        DataDO dataDO = extractOne(coord, rtMap, params);
        //EIC结果如果为空则没有继续的必要了
        if (dataDO == null) {
            log.info(coord.getPeptideRef() + ":EIC结果为空");
            return null;
        }
        //如果
        if (dataDO.getIntMap() == null || (!params.getPredict() && dataDO.getIntMap().size() <= coord.getFragments().size() / 2)) {
            dataDO.setStatus(IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode());
            return null;
        }

        dataDO = scorer.scoreForOne(run, dataDO, coord, rtMap, params);
        DataSumDO sum = judge(dataDO);
        return new AnyPair<>(dataDO, sum);
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
    public AnyPair<DataDO, DataSumDO> predictOneReplace(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, RunDO run, OverviewDO overview, AnalyzeParams params) {
        //Step1.对库中的碎片进行排序,按照强度从大到小排列
        Map<String, FragmentInfo> libFragMap = coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, Function.identity()));
        List<FragmentInfo> sortedLibFrags = new ArrayList<>(coord.getFragments()).stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList());
        List<String> libIons = sortedLibFrags.stream().map(FragmentInfo::getCutInfo).toList();
        String maxLibIon = libIons.get(0);
        //Step2.生成碎片离子,碎片最大带电量为母离子的带电量,最小碎片长度为3
        Set<FragmentInfo> proproFiList = fragmentFactory.buildFragmentMap(coord, 3);
        proproFiList.forEach(fi -> fi.setIntensity(500d)); //给到一个任意的初始化强度
        Map<String, FragmentInfo> predictFragmentMap = proproFiList.stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, Function.identity()));
        //将预测碎片中的库碎片信息替换为库碎片完整信息(主要是intensity值)
        libFragMap.keySet().forEach(cutInfo -> {
            predictFragmentMap.put(cutInfo, libFragMap.get(cutInfo));
        });
        //Step3.对所有碎片进行EIC计算
        coord.setFragments(new ArrayList<>(proproFiList));
        DataDO data = extractOne(coord, rtMap, params);
        Map<String, float[]> intMap = data.getIntMap();

        //Step4.获取所有碎片的统计分,并按照CV值进行排序,记录前15的碎片
        List<IonStat> statList = buildIonStat(intMap);
        int maxCandidateIons = params.getMethod().getScore().getMaxCandidateIons();
        maxCandidateIons = 20;
        if (statList.size() > maxCandidateIons) {
            statList = statList.subList(0, maxCandidateIons);
        }
        List<String> totalIonList = statList.stream().map(IonStat::cutInfo).toList();

        //Step5.开始全枚举所有的组合分
        double bestScore = -99999d;
        DataSumDO bestDataSum = null;
        DataDO bestData = null;
        List<FragmentInfo> bestIonGroup = null;

        int replace = params.getChangeCharge() ? 6 : 1; //如果是新带点碎片预测,那么直接全部替换
        List<List<String>> allPossibleIonsGroup = Generator.combination(totalIonList).simple(replace).stream().collect(Collectors.toList()); //替换策略
        double maxBYCount = -1;
        for (int i = 0; i < allPossibleIonsGroup.size(); i++) {
            List<String> selectedIons = allPossibleIonsGroup.get(i);

            //抹去强度最低的两个碎片
            List<String> ions = new ArrayList<>(libIons.subList(0, libIons.size() - selectedIons.size()));
            ions.addAll(selectedIons);
            DataDO buildData = buildData(data, ions);
            if (buildData == null) {
                continue;
            }
            List<FragmentInfo> selectFragments = selectFragments(predictFragmentMap, ions);
            if (selectFragments.size() < libIons.size()) {
                continue;
            }
            coord.setFragments(selectFragments);
            try {
                buildData = scorer.scoreForOne(run, buildData, coord, rtMap, params);
            } catch (Exception e) {
                log.error("Peptide打分异常:" + coord.getPeptideRef());
            }

            if (buildData.getPeakGroupList() != null) {
                DataSumDO dataSum = scorer.calcBestTotalScore(buildData, overview, maxLibIon);
                double currentBYCount = scorer.calcBestIonsCount(buildData);
                if (currentBYCount > maxBYCount) {
                    maxBYCount = currentBYCount;
                }
                if (dataSum != null && dataSum.getTotalScore() > bestScore) {
                    bestScore = dataSum.getTotalScore();
                    bestDataSum = dataSum;
                    bestData = buildData;
                    bestIonGroup = selectFragments;
                }
            }
        }

        if (bestData == null) {
            //  log.info("居然一个可能的组都没有:" + coord.getPeptideRef());
            return null;
        }
        double finalBYCount = scorer.calcBestIonsCount(bestData);
        //Max BYCount Limit
        if (maxBYCount != finalBYCount && finalBYCount <= 5) {
            log.info("未预测到严格意义下的新碎片组合:" + coord.getPeptideRef() + ",IonsCount:" + finalBYCount);
            return null;
        }

        log.info("预测到严格意义下的新碎片组合:" + coord.getPeptideRef() + ",IonsCount:" + finalBYCount);
        coord.setFragments(bestIonGroup); //这里必须要将coord置为最佳峰组
//        log.info(run.getAlias() + "碎片组:" + bestIonGroup.stream().map(FragmentInfo::getCutInfo).toList() + "; Score:" + bestScore + " RT:" + bestRt);
        return new AnyPair<DataDO, DataSumDO>(bestData, bestDataSum);
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
    public List<DataDO> epps(RunDO run, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
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
            //如果EIC结果中所有的碎片均为空,那么也不需要再做Reselect操作,直接跳过
            if (dataDO == null) {
                log.info(coord.getPeptideRef() + ":EIC结果为空");
                return;
            }

            //Step2. 常规选峰及打分,未满足条件的直接忽略
            dataDO = scorer.scoreForOne(run, dataDO, coord, rtMap, params);
            dataList.add(dataDO);

            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(dataDO);

            //如果没有打分数据,那么对应的decoy也不再计算,以保持target与decoy 1:1的混合比例,这里需要注意的是,即便是scoreList是空,也需要将DataDO存储到数据库中,以便后续的重新统计和分析
            if (dataDO.getPeakGroupList() == null) {
                return;
            }

            //Step4. 如果第一,二步均符合条件,那么开始对对应的伪肽段进行数据提取和打分
            coord.setDecoy(true);
            DataDO decoyData = extractOne(coord, rtMap, params);
            if (decoyData == null) {
                return;
            }

            //Step5. 对Decoy进行打分
            decoyData = scorer.scoreForOne(run, decoyData, coord, rtMap, params);
            dataList.add(decoyData);

            //Step6. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(decoyData);
        });

        LogUtil.log("XIC+选峰+打分耗时", start);
        log.info("总计构建Data数目" + dataList.size() + "/" + (coordinates.size() * 2) + "个");
        if (dataList.stream().filter(data -> data.getStatus() == null).toList().size() > 0) {
            log.info("居然有问题");
        }
        return dataList;
    }

    public List<DataDO> reselect(RunDO run, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        List<DataDO> dataList = Collections.synchronizedList(new ArrayList<>());
        long start = System.currentTimeMillis();
        if (coordinates == null || coordinates.size() == 0) {
            log.error("肽段坐标为空");
            return null;
        }
        AtomicLong newIonsGroup = new AtomicLong(0);
        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段.如果先处理的真肽段没有被提取到任何信息,或者提取后的峰太差被忽略掉,都会同时删掉对应的伪肽段的XIC
        coordinates.parallelStream().forEach(coord -> {
            DataDO dataDO = extractOne(coord, rtMap, params);
            //如果EIC结果中所有的碎片均为空,那么也不需要再做Reselect操作,直接跳过
            if (dataDO == null) {
                log.info(coord.getPeptideRef() + ":EIC结果为空");
                return;
            }
            //Step2. 常规选峰及打分,未满足条件的直接忽略
            dataDO = scorer.scoreForOne(run, dataDO, coord, rtMap, params);
            lda.scoreForPeakGroups(dataDO.getPeakGroupList(), params.getBaseOverview().getWeights(), params.getBaseOverview().getParams().getMethod().getScore().getScoreTypes());
            DataSumDO tempSum = scorer.calcBestTotalScore(dataDO, params.getBaseOverview(), null);
            if (tempSum == null || tempSum.getStatus() != IdentifyStatus.SUCCESS.getCode()) {
                DataSumDO dataSum = scorer.calcBestTotalScore(dataDO, params.getBaseOverview(), null);
                if (dataSum == null || dataSum.getStatus() != IdentifyStatus.SUCCESS.getCode()) {
                    AnyPair<DataDO, DataSumDO> pair = predictOneDelete(coord, rtMap, run, params.getBaseOverview(), params);
                    if (pair != null && pair.getLeft() != null) {
                        newIonsGroup.getAndIncrement();
                        dataDO = pair.getLeft();
                    }
                }
            }

            dataList.add(dataDO);
            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(dataDO);

            //如果没有打分数据,那么对应的decoy也不再计算,以保持target与decoy 1:1的混合比例,这里需要注意的是,即便是scoreList是空,也需要将DataDO存储到数据库中,以便后续的重新统计和分析
            if (dataDO.getPeakGroupList() == null) {
                return;
            }

            //Step4. 如果第一,二步均符合条件,那么开始对对应的伪肽段进行数据提取和打分
            coord.setDecoy(true);
            DataDO decoyData = extractOne(coord, rtMap, params);
            if (decoyData == null) {
                return;
            }

            //Step5. 对Decoy进行打分
            decoyData = scorer.scoreForOne(run, decoyData, coord, rtMap, params);
            dataList.add(decoyData);

            //Step6. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(decoyData);
        });

        LogUtil.log("XIC+选峰+打分耗时", start);
        log.info("新增组合碎片数目为:" + newIonsGroup.get());
        log.info("总计构建Data数目" + dataList.size() + "/" + (coordinates.size() * 2) + "个");
        if (dataList.stream().filter(data -> data.getStatus() == null).toList().size() > 0) {
            log.info("居然有问题");
        }
        return dataList;
    }

    public List<DataDO> csi(RunDO run, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        List<DataDO> dataList = Collections.synchronizedList(new ArrayList<>());
        if (coordinates == null || coordinates.size() == 0) {
            log.error("肽段坐标为空");
            return null;
        }
        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段.如果先处理的真肽段没有被提取到任何信息,或者提取后的峰太差被忽略掉,都会同时删掉对应的伪肽段的XIC
        coordinates.parallelStream().forEach(coord -> {
            DataDO dataDO = extractOne(coord, rtMap, params);
            if (dataDO == null) {
                return;
            }

            dataDO = scorer.scoreForOne(run, dataDO, coord, rtMap, params);
//            sumList.add(judge(dataDO));
            dataList.add(dataDO);
            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(dataDO);

            //如果没有打分数据,那么对应的decoy也不再计算,以保持target与decoy 1:1的混合比例,这里需要注意的是,即便是scoreList是空,也需要将DataDO存储到数据库中,以便后续的重新统计和分析
            if (dataDO.getPeakGroupList() == null) {
                return;
            }

            coord.setDecoy(true);
            DataDO decoyData = extractOne(coord, rtMap, params);
            if (decoyData == null) {
                return;
            }

            //Step5. 对Decoy进行打分
            decoyData = scorer.scoreForOne(run, decoyData, coord, rtMap, params);
            dataList.add(decoyData);

            //Step6. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(decoyData);
        });

//        LogUtil.log("XIC+选峰+打分耗时", start);
//        log.info("新增组合碎片数目为:" + newIonsGroup.get());
//        log.info("总计构建Data数目" + dataList.size() + "/" + (coordinates.size() * 2) + "个");
//        if (dataList.stream().filter(data -> data.getStatus() == null).toList().size() > 0) {
//            log.info("居然有问题");
//        }
//        Result<List<DataSumDO>> result = dataSumService.insert(sumList, run.getProjectId());
//        if (result.isFailed()) {
//            log.error(result.getErrorMessage());
//        }
//        log.info("检测到肽段数目:" + sumList.stream().filter(sum -> sum.getStatus().equals(IdentifyStatus.SUCCESS.getCode())).count());
        return dataList;
    }

    private List<FragmentInfo> selectFragments(Map<String, FragmentInfo> fragMap, List<String> selectedIons) {
        List<FragmentInfo> fragmentInfos = new ArrayList<>();
        for (String selectedIon : selectedIons) {
            fragmentInfos.add(fragMap.get(selectedIon));
        }
        return fragmentInfos;
    }

    private DataDO buildData(DataDO data, List<String> selectedIons) {
        HashMap<String, float[]> selectedIntMap = new HashMap<>();
        HashMap<String, Float> selectedCutInfoMap = new HashMap<>();
        for (String cutInfo : selectedIons) {
            if (data.getIntMap().get(cutInfo) == null) {
                return null;
            }
            selectedIntMap.put(cutInfo, data.getIntMap().get(cutInfo));
            selectedCutInfoMap.put(cutInfo, data.getCutInfoMap().get(cutInfo));
        }
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

    private DataSumDO judge(DataDO dataDO) {

        DataSumDO sum = new DataSumDO(dataDO);
        PeakGroup finalPgs = null;
        List<PeakGroup> peakGroupList = dataDO.getPeakGroupList();
        if (peakGroupList != null && peakGroupList.size() > 0) {
            List<PeakGroup> candidateList = peakGroupList.stream().filter(PeakGroup::getFine).collect(Collectors.toList());
            if (candidateList.size() > 0) {
                finalPgs = candidateList.stream().sorted(Comparator.comparing(PeakGroup::getTotal).reversed()).collect(Collectors.toList()).get(0);
            }
        }

        if (finalPgs != null) {
            sum.setApexRt(finalPgs.getApexRt());
            sum.setSelectedRt(finalPgs.getSelectedRt());
            sum.setIntensitySum(finalPgs.getIntensitySum());
            sum.setIonsLow(finalPgs.getIonsLow());
            dataDO.setStatus(IdentifyStatus.SUCCESS.getCode());
            sum.setStatus(IdentifyStatus.SUCCESS.getCode());
        } else {
            if (dataDO.getStatus() == null || dataDO.getStatus().equals(IdentifyStatus.WAIT.getCode())) {
                sum.setStatus(IdentifyStatus.FAILED.getCode());
                dataDO.setStatus(IdentifyStatus.FAILED.getCode());
            } else {
                sum.setStatus(dataDO.getStatus());
            }
        }

        return sum;
    }

}
