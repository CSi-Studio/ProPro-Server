package net.csibio.propro.algorithm.irt;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.SimplePeptide;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.utils.ConvolutionUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component("irtByAnaLib")
public class IrtByAnaLib extends Irt {

    /**
     * XIC iRT内标库的数据
     *
     * @param exp
     * @param params
     * @return
     */
    @Override
    public List<DataDO> extract(ExperimentDO exp, AnalyzeParams params) {
        Result checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            log.error(checkResult.getErrorMessage());
            return null;
        }

        List<DataDO> finalDataList = new ArrayList<>();
        List<BlockIndexDO> blockList = blockIndexService.getAll(new BlockIndexQuery(exp.getId(), 2));
        blockList = blockList.stream().sorted(Comparator.comparing(a -> a.getRange().getStart())).collect(Collectors.toList());

        int selectPoints = params.getMethod().getIrt().getWantedNumber();
        int rangeSize = exp.getWindowRanges().size();
        selectPoints = Math.min(rangeSize, selectPoints);//获取windowRange Size大小,如果超过50的话则采用采样录取的方式
        int step = rangeSize / selectPoints;

        log.info("Irt Selected Points Count:" + selectPoints + "; Step:" + step);
        Compressor mzCompressor = exp.fetchCompressor(Compressor.TARGET_MZ);
        Compressor intCompressor = exp.fetchCompressor(Compressor.TARGET_INTENSITY);
        DIAParser parser = null;
        try {
            parser = new DIAParser(exp.getAirdPath(), mzCompressor, intCompressor, mzCompressor.getPrecision());
            for (int i = 0; i < selectPoints; i++) {
                log.info("第" + (i + 1) + "轮搜索开始");
                //Step1.按照步长获取SwathList的点位库
                BlockIndexDO swathIndexDO = blockList.get(i * step);
                //Step2.获取标准库的目标肽段片段的坐标
                //如果使用标准库进行卷积,为了缩短读取数据库的时间,每一轮从数据库中仅读取300个点位进行测试
                List<SimplePeptide> coordinates = peptideService.buildCoord4Irt(params.getIrtLibraryId(), swathIndexDO.getRange());
                if (coordinates.size() == 0) {
                    log.warn("No iRT Targets Found,Rang:" + swathIndexDO.getRange().getStart() + ":" + swathIndexDO.getRange().getEnd());
                    continue;
                }

                //Step3&4.提取指定原始谱图, 提取数据并且存储数据,如果传入的库是标准库,那么使用采样的方式进行数据提取. 如果是使用标准库进行校准的,那么会按照需要选择的总点数进行抽取选择
                try {
                    TreeMap<Float, MzIntensityPairs> rtMap = parser.getSpectrums(swathIndexDO.getStartPtr(), swathIndexDO.getEndPtr(), swathIndexDO.getRts(), swathIndexDO.getMzs(), swathIndexDO.getInts());
                    extractor.extract4IrtByLib(finalDataList, coordinates, rtMap, params);
                } catch (Exception e) {
                    log.error("Precursor m/z start:" + swathIndexDO.getRange().getStart());
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
