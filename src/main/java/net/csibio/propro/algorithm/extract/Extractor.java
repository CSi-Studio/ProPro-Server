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
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.ConvolutionUtil;
import net.csibio.propro.utils.DataUtil;
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
     * 根据coord肽段坐标读取exp对应的aird文件中涉及的相关光谱图
     *
     * @param exp
     * @param coord
     * @return
     */
    public Result<TreeMap<Float, MzIntensityPairs>> getRtMap(ExperimentDO exp, PeptideCoord coord) {
        Result checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            log.error("条件检查失败:" + checkResult.getErrorMessage());
            return checkResult;
        }

        Compressor mzCompressor = exp.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = exp.fetchCompressor(Compressor.TARGET_INTENSITY);

        //Step1.获取窗口信息
        TreeMap<Float, MzIntensityPairs> rtMap;
        BlockIndexDO index = blockIndexService.getOne(exp.getId(), coord.getMz().floatValue());
        if (index == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        DIAParser parser = null;
        try {
            parser = new DIAParser(exp.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
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
     * 实时提取某一个PeptideRef的XIC图谱
     * 其中Exp如果没有包含irt结果,则会进行全rt进行搜索
     * 不适合用于大批量处理
     *
     * @param exp
     * @param coord
     * @return
     */
    public Result<DataDO> extractOne(ExperimentDO exp, PeptideCoord coord, AnalyzeParams params) {
        Result<TreeMap<Float, MzIntensityPairs>> rtMapResult = getRtMap(exp, coord);
        if (rtMapResult.isFailed()) {
            return Result.Error(ResultCode.PARSE_ERROR);
        }

        Double rt = coord.getRt();
        if (params.getMethod().getEic().getRtWindow() == -1) {
            coord.setRtStart(-1);
            coord.setRtEnd(99999);
        } else {
            Double targetRt = exp.getIrt().getSi().realRt(rt);
            coord.setRtStart(targetRt - params.getMethod().getEic().getRtWindow());
            coord.setRtEnd(targetRt + params.getMethod().getEic().getRtWindow());
        }

        DataDO dataDO = extractOne(coord, rtMapResult.getData(), params, null);
        if (dataDO == null) {
            return Result.Error(ResultCode.ANALYSE_DATA_ARE_ALL_ZERO);
        }

        return Result.OK(dataDO);
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
        for (PeptideCoord coord : coordinates) {
            DataDO data = extractOne(coord, rtMap, params);
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

            if (dataDO.getScoreList() != null) {
                finalList.add(dataDO);
                log.info("第" + i + "次搜索找到了:" + dataDO.getPeptideRef() + ",BestRT:" + dataDO.getScoreList().get(0).getRt() + "耗时:" + (System.currentTimeMillis() - start));
                count++;
            }
        }
    }

    public DataDO extractOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params) {
        return extractOne(coord, rtMap, params, null);
    }

    private DataDO extractOne(PeptideCoord coord, TreeMap<Float, MzIntensityPairs> rtMap, AnalyzeParams params, String overviewId) {
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
                //如果该cutInfo没有XIC到任何数据,则不存入IntMap中
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
                        if (dataDO.getScoreList() != null) {
                            peakCount += dataDO.getScoreList().size();
                        }
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
            e.printStackTrace();
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
        List<PeptideCoord> coords;
        TreeMap<Float, MzIntensityPairs> rtMap;
        //Step2.获取标准库的目标肽段片段的坐标
        coords = peptideService.buildCoord(params.getAnaLibId(), swathIndex.getRange(), params.getMethod().getEic().getRtWindow(), exp.getIrt().getSi());
        if (coords.isEmpty()) {
            log.warn("No Coordinates Found,Rang:" + swathIndex.getRange().getStart() + ":" + swathIndex.getRange().getEnd());
            return null;
        }
        //Step3.提取指定原始谱图
        rtMap = parser.getSpectrums(swathIndex.getStartPtr(), swathIndex.getEndPtr(), swathIndex.getRts(), swathIndex.getMzs(), swathIndex.getInts());
        return epps(exp, coords, rtMap, params);
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

        boolean exist = overviewService.exist(new OverviewQuery().setProjectId(exp.getProjectId()).setExpId(exp.getId()));
        if (!exist) {
            overview.setDefaultOne(true);
        }
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
