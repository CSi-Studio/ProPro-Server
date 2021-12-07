package net.csibio.propro.algorithm.extract;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.bean.WindowRange;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.algorithm.core.CoreFunc;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.score.features.DIAScorer;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.AnyPair;
import net.csibio.propro.domain.bean.common.IntegerPair;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.vo.RunDataVO;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.ConvolutionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

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
    RunService runService;
    @Autowired
    DataService dataService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    Scorer scorer;
    @Autowired
    TaskService taskService;
    @Autowired
    CoreFunc coreFunc;
    @Autowired
    Lda lda;
    @Autowired
    DIAScorer diaScorer;

    /**
     * 根据coord肽段坐标读取run对应的aird文件中涉及的相关光谱图
     *
     * @param run
     * @return
     */
    public Result<TreeMap<Float, MzIntensityPairs>> getMS1Map(RunDO run) {
        Result checkResult = ConvolutionUtil.checkRun(run);
        if (checkResult.isFailed()) {
            log.error("条件检查失败:" + checkResult.getErrorMessage());
            return checkResult;
        }

        Compressor mzCompressor = run.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = run.fetchCompressor(Compressor.TARGET_INTENSITY);

        //Step1.获取窗口信息
        TreeMap<Float, MzIntensityPairs> rtMap;
        BlockIndexDO index = blockIndexService.getMS1(run.getId());
        if (index == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        DIAParser parser = null;
        try {
            parser = new DIAParser(run.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
            rtMap = parser.getSpectrums(index.getStartPtr(), index.getEndPtr(), index.getRts(), index.getMzs(), index.getInts());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.Error(ResultCode.PARSE_ERROR);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        return Result.OK(rtMap);
    }

    /**
     * 根据coord肽段坐标读取run对应的aird文件中涉及的相关光谱图
     *
     * @param run
     * @param coord
     * @return
     */
    public Result<TreeMap<Float, MzIntensityPairs>> getMS1Map(RunDO run, PeptideCoord coord) {
        Result checkResult = ConvolutionUtil.checkRun(run);
        if (checkResult.isFailed()) {
            log.error("条件检查失败:" + checkResult.getErrorMessage());
            return checkResult;
        }

        Compressor mzCompressor = run.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = run.fetchCompressor(Compressor.TARGET_INTENSITY);

        //Step1.获取窗口信息
        TreeMap<Float, MzIntensityPairs> rtMap;
        BlockIndexDO index = blockIndexService.getMS1(run.getId());
        if (index == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        DIAParser parser = null;
        try {
            parser = new DIAParser(run.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
            rtMap = parser.getSpectrumsByRtRange(index.getStartPtr(), index.getRts(), index.getMzs(), index.getInts(), (float) coord.getRtStart(), (float) coord.getRtEnd());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.Error(ResultCode.PARSE_ERROR);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        return Result.OK(rtMap);
    }

    /**
     * 根据coord肽段坐标读取run对应的aird文件中涉及的相关光谱图
     *
     * @param run
     * @param coord
     * @return
     */
    public Result<TreeMap<Float, MzIntensityPairs>> getMS2Map(RunDO run, PeptideCoord coord) {
        Result checkResult = ConvolutionUtil.checkRun(run);
        if (checkResult.isFailed()) {
            log.error("条件检查失败:" + checkResult.getErrorMessage());
            return checkResult;
        }

        Compressor mzCompressor = run.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = run.fetchCompressor(Compressor.TARGET_INTENSITY);

        //Step1.获取窗口信息
        TreeMap<Float, MzIntensityPairs> rtMap;
        BlockIndexDO index = blockIndexService.getMS2(run.getId(), coord.getMz());
        if (index == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        DIAParser parser = null;
        try {
            parser = new DIAParser(run.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
            rtMap = parser.getSpectrumsByRtRange(index.getStartPtr(), index.getRts(), index.getMzs(), index.getInts(), (float) coord.getRtStart(), (float) coord.getRtEnd());
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.Error(ResultCode.PARSE_ERROR);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        return Result.OK(rtMap);
    }

    /**
     * 提取XIC的核心函数,最终返回提取到XIC的Peptide数目
     * 目前只支持MS2的XIC提取
     *
     * @param params 将XIC提取,选峰及打分合并在一个步骤中执行,可以完整的省去一次IO读取及解析,提升分析速度,
     *               需要runDO,libraryId,rtExtractionWindow,mzExtractionWindow,SlopeIntercept
     */
    public Result<OverviewDO> extract(RunDO run, AnalyzeParams params) {
        Result<OverviewDO> resultDO = new Result(true);
        TaskDO task = params.getTaskDO();
        task.addLog("基本条件检查开始");
        Result checkResult = ConvolutionUtil.checkRun(run);
        if (checkResult.isFailed()) {
            return Result.Error(ResultCode.RUN_NOT_EXISTED);
        }

        OverviewDO overview = createOverview(run, params);

        //核心函数在这里
        extract(overview, run, params);
        overviewService.update(overview);
        resultDO.setData(overview);
        return resultDO;
    }

    /**
     * 实时提取某一个PeptideRef的XIC图谱
     * 其中Run如果没有包含irt结果,则会进行全rt进行搜索
     * 不适合用于大批量处理
     *
     * @param run
     * @param coord
     * @return
     */
    public Result<RunDataVO> predictOne(RunDO run, OverviewDO overview, PeptideCoord coord, AnalyzeParams params) {
        Double rt = coord.getRt();
        if (params.getMethod().getEic().getRtWindow() == -1) {
            coord.setRtRange(-1, 99999);
        } else {
            double targetRt = run.getIrt().getSi().realRt(rt);
            coord.setRtRange(targetRt - 300, targetRt + 300);
        }
        Result<TreeMap<Float, MzIntensityPairs>> ms1Result = getMS1Map(run, coord);
        Result<TreeMap<Float, MzIntensityPairs>> ms2Result = getMS2Map(run, coord);
        if (ms1Result.isFailed()) {
            return Result.Error(ms1Result.getErrorCode());
        }
        if (ms2Result.isFailed()) {
            return Result.Error(ms2Result.getErrorCode());
        }

        AnyPair<DataDO, DataSumDO> dataPair = coreFunc.predictOneNiubi(coord, ms1Result.getData(), ms2Result.getData(), run, overview, params);
//        AnyPair<DataDO, DataSumDO> dataPair = coreFunc.predictOneDelete(coord, ms2Result.getData(), run, overview, params);
        if (dataPair == null) {
            return Result.Error(ResultCode.ANALYSE_DATA_ARE_ALL_ZERO);
        }
        RunDataVO runDataVO = new RunDataVO().merge(dataPair.getLeft(), dataPair.getRight());
        if (runDataVO == null) {
            return Result.Error(ResultCode.ANALYSE_DATA_ARE_ALL_ZERO);
        }

        return Result.OK(runDataVO);
    }

    /**
     * 需要传入最终结果集的List对象
     * 最终的XIC结果存储在内存中不落盘,一般用于iRT的计算
     * 由于是直接在内存中的,所以XIC的结果不进行压缩
     *
     * @param finalList
     * @param coordinates
     * @param ms2Map
     * @param params
     */
    public void extract4Irt(List<DataDO> finalList, List<PeptideCoord> coordinates, TreeMap<Float, MzIntensityPairs> ms2Map, AnalyzeParams params) {
        for (PeptideCoord coord : coordinates) {
            DataDO data = eppsOne(coord, null, ms2Map, params);
            if (data != null) {
                finalList.add(data);
            }
        }
    }

    public DataDO eppsOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> ms1Map, TreeMap<Float, MzIntensityPairs> ms2Map, AnalyzeParams params) {
        return coreFunc.extract(coord, ms1Map, ms2Map, params, true, null);
    }


    /**
     * 提取MS2 XIC图谱并且输出最终结果,不返回最终的XIC结果以减少内存的使用
     *
     * @param overviewDO
     * @param run
     * @param params
     */
    private void extract(OverviewDO overviewDO, RunDO run, AnalyzeParams params) {
        TaskDO task = params.getTaskDO();
        //Step1.获取窗口信息
        List<WindowRange> ranges = run.getWindowRanges();
        BlockIndexQuery query = new BlockIndexQuery(run.getId(), 2);

        //获取所有MS2的窗口

        List<BlockIndexDO> blockIndexList = blockIndexService.getAll(query);
        blockIndexList = blockIndexList.stream().sorted(Comparator.comparing(blockIndexDO -> blockIndexDO.getRange().getStart())).toList();
        task.addLog("总计有窗口:" + ranges.size() + "个,开始进行MS2 提取XIC计算");
        taskService.update(task);
        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行XIC提取
        int count = 1;
        DIAParser parser = null;
        try {
            parser = new DIAParser(run.getAirdIndexPath());
            long peakCount = 0L;
            int dataCount = 0;
            Result<TreeMap<Float, MzIntensityPairs>> ms1Result = getMS1Map(run);
            if (ms1Result.isFailed()) {
                task.finish(TaskStatus.FAILED.getName(), ResultCode.PARSE_MS1_SPECTRUM_FAILED.getMessage());
                taskService.update(task);
                return;
            }
            TreeMap<Float, MzIntensityPairs> ms1Map = ms1Result.getData();

            for (BlockIndexDO index : blockIndexList) {
                List<DataDO> dataList = null;
                long start = System.currentTimeMillis();
                task.addLog("开始处理窗口:" + index.getRange().getStart() + "-" + index.getRange().getEnd() + ",当前轮数" + count + "/" + blockIndexList.size());
                //构建坐标
                List<PeptideCoord> coords = peptideService.buildCoord(params.getAnaLibId(), index.getRange(), params.getMethod().getEic().getRtWindow(), run.getIrt().getSi());
                if (coords.isEmpty()) {
                    task.addLog("No Coordinates Found,Rang:" + index.getRange().getStart() + ":" + index.getRange().getEnd());
                    taskService.update(task);
                    log.warn("No Coordinates Found,Rang:" + index.getRange().getStart() + ":" + index.getRange().getEnd());
                    continue;
                }
                //Step3.提取指定原始谱图
                TreeMap<Float, MzIntensityPairs> ms2Map = parser.getSpectrums(index.getStartPtr(), index.getEndPtr(), index.getRts(), index.getMzs(), index.getInts());
                if (params.getReselect()) {
                    dataList = coreFunc.reselect(run, coords, ms1Map, ms2Map, params);
                } else {
                    dataList = coreFunc.csi(run, coords, ms1Map, ms2Map, params);
                }

                if (dataList != null) {
                    for (DataDO dataDO : dataList) {
                        if (dataDO.getPeakGroupList() != null) {
                            peakCount += dataDO.getPeakGroupList().size();
                        }
                    }
                    dataCount += dataList.size();
                } else {
                    task.addLog("鉴定错误,有异常情况导致鉴定结果为空");
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
            e.printStackTrace();
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public void calcIonsCount(DataDO dataDO, PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, Float ionsLowLimit, Float ionsHighLimit) {
        String maxIon = coord.getFragments().get(0).getCutInfo();
        int[] ionsLow = new int[dataDO.getRtArray().length];
        int[] ionsHigh = new int[dataDO.getRtArray().length];
        for (int i = 0; i < dataDO.getRtArray().length; i++) {
            MzIntensityPairs pairs = rtMap.get(dataDO.getRtArray()[i]);
            float[] maxIntensities = dataDO.getIntMap().get(maxIon); //获取该spectrum中maxIon的强度列表
            float maxIonIntensityInThisSpectrum = 0;
            if (maxIntensities == null || maxIntensities.length == 0) {
                maxIonIntensityInThisSpectrum = Float.MAX_VALUE;
            } else {
                maxIonIntensityInThisSpectrum = maxIntensities[i];
            }

            IntegerPair pair = diaScorer.calcTotalIons(pairs,
                    coord.getUnimodMap(),
                    coord.getSequence(),
                    coord.getCharge(),
                    ionsLowLimit,
                    ionsHighLimit,
                    maxIonIntensityInThisSpectrum);
            ionsLow[i] = pair.left();
            ionsHigh[i] = pair.right();
        }

        dataDO.setIonsLow(ionsLow);
        dataDO.setIonsHigh(ionsHigh);
    }

    /**
     * 根据input入参初始化一个AnalyseOverviewDO
     *
     * @param params
     * @return
     */
    public OverviewDO createOverview(RunDO run, AnalyzeParams params) {
        OverviewDO overview = new OverviewDO();
        overview.setProjectId(run.getProjectId());
        overview.setRunId(run.getId());
        overview.setRunName(run.getName());
        overview.setParams(params);
        overview.setType(run.getType());
        overview.setAnaLibId(params.getAnaLibId());
        overview.setInsLibId(params.getInsLibId());
        overview.setName(run.getName() + "-" + params.getInsLibName() + "-" + params.getAnaLibName() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overview.setNote(params.getNote());
        overview.setReselect(params.getReselect());

        //是否是已存在的overview
        boolean exist = overviewService.exist(new OverviewQuery().setProjectId(run.getProjectId()).setRunId(run.getId()));
        if (!exist) {
            overview.setDefaultOne(true);
        }
        Result result = overviewService.insert(overview);
        if (result.isFailed()) {
            log.error("Insert Overview Exception: " + overview.getName() + "-" + result.getErrorMessage());
            return null;
        }
        params.setOverviewId(overview.getId());
        return overview;
    }
}
