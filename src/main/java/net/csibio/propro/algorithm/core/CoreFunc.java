package net.csibio.propro.algorithm.core;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.utils.ConvolutionUtil;
import net.csibio.propro.utils.DataUtil;
import net.csibio.propro.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component("coreFunc")
public class CoreFunc {

    @Autowired
    Scorer scorer;

    /**
     * EIC Core Function
     * 核心EIC函数
     * <p>
     * 本函数为整个分析过程中最耗时的步骤
     *
     * @param coord
     * @param rtMap
     * @param params
     * @param overviewId
     * @return
     */
    public DataDO extractOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params, String overviewId) {
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

        DataDO data = new DataDO();
        data.setProteins(coord.getProteins());
        data.setRtArray(rtArray);
        data.setOverviewId(overviewId);
        data.setPeptideRef(coord.getPeptideRef());
        data.setDecoy(coord.isDecoy());
        data.setLibRt(coord.getRt());
        try {
            data.setCutInfoMap(coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, f -> f.getMz().floatValue())));
        } catch (Exception e) {
            e.printStackTrace();
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
                // data.getIntMap().put(fi.getCutInfo(), null);
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
     * EIC+PEAK_PICKER+PEAK_SCORE 核心流程
     * 最终的提取XIC结果需要落盘数据库,一般用于正式XIC提取的计算
     *
     * @param coordinates
     * @param rtMap
     * @param params
     * @return
     */
    public List<DataDO> epps(ExperimentDO exp, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        List<DataDO> dataList = Collections.synchronizedList(new ArrayList<>());
        long start = System.currentTimeMillis();
        if (coordinates == null || coordinates.size() == 0) {
            log.error("肽段坐标为空");
            return null;
        }
        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段.如果先处理的真肽段没有被提取到任何信息,或者提取后的峰太差被忽略掉,都会同时删掉对应的伪肽段的XIC
        coordinates.parallelStream().forEach(coord -> {
            //Step1. 常规提取XIC,XIC结果不进行压缩处理,如果没有提取到任何结果,那么加入忽略列表
            DataDO dataDO = extractOne(coord, rtMap, params, params.getOverviewId());
            if (dataDO == null) {
                log.info(coord.getPeptideRef() + ":EIC结果为空");
                return;
            }

            //Step2. 常规选峰及打分,未满足条件的直接忽略
            scorer.scoreForOne(exp, dataDO, coord, rtMap, params);
            dataList.add(dataDO);
            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            DataUtil.compress(dataDO);
            //如果没有打分数据,那么对应的decoy也不再计算,以保持target与decoy 1:1的混合比例
            if (dataDO.getScoreList() == null) {
                return;
            }

            //Step4. 如果第一,二步均符合条件,那么开始对对应的伪肽段进行数据提取和打分
            coord.setDecoy(true);
            DataDO decoyData = extractOne(coord, rtMap, params, params.getOverviewId());
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
        return dataList;
    }
}
