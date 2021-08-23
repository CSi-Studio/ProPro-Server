package net.csibio.propro.service.impl;

import net.csibio.propro.constants.constant.SuffixConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.ProjectDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.query.ProjectQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.ExperimentService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.ProjectService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.task.ExperimentTask;
import net.csibio.propro.utils.RepositoryUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service("projectService")
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    ProjectDAO projectDAO;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    ExperimentTask experimentTask;
    @Autowired
    OverviewService overviewService;


    @Override
    public BaseDAO<ProjectDO, ProjectQuery> getBaseDAO() {
        return projectDAO;
    }

    @Override
    public void beforeInsert(ProjectDO projectDO) throws XException {
        if (projectDO.getName() == null) {
            throw new XException(ResultCode.PROJECT_NAME_CANNOT_BE_EMPTY);
        }
        projectDO.setCreateDate(new Date());
        projectDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(ProjectDO projectDO) throws XException {
        if (projectDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (projectDO.getName() == null) {
            throw new XException(ResultCode.PROJECT_NAME_CANNOT_BE_EMPTY);
        }
        projectDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //Step1. 删除项目下所有的鉴定结果
        Result result1 = overviewService.remove(new OverviewQuery().setProjectId(id));
        //其次删除所有的实验信息,这里使用依次删除每一个实验的方法,是需要保证在删除每一个实验的时候,每一个实验首先将其下的索引数据删除完毕
        Result result2 = experimentService.remove(new ExperimentQuery().setProjectId(id));
        if (result1.isFailed() || result2.isFailed()) {
            result1.getErrorList().addAll(result2.getErrorList());
            throw new XException(ResultCode.DELETE_ERROR, result1.getErrorList());
        }
    }

    @Override
    public List<String> listUnloadProjects() {
        File directory = new File(RepositoryUtil.getProjectRoot());
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        Set<String> namesSet = new HashSet<>();
        for (File file : files) {
            if (file.isDirectory()) {
                namesSet.add(file.getName());
            }
        }

        List<IdName> idNameList = getAll(new ProjectQuery(), IdName.class);
        Set<String> dbNamesSet = idNameList.stream().map(IdName::name).collect(Collectors.toSet());
        List<String> unloadsNames = new ArrayList<>();
        for (String fileName : namesSet) {
            if (!dbNamesSet.contains(fileName)) {
                unloadsNames.add(fileName);
            }
        }
        return unloadsNames;
    }

    public List<File> scanExpFilesByProjectName(String projectName) {
        File directory = new File(RepositoryUtil.getProjectRepo(projectName));
        List<File> newFileList = new ArrayList<>();
        File[] fileArray = directory.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isFile()) {
                    newFileList.add(file);
                }
            }
        }
        return newFileList;
    }

    @Override
    public Result<TaskDO> scan(ProjectDO project) {
        List<File> fileList = scanExpFilesByProjectName(project.getName());
        List<File> newFileList = new ArrayList<>();
        List<ExperimentDO> exps = experimentService.getAllByProjectId(project.getId());
        List<String> existedExpNames = new ArrayList<>();
        for (ExperimentDO exp : exps) {
            existedExpNames.add(exp.getName());
        }
        //过滤文件
        for (File file : fileList) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(SuffixConst.JSON) && !existedExpNames.contains(FilenameUtils.getBaseName(file.getName()))) {
                newFileList.add(file);
            }
        }
        if (newFileList.isEmpty()) {
            return Result.Error(ResultCode.NO_NEW_EXPERIMENTS.getMessage());
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.SCAN_AND_UPDATE_EXPERIMENTS, project.getName());
        taskDO.addLog(newFileList.size() + " total");
        taskService.insert(taskDO);
        List<ExperimentDO> expsToUpdate = new ArrayList<>();
        for (File file : newFileList) {
            ExperimentDO exp = new ExperimentDO();
            exp.setName(FilenameUtils.getBaseName(file.getName()));
            exp.setProjectId(project.getId());
            exp.setProjectName(project.getName());
            exp.setType(project.getType());
            Result result = experimentService.insert(exp);
            if (result.isFailed()) {
                taskDO.addLog("ERROR-" + exp.getId() + "-" + exp.getName());
                taskDO.addLog(result.getErrorMessage());
                taskService.update(taskDO);
            }
            expsToUpdate.add(exp);
        }

        experimentTask.uploadAird(expsToUpdate, taskDO);
        return Result.OK(taskDO);
    }

    @Override
    public void scanAll() {
        //TODO 李然 注意查看ProjectSevice中关于本函数的说明
    }

    @Override
    public Result remove(ProjectQuery query) {
        List<IdName> idNameList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < idNameList.size(); i++) {
            Result res = removeById(idNameList.get(i).id());
            if (res.isFailed()) {
                errorList.add(res.getErrorMessage());
            }
        }

        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        } else {
            return Result.OK();
        }
    }
}
