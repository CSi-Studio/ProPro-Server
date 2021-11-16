package net.csibio.propro.service.impl;

import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.BlockIndexDAO;
import net.csibio.propro.domain.bean.common.AnyPair;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.utils.ArrayUtil;
import net.csibio.propro.utils.ConvolutionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Service("blockIndexService")
public class BlockIndexServiceImpl implements BlockIndexService {

    @Autowired
    BlockIndexDAO blockIndexDAO;

    @Override
    public List<BlockIndexDO> getAllByRunId(String runId) {
        BlockIndexQuery query = new BlockIndexQuery();
        query.setRunId(runId);
        return blockIndexDAO.getAll(query);
    }

    @Override
    public List<BlockIndexDO> getAllMS2ByRunId(String runId) {
        BlockIndexQuery query = new BlockIndexQuery();
        query.setRunId(runId);
        query.setLevel(2);
        List<BlockIndexDO> indexList = blockIndexDAO.getAll(query);
        indexList = indexList.stream().filter(b -> b.getRange() != null).toList();
        return indexList;
    }

    @Override
    public AnyPair<Float, Float> getNearestSpectrumByRt(TreeMap<Float, MzIntensityPairs> rtMap, Double rt) {
        float[] fArray = ArrayUtil.toPrimitive(rtMap.keySet());
        int rightIndex = ConvolutionUtil.findRightIndex(fArray, rt.floatValue());
        int finalIndex = rightIndex;
        if (rightIndex == -1) {
            //Max value in fArray is less than rt. The max index of fArray is the nearest index.
            finalIndex = fArray.length - 1;
            return new AnyPair<Float, Float>(fArray[finalIndex], fArray[finalIndex]);
        } else if (rightIndex != 0 && (fArray[rightIndex] - rt) > (fArray[rightIndex - 1] - rt)) {
            //if rightIndex == 0, finalIndex == 0
            finalIndex = rightIndex - 1;
        }
        return new AnyPair<Float, Float>(fArray[finalIndex], fArray[finalIndex + 1]);
    }

    @Override
    public BlockIndexDO getOne(String runId, Double mz) {
        BlockIndexQuery query = new BlockIndexQuery(runId, 2);
        query.setMz(mz);
        return blockIndexDAO.getOne(query);
    }

    /**
     * 本函数用于ScanningSwath的数据解析,deltaMz是ScanningSwath的窗口宽度,CollectedNumber是需要获取的相邻的Swath窗口的数目
     * 例如CollectedNumber=3,则意味着需要获取向上向下各3个窗口的数据,总计额外获取6个窗口的数据
     *
     * @param runId
     * @param mz
     * @param deltaMz
     * @param collectedNumber
     * @return
     */
    @Override
    public List<BlockIndexDO> getLinkedBlockIndex(String runId, Double mz, Double deltaMz, Integer collectedNumber) {
        List<BlockIndexDO> indexList = new ArrayList<>();
        BlockIndexDO index0 = getOne(runId, mz);
        indexList.add(index0);
        for (int i = 1; i <= collectedNumber; i++) {
            BlockIndexDO index1 = getOne(runId, mz - deltaMz * i);
            if (index1 != null) {
                indexList.add(index1);
            }
            BlockIndexDO index2 = getOne(runId, mz + deltaMz * i);
            if (index2 != null) {
                indexList.add(index2);
            }
        }

        return indexList;
    }

    @Override
    public BaseDAO<BlockIndexDO, BlockIndexQuery> getBaseDAO() {
        return blockIndexDAO;
    }

    @Override
    public void beforeInsert(BlockIndexDO blockIndexDO) throws XException {
        if (blockIndexDO.getRunId() == null) {
            throw new XException(ResultCode.RUN_ID_CANNOT_BE_EMPTY);
        }
    }

    @Override
    public void beforeUpdate(BlockIndexDO blockIndexDO) throws XException {
        if (blockIndexDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (blockIndexDO.getRunId() == null) {
            throw new XException(ResultCode.RUN_ID_CANNOT_BE_EMPTY);
        }
    }

    @Override
    public void beforeRemove(String id) throws XException {
        if (id == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
    }
}
