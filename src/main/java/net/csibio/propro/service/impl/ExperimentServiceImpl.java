package net.csibio.propro.service.impl;

import com.alibaba.fastjson.JSONObject;
import net.csibio.aird.bean.AirdInfo;
import net.csibio.aird.bean.BlockIndex;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.ExperimentDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.experiment.ExpIrt;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.BlockIndexQuery;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.ExperimentService;
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

@Service("experimentService")
public class ExperimentServiceImpl implements ExperimentService {

    @Autowired
    ExperimentDAO experimentDAO;
    @Autowired
    TaskService taskService;
    @Autowired
    BlockIndexService blockIndexService;

    @Override
    public BaseDAO<ExperimentDO, ExperimentQuery> getBaseDAO() {
        return experimentDAO;
    }

    @Override
    public void beforeInsert(ExperimentDO experimentDO) throws XException {
        if (experimentDO.getName() == null) {
            throw new XException(ResultCode.EXPERIMENT_NAME_CANNOT_BE_EMPTY);
        }
        if (experimentDO.getType() == null) {
            throw new XException(ResultCode.EXPERIMENT_TYPE_MUST_DEFINE);
        }
        if (experimentDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        experimentDO.setCreateDate(new Date());
        experimentDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(ExperimentDO experimentDO) throws XException {
        if (experimentDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (experimentDO.getName() == null) {
            throw new XException(ResultCode.EXPERIMENT_NAME_CANNOT_BE_EMPTY);
        }
        if (experimentDO.getType() == null) {
            throw new XException(ResultCode.EXPERIMENT_TYPE_MUST_DEFINE);
        }
        if (experimentDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        experimentDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //删除实验前首先删除所有关联的索引
        blockIndexService.remove(new BlockIndexQuery().setExpId(id));
    }

    @Override
    public List<ExperimentDO> getAllByProjectId(String projectId) {
        return experimentDAO.getAll(new ExperimentQuery().setProjectId(projectId));
    }

    @Override
    public List<ExpIrt> getAllIrtByProjectId(String projectId) {
        return experimentDAO.getAll(new ExperimentQuery().setProjectId(projectId), ExpIrt.class);
    }

    @Override
    public void uploadAirdFile(ExperimentDO experimentDO, TaskDO taskDO) {
        taskDO.addLog("Start Parsing Aird File:" + experimentDO.getName());
        taskService.update(taskDO);
        try {
            File indexFile = new File(experimentDO.getAirdIndexPath());
            File airdFile = new File(experimentDO.getAirdPath());
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
            experimentDO.setAirdSize(airdFile.length());
            experimentDO.setAirdIndexSize(indexFile.length());
            experimentDO.setWindowRanges(airdInfo.getRangeList());
            experimentDO.setFeatures(airdInfo.getFeatures());
            experimentDO.setInstruments(airdInfo.getInstruments());
            experimentDO.setCompressors(airdInfo.getCompressors());
            experimentDO.setParentFiles(airdInfo.getParentFiles());
            experimentDO.setSoftwares(airdInfo.getSoftwares());
            experimentDO.setVendorFileSize(airdInfo.getFileSize());

            List<BlockIndexDO> blockIndexList = new ArrayList<>();
            for (BlockIndex blockIndex : airdInfo.getIndexList()) {
                BlockIndexDO blockIndexDO = new BlockIndexDO();
                BeanUtils.copyProperties(blockIndex, blockIndexDO);
                blockIndexDO.setExpId(experimentDO.getId());
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
    public Result remove(ExperimentQuery query) {
        List<IdName> expList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        expList.forEach(idName -> {
            Result res = removeById(idName.id());
            if (res.isFailed()) {
                errorList.add("Remove Experiment Failed:" + res.getErrorMessage());
            }
        });
        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        }
        return Result.OK();
    }
}
