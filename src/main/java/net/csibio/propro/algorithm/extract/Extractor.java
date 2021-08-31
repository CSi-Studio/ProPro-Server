package net.csibio.propro.algorithm.extract;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.AnalyseUtil;
import net.csibio.propro.utils.ConvolutionUtil;
import net.csibio.propro.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("extractor")
public class Extractor {

    @Autowired
    LibraryService libraryService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    DataService dataService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    Scorer scorer;
    @Autowired
    TaskService taskService;

    /**
     * 提取XIC的核心函数,最终返回提取到XIC的Peptide数目
     * 目前只支持MS2的XIC提取
     *
     * @param params 将XIC提取,选峰及打分合并在一个步骤中执行,可以完整的省去一次IO读取及解析,提升分析速度,
     *               需要experimentDO,libraryId,rtExtractionWindow,mzExtractionWindow,SlopeIntercept
     */
    public Result<OverviewDO> extract(ExperimentDO exp, AnalyzeParams params) {
        Result<OverviewDO> resultDO = new Result(true);
        TaskDO task = params.getTaskDO();
        task.addLog("基本条件检查开始");
        Result checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }

        OverviewDO overviewDO = createOverview(exp, params);

        //核心函数在这里
        extract(overviewDO, exp, params);
        overviewService.update(overviewDO);
        resultDO.setData(overviewDO);
        return resultDO;
    }

    /**
     * 实时提取某一个PeptideRef的XIC图谱,即全时间段XIC提取
     * 不适合用于大批量处理
     *
     * @param exp
     * @param peptide
     * @return
     */
    public Result<DataDO> extractOne(ExperimentDO exp, PeptideDO peptide, AnalyzeParams params) {
        Result checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            log.error("条件检查失败:" + checkResult.getErrorMessage());
            return checkResult;
        }

        Compressor mzCompressor = exp.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = exp.fetchCompressor(Compressor.TARGET_INTENSITY);
        DIAParser parser = new DIAParser(exp.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
        //Step1.获取窗口信息.
        BlockIndexQuery query = new BlockIndexQuery(exp.getId(), 2);
        query.setMz(peptide.getMz().floatValue());
        BlockIndexDO swathIndexDO;
        TreeMap<Float, MzIntensityPairs> rtMap;
        swathIndexDO = blockIndexService.getOne(exp.getId(), peptide.getMz().floatValue());
        if (swathIndexDO == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        rtMap = parser.getSpectrums(swathIndexDO.getStartPtr(), swathIndexDO.getEndPtr(), swathIndexDO.getRts(), swathIndexDO.getMzs(), swathIndexDO.getInts());

        parser.close();

        PeptideCoord tp = new PeptideCoord(peptide);
        Double rt = peptide.getRt();
        if (params.getMethod().getEic().getRtWindow() == -1) {
            tp.setRtStart(-1);
            tp.setRtEnd(99999);
        } else {
            Double targetRt = (rt - exp.getIrt().getSi().getIntercept()) / exp.getIrt().getSi().getSlope();
            tp.setRtStart(targetRt - params.getMethod().getEic().getRtWindow());
            tp.setRtEnd(targetRt + params.getMethod().getEic().getRtWindow());
        }

        DataDO dataDO = extractOne(tp, rtMap, params, null);
        if (dataDO == null) {
            return Result.Error(ResultCode.ANALYSE_DATA_ARE_ALL_ZERO);
        }

        Result<DataDO> resultDO = new Result<DataDO>(true);
        resultDO.setData(dataDO);
        return resultDO;
    }

    /**
     * 需要传入最终结果集的List对象
     * 最终的XIC结果存储在内存中不落盘,一般用于iRT的计算
     * 由于是直接在内存中的,所以XIC的结果不进行压缩
     *
     * @param finalList
     * @param coordinates
     * @param rtMap
     * @param params
     */
    public void extract4Irt(List<DataDO> finalList, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        for (PeptideCoord tp : coordinates) {
            DataDO data = extractOne(tp, rtMap, params);
            if (data != null) {
                finalList.add(data);
            }
        }
    }

    public void extract4IrtByLib(List<DataDO> finalList, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < coordinates.size(); i++) {
            if (count >= 1) {
                break;
            }
            if (coordinates.get(i).getSequence().length() <= 13) {
                continue;
            }
            DataDO dataDO = extractOne(coordinates.get(i), rtMap, params, null);
            if (dataDO == null) {
                continue;
            }
            scorer.strictScoreForOne(dataDO, coordinates.get(i), params.getMethod().getQuickFilter().getMinShapeScore());

            if (dataDO.getFeatureScoresList() != null) {
                finalList.add(dataDO);
                log.info("第" + i + "次搜索找到了:" + dataDO.getPeptideRef() + ",BestRT:" + dataDO.getFeatureScoresList().get(0).getRt() + "耗时:" + (System.currentTimeMillis() - start));
                count++;
            }
        }
    }

    private DataDO extractOne(PeptideCoord tp, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        return extractOne(tp, rtMap, params, null);
    }

    private DataDO extractOne(PeptideCoord sp, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params, String overviewId) {
        float mzStart = 0;
        float mzEnd = -1;
        //所有的碎片共享同一个RT数组
        ArrayList<Float> rtList = new ArrayList<>();
        for (Float rt : rtMap.keySet()) {
            if (params.getMethod().getEic().getRtWindow() != -1 && rt > sp.getRtEnd()) {
                break;
            }
            if (params.getMethod().getEic().getRtWindow() == -1 || (rt >= sp.getRtStart() && rt <= sp.getRtEnd())) {
                rtList.add(rt);
            }
        }

        Float[] rtArray = new Float[rtList.size()];
        rtList.toArray(rtArray);

        DataDO data = new DataDO();
        data.setRtArray(rtArray);
        data.setOverviewId(overviewId);
        data.setPeptideRef(sp.getPeptideRef());
        data.setDecoy(sp.isDecoy());
        data.setLibRt(sp.getRt());
//        data.setLibMz(sp.getMz());
        data.setCutInfos(sp.getFragments().stream().map(FragmentInfo::getCutInfo).collect(Collectors.toList()));

        boolean isHit = false;
        float window = params.getMethod().getEic().getMzWindow().floatValue();
        Boolean adaptiveMzWindow = params.getMethod().getEic().getAdaptiveMzWindow();

        for (FragmentInfo fi : sp.getFragments()) {
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
                data.getIntensityMap().put(fi.getCutInfo(), null);
            } else {
                isHit = true;
                data.getIntensityMap().put(fi.getCutInfo(), intArray); //记录每一个碎片的光谱图
            }
            //data.getMzMap().put(fi.getCutInfo(), fi.getMz().floatValue());
        }

        //如果所有的片段均没有提取到XIC的结果,则直接返回null
        if (!isHit) {
            return null;
        }

        return data;
    }

    /**
     * 提取MS2 XIC图谱并且输出最终结果,不返回最终的XIC结果以减少内存的使用
     *
     * @param overviewDO
     * @param exp
     * @param analyzeParams
     */
    private void extract(OverviewDO overviewDO, ExperimentDO exp, AnalyzeParams analyzeParams) {
        TaskDO task = analyzeParams.getTaskDO();
        //Step1.获取窗口信息
        List<WindowRange> ranges = exp.getWindowRanges();
        BlockIndexQuery query = new BlockIndexQuery(exp.getId(), 2);

        //获取所有MS2的窗口
        List<BlockIndexDO> blockIndexList = blockIndexService.getAll(query);
        blockIndexList = blockIndexList.stream().sorted(Comparator.comparing(blockIndexDO -> blockIndexDO.getRange().getStart())).toList();
        task.addLog("总计有窗口:" + ranges.size() + "个,开始进行MS2 提取XIC计算");
        taskService.update(task);
        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行XIC提取
        int count = 1;
        DIAParser parser = null;
        try {
            parser = new DIAParser(exp.getAirdIndexPath());
            long peakCount = 0L;
            int dataCount = 0;
            for (BlockIndexDO index : blockIndexList) {
                long start = System.currentTimeMillis();
                task.addLog("开始处理窗口:" + index.getRange().getStart() + "-" + index.getRange().getEnd());
                List<DataDO> dataList = doExtract(parser, exp, index, analyzeParams);
                if (dataList != null) {
                    for (DataDO dataDO : dataList) {
                        peakCount += dataDO.getFeatureScoresList().size();
                    }
                    dataCount += dataList.size();
                }
                dataService.insert(dataList, overviewDO.getProjectId());
                task.addLog("第" + count + "轮数据XIC提取完毕,Range:[" + index.getRange().getStart() + "," + index.getRange().getEnd() + "],有效肽段:" + (dataList == null ? 0 : dataList.size()) + "个,耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒");
                taskService.update(task);
                count++;
            }

            log.info("Total Peptide Count:" + dataCount);
            log.info("Total Peak Count:" + peakCount);
            overviewDO.getStatistic().put(StatConst.TOTAL_PEPTIDE_COUNT, dataCount);
            overviewDO.getStatistic().put(StatConst.TOTAL_PEAK_COUNT, peakCount);
            overviewService.update(overviewDO);

        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    /**
     * 返回提取到的数目
     *
     * @param parser
     * @param swathIndex
     * @param params
     * @return
     */
    private List<DataDO> doExtract(DIAParser parser, ExperimentDO exp, BlockIndexDO swathIndex, AnalyzeParams params) {
        List<PeptideCoord> coordinates;
        TreeMap<Float, MzIntensityPairs> rtMap;
        //Step2.获取标准库的目标肽段片段的坐标
        coordinates = peptideService.buildCoord(params.getAnaLibId(), swathIndex.getRange(), params.getMethod().getEic().getRtWindow(), exp.getIrt().getSi());
        if (coordinates.isEmpty()) {
            log.warn("No Coordinates Found,Rang:" + swathIndex.getRange().getStart() + ":" + swathIndex.getRange().getEnd());
            return null;
        }
        //Step3.提取指定原始谱图
        rtMap = parser.getSpectrums(swathIndex.getStartPtr(), swathIndex.getEndPtr(), swathIndex.getRts(), swathIndex.getMzs(), swathIndex.getInts());
        return epps(exp, coordinates, rtMap, params);

    }

    /**
     * 最终的提取XIC结果需要落盘数据库,一般用于正式XIC提取的计算
     *
     * @param coordinates
     * @param rtMap
     * @param params
     * @return
     */
    private List<DataDO> epps(ExperimentDO exp, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        List<DataDO> dataList = Collections.synchronizedList(new ArrayList<>());
        long start = System.currentTimeMillis();
        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段.如果先处理的真肽段没有被提取到任何信息,或者提取后的峰太差被忽略掉,都会同时删掉对应的伪肽段的XIC
        coordinates.parallelStream().forEach(sp -> {
            //Step1. 常规提取XIC,XIC结果不进行压缩处理,如果没有提取到任何结果,那么加入忽略列表
            DataDO dataDO = extractOne(sp, rtMap, params, params.getOverviewId());
            if (dataDO == null) {
                return;
            }

            //Step2. 常规选峰及打分,未满足条件的直接忽略
            scorer.scoreForOne(exp, dataDO, sp, rtMap, params);
            if (dataDO.getFeatureScoresList() == null) {
                return;
            }

            //Step3. 忽略过程数据,将数据提取结果加入最终的列表
            AnalyseUtil.compress(dataDO);
            dataList.add(dataDO);

            //Step4. 如果第一,二步均符合条件,那么开始对对应的伪肽段进行数据提取和打分
            sp.setDecoy(true);
            DataDO decoyData = extractOne(sp, rtMap, params, params.getOverviewId());
            if (decoyData == null) {
                return;
            }

            //Step5. 对Decoy进行打分
            scorer.scoreForOne(exp, decoyData, sp, rtMap, params);
            if (decoyData.getFeatureScoresList() == null) {
                return;
            }

            //Step6. 忽略过程数据,将数据提取结果加入最终的列表
            AnalyseUtil.compress(decoyData);
            dataList.add(decoyData);
        });

        LogUtil.log("提取XIC+选峰+打分耗时", start);
        return dataList;
    }

    /**
     * 根据input入参初始化一个AnalyseOverviewDO
     *
     * @param params
     * @return
     */
    public OverviewDO createOverview(ExperimentDO exp, AnalyzeParams params) {
        OverviewDO overview = new OverviewDO();
        overview.setProjectId(exp.getProjectId());
        overview.setExpId(exp.getId());
        overview.setExpName(exp.getName());
        overview.setParams(params);
        overview.setType(exp.getType());
        overview.setAnaLibId(params.getAnaLibId());
        overview.setInsLibId(params.getInsLibId());
        overview.setName(exp.getName() + "-" + params.getInsLibName() + "-" + params.getAnaLibName() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overview.setNote(params.getNote());
        Result result = overviewService.insert(overview);
        if (result.isFailed()) {
            log.error(result.getErrorMessage());
            return null;
        }
        if (overview.getId() == null) {
            log.error("插入有问题");
        }
        params.setOverviewId(overview.getId());
        return overview;
    }
}
