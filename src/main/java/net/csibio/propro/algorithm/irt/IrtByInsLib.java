package net.csibio.propro.algorithm.irt;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.utils.ConvolutionUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component("irtByInsLib")
@Slf4j
public class IrtByInsLib extends Irt {

    /**
     * XIC iRT内标库的数据
     *
     * @param run
     * @param params
     * @return
     */
    @Override
    public List<DataDO> extract(RunDO run, AnalyzeParams params) {
        Result checkResult = ConvolutionUtil.checkRun(run);
        if (checkResult.isFailed()) {
            log.error(checkResult.getErrorMessage());
            return null;
        }

        List<DataDO> finalDataList = new ArrayList<>();
        List<BlockIndexDO> blockList = blockIndexService.getAllMS2ByRunId(run.getId());
        blockList = blockList.stream().sorted(Comparator.comparing(a -> a.getRange().getStart())).collect(Collectors.toList());
        Compressor mzCompressor = run.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = run.fetchCompressor(Compressor.TARGET_INTENSITY);
        DIAParser parser = null;
        try {
            parser = new DIAParser(run.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
            for (int i = 0; i < blockList.size(); i++) {
                log.info("第" + (i + 1) + "轮搜索开始");
                //Step1.按照步长获取SwathList的点位库
                BlockIndexDO blockIndex = blockList.get(i);
                //Step2.获取标准库的目标肽段片段的坐标
                List<PeptideCoord> coords = peptideService.buildCoord4Irt(params.getInsLibId(), blockIndex.getRange());
                if (coords.size() == 0) {
                    log.warn("No iRT Targets Found,Rang:" + blockIndex.getRange().getStart() + ":" + blockIndex.getRange().getEnd());
                    continue;
                }

                //Step3&4.提取指定原始谱图,提取数据并且存储数据,如果传入的库是标准库,那么使用采样的方式进行数据提取
                try {
                    TreeMap<Float, MzIntensityPairs> ms2Map = parser.getSpectrums(blockIndex.getStartPtr(), blockIndex.getEndPtr(), blockIndex.getRts(), blockIndex.getMzs(), blockIndex.getInts());
                    extractor.extract4Irt(finalDataList, coords, ms2Map, params);
                } catch (Exception e) {
                    log.error("Parsing Error!!Precursor m/z start:" + blockIndex.getRange().getStart());
                    throw e;
                }
            }
        } finally {
            if (parser != null) {
                parser.close(); //使用完毕以后关闭parser
            }
        }
        return finalDataList;
    }
}
