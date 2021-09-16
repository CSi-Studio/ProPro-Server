package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.constants.constant.SmoothConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.blockindex.BlockIndexVO;
import net.csibio.propro.domain.bean.common.DoubleTreble;
import net.csibio.propro.domain.bean.common.FloatPairs;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.ExperimentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = {"Block Index Module"})
@RestController
@RequestMapping("blockindex/")
public class BlockIndexController {

    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    ExperimentService experimentService;

    @GetMapping(value = "/list")
    Result list(BlockIndexQuery query) {
        if (StringUtils.isEmpty(query.getExpId())) {
            return Result.Error(ResultCode.EXPERIMENT_ID_CANNOT_BE_EMPTY);
        }

        long startX = System.currentTimeMillis();
        List<BlockIndexVO> allIndexList = blockIndexService.getAll(query, BlockIndexVO.class);
        System.out.println("Time Cost:" + (System.currentTimeMillis() - startX));
        // allIndexList = allIndexList.stream().sorted(Comparator.comparing(index -> index.getRange().getStart())).toList();
        return Result.OK(allIndexList);
    }

    @GetMapping(value = "/detail")
    Result detail(@RequestParam("id") String id) {
        BlockIndexDO blockIndex = blockIndexService.getById(id);
        if (blockIndex == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }
        return Result.OK(blockIndex);
    }

    @GetMapping(value = "/spectrum")
    Result spectrum(@RequestParam("blockIndexId") String blockIndexId,
                    @RequestParam("rt") float rt) {
        BlockIndexDO blockIndex = blockIndexService.getById(blockIndexId);
        if (blockIndex == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }

        ExperimentDO exp = experimentService.getById(blockIndex.getExpId());
        if (exp == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }

        FloatPairs pairs = experimentService.getSpectrum(exp, blockIndex, rt);
        return Result.OK(pairs);
    }

    @GetMapping(value = "/spectrums")
    Result spectrums(@RequestParam("blockIndexId") String blockIndexId,
                     @RequestParam("rtList") List<Float> rtList) {
        BlockIndexDO blockIndex = blockIndexService.getById(blockIndexId);
        if (blockIndex == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }

        ExperimentDO experiment = experimentService.getById(blockIndex.getExpId());
        if (experiment == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }

        Compressor mzCompressor = experiment.fetchCompressor(Compressor.TARGET_MZ);
        DIAParser parser = new DIAParser(experiment.getAirdPath(), mzCompressor, experiment.fetchCompressor(Compressor.TARGET_INTENSITY), mzCompressor.getPrecision());
        List<FloatPairs> pairsList = new ArrayList<>();
        try {
            for (float rt : rtList) {
                MzIntensityPairs pairs = parser.getSpectrumByRt(blockIndex.getStartPtr(), blockIndex.getRts(), blockIndex.getMzs(), blockIndex.getInts(), rt);
                pairsList.add(new FloatPairs(pairs.getMzArray(), pairs.getIntensityArray()));
                parser.close();
            }
        } finally {
            parser.close();
        }

        return Result.OK(pairsList);
    }

    @GetMapping(value = "/spectrumGauss")
    Result spectrumGauss(@RequestParam("blockIndexId") String blockIndexId,
                         @RequestParam("rt") float rt,
                         @RequestParam("pointNum") int pointNum) {
        BlockIndexDO blockIndex = blockIndexService.getById(blockIndexId);
        if (blockIndex == null) {
            return Result.Error(ResultCode.BLOCK_INDEX_NOT_EXISTED);
        }

        ExperimentDO experiment = experimentService.getById(blockIndex.getExpId());
        if (experiment == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }

        Compressor mzCompressor = experiment.fetchCompressor(Compressor.TARGET_MZ);
        DIAParser parser = new DIAParser(experiment.getAirdPath(), mzCompressor, experiment.fetchCompressor(Compressor.TARGET_INTENSITY), mzCompressor.getPrecision());
        MzIntensityPairs pairs = parser.getSpectrumByRt(blockIndex.getStartPtr(), blockIndex.getRts(), blockIndex.getMzs(), blockIndex.getInts(), rt);
        parser.close();
        //对光谱进行高斯平滑
        float[] mzFloat = pairs.getMzArray();
        float[] intFloat = pairs.getIntensityArray();
        double[] mzArray = new double[mzFloat.length];
        double[] intArray = new double[intFloat.length];
        for (int i = 0; i < mzArray.length; i++) {
            mzArray[i] = mzFloat[i];
            intArray[i] = intFloat[i];
        }
        double[] weights = SmoothConst.GAUSS.get(pointNum);
        double[] smoothInts = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            //mid
            double sum = intArray[i] * weights[0];
            //left
            for (int j = 1; j < weights.length; j++) {
                if (i - j < 0) {
                    break;
                }
                sum += weights[j] * intArray[i - j];
            }
            //right
            for (int j = 1; j < weights.length; j++) {
                if (i + j >= intArray.length) {
                    break;
                }
                sum += weights[j] * intArray[i + j];
            }
            smoothInts[i] = sum;
        }
        return Result.OK(new DoubleTreble(mzArray, intArray, smoothInts));
    }

}
