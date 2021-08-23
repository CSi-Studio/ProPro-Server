package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.blockindex.BlockIndexVO;
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

        List<BlockIndexVO> allIndexList = blockIndexService.getAll(query, BlockIndexVO.class);
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

        ExperimentDO experiment = experimentService.getById(blockIndex.getExpId());
        if (experiment == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }

        Compressor mzCompressor = experiment.fetchCompressor(Compressor.TARGET_MZ);
        DIAParser parser = new DIAParser(experiment.getAirdPath(), mzCompressor, experiment.fetchCompressor(Compressor.TARGET_INTENSITY), mzCompressor.getPrecision());
        MzIntensityPairs pairs = parser.getSpectrumByRt(blockIndex.getStartPtr(), blockIndex.getRts(), blockIndex.getMzs(), blockIndex.getInts(), rt);
        parser.close();
        return Result.OK(new FloatPairs(pairs.getMzArray(), pairs.getIntensityArray()));
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

}
