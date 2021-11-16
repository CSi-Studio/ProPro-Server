package net.csibio.propro.service;

import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.domain.bean.common.AnyPair;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.query.BlockIndexQuery;

import java.util.List;
import java.util.TreeMap;

public interface BlockIndexService extends BaseService<BlockIndexDO, BlockIndexQuery> {

    List<BlockIndexDO> getAllByRunId(String runId);

    List<BlockIndexDO> getAllMS2ByRunId(String runId);

    AnyPair<Float, Float> getNearestSpectrumByRt(TreeMap<Float, MzIntensityPairs> rtMap, Double rt);

    BlockIndexDO getOne(String runId, Double mz);

    List<BlockIndexDO> getLinkedBlockIndex(String runId, Double mz, Double deltaMz, Integer collectedNumber);
}
