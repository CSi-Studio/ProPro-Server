package net.csibio.propro.service.impl;

import com.alibaba.fastjson.JSONObject;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.BlockIndex;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.aird.parser.DIAParser;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.RunDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.FloatPairs;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.run.RunIrt;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.domain.query.RunQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.RunService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.FileUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("runService")
public class RunServiceImpl implements RunService {

    @Autowired
    RunDAO runDAO;
    @Autowired
    TaskService taskService;
    @Autowired
    BlockIndexService blockIndexService;

    @Override
    public BaseDAO<RunDO, RunQuery> getBaseDAO() {
        return runDAO;
    }

    @Override
    public void beforeInsert(RunDO runDO) throws XException {
        if (runDO.getName() == null) {
            throw new XException(ResultCode.RUN_NAME_CANNOT_BE_EMPTY);
        }
        if (runDO.getType() == null) {
            throw new XException(ResultCode.RUN_TYPE_MUST_DEFINE);
        }
        if (runDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        runDO.setCreateDate(new Date());
        runDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(RunDO runDO) throws XException {
        if (runDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (runDO.getName() == null) {
            throw new XException(ResultCode.RUN_NAME_CANNOT_BE_EMPTY);
        }
        if (runDO.getType() == null) {
            throw new XException(ResultCode.RUN_TYPE_MUST_DEFINE);
        }
        if (runDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        runDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //删除实验前首先删除所有关联的索引
        blockIndexService.remove(new BlockIndexQuery().setRunId(id));
    }

    @Override
    public List<RunDO> getAllByProjectId(String projectId) {
        return runDAO.getAll(new RunQuery().setProjectId(projectId));
    }

    @Override
    public List<RunIrt> getAllIrtByProjectId(String projectId) {
        return runDAO.getAll(new RunQuery().setProjectId(projectId), RunIrt.class);
    }

    @Override
    public void uploadAirdFile(RunDO runDO, TaskDO taskDO) {
        taskDO.addLog("Start Parsing Aird File:" + runDO.getName());
        taskService.update(taskDO);
        try {
            File indexFile = new File(runDO.getAirdIndexPath());
            File airdFile = new File(runDO.getAirdPath());
            String airdInfoJson = FileUtil.readFile(indexFile);
            AirdInfo airdInfo = null;
            try {
                airdInfo = JSONObject.parseObject(airdInfoJson, AirdInfo.class);
            } catch (Exception e) {
                taskDO.addLog("Aird Index File Format Error,Can not Convert from JSON String");
                taskDO.finish(TaskStatus.FAILED.getName());
                taskService.update(taskDO);
                return;
            }
            runDO.setAirdSize(airdFile.length());
            runDO.setAirdIndexSize(indexFile.length());
            runDO.setWindowRanges(airdInfo.getRangeList());
            runDO.setFeatures(airdInfo.getFeatures());
            runDO.setInstruments(airdInfo.getInstruments());
            runDO.setCompressors(airdInfo.getCompressors());
            runDO.setParentFiles(airdInfo.getParentFiles());
            runDO.setSoftwares(airdInfo.getSoftwares());
            runDO.setVendorFileSize(airdInfo.getFileSize());

            List<BlockIndexDO> blockIndexList = new ArrayList<>();
            for (BlockIndex blockIndex : airdInfo.getIndexList()) {
                BlockIndexDO blockIndexDO = new BlockIndexDO();
                BeanUtils.copyProperties(blockIndex, blockIndexDO);
                blockIndexDO.setRunId(runDO.getId());
                blockIndexDO.setRange(blockIndex.getWindowRange());
                blockIndexList.add(blockIndexDO);
            }
            blockIndexService.insert(blockIndexList);
            taskDO.addLog("Block Index Insert Success.索引存储成功");
            taskService.update(taskDO);

        } catch (IOException e) {
            e.printStackTrace();
            taskDO.addLog("Aird Parse Exception");
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }
    }

    @Override
    public FloatPairs getSpectrum(RunDO run, Double mz, Float rt) {
        BlockIndexDO blockIndex = blockIndexService.getOne(run.getId(), mz);
        if (blockIndex == null) {
            return null;
        }

        return getSpectrum(run, blockIndex, rt);

    }

    @Override
    public FloatPairs getSpectrum(RunDO run, BlockIndexDO blockIndex, Float rt) {
        Compressor mzCompressor = run.fetchCompressor(Compressor.TARGET_MZ);
        DIAParser parser = new DIAParser(run.getAirdPath(), mzCompressor, run.fetchCompressor(Compressor.TARGET_INTENSITY), mzCompressor.getPrecision());
        MzIntensityPairs pairs = parser.getSpectrumByRt(blockIndex.getStartPtr(), blockIndex.getRts(), blockIndex.getMzs(), blockIndex.getInts(), rt);
        parser.close();
        return new FloatPairs(pairs.getMzArray(), pairs.getIntensityArray());
    }

    @Override
    public Result remove(RunQuery query) {
        List<IdName> runList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        runList.forEach(idName -> {
            Result res = removeById(idName.id());
            if (res.isFailed()) {
                errorList.add("Remove Run Failed:" + res.getErrorMessage());
            }
        });
        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        }
        return Result.OK();
    }
}
