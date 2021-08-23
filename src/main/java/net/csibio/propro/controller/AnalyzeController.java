package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.constants.enums.LibraryType;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.MethodQuery;
import net.csibio.propro.domain.vo.PrepareAnalyzeVO;
import net.csibio.propro.service.*;
import net.csibio.propro.task.ExperimentTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = {"Analyze Module"})
@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    @Autowired
    ProjectService projectService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    MethodService methodService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    ExperimentTask experimentTask;

    @GetMapping(value = "/scoreTypes")
    Result scoreTypes() {
        return Result.OK(ScoreType.getUsedTypes());
    }

    @GetMapping(value = "/prepare")
    Result<PrepareAnalyzeVO> prepare(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }

        List<IdName> insLibList = libraryService.getAll(new LibraryQuery().setType(LibraryType.INS.getName()), IdName.class);
        List<IdName> anaLibList = libraryService.getAll(new LibraryQuery().setType(LibraryType.ANA.getName()), IdName.class);
        List<IdName> methodList = methodService.getAll(new MethodQuery(), IdName.class);
        PrepareAnalyzeVO analyzeVO = new PrepareAnalyzeVO();
        analyzeVO.setInsLibs(insLibList);
        analyzeVO.setAnaLibs(anaLibList);
        analyzeVO.setMethods(methodList);
        analyzeVO.setAnaLibId(project.getAnaLibId());
        analyzeVO.setInsLibId(project.getInsLibId());
        analyzeVO.setMethodId(project.getMethodId());
        analyzeVO.setProjectName(project.getName());
        return Result.OK(analyzeVO);
    }

    @PostMapping(value = "/analyze")
    Result analyze(@RequestParam(value = "projectId") String projectId,
                   @RequestParam(value = "onlyIrt", defaultValue = "false") Boolean onlyIrt,
                   @RequestParam("expIdList") List<String> expIdList,
                   @RequestParam("methodId") String methodId,
                   @RequestParam("anaLibId") String anaLibId,
                   @RequestParam(value = "insLibId", required = false) String insLibId
    ) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        MethodDO method = methodService.getById(methodId);
        if (method == null) {
            return Result.Error(ResultCode.METHOD_NOT_EXISTED);
        }
        LibraryDO anaLib = libraryService.getById(anaLibId);
        if (anaLib == null) {
            return Result.Error(ResultCode.ANA_LIBRARY_NOT_EXISTED);
        }
        LibraryDO insLib = null;
        if (!method.getIrt().isUseAnaLibForIrt()) {
            insLib = libraryService.getById(insLibId);
            if (insLib == null) {
                return Result.Error(ResultCode.INS_LIBRARY_NOT_EXISTED);
            }
        } else {
            insLib = anaLib;
        }
        List<ExperimentDO> experimentList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId).setIds(expIdList));
        LibraryDO finalInsLib = insLib;

        if (onlyIrt) {
            TaskDO task = new TaskDO(TaskTemplate.IRT, "Analyze-");
            taskService.insert(task);
            AnalyzeParams params = new AnalyzeParams(method);
            params.setAnaLibId(anaLib.getId());
            params.setAnaLibName(anaLib.getName());
            params.setInsLibId(finalInsLib.getId());
            params.setInsLibName(finalInsLib.getName());
            experimentTask.doIrt(task, experimentList, params);
        } else {
            experimentList.forEach(exp -> {
                TaskDO task = new TaskDO(TaskTemplate.EXTRACT_PEAKPICK_SCORE, "Analyze-EPPS-");
                taskService.insert(task);
                AnalyzeParams params = new AnalyzeParams(method);
                params.setAnaLibId(anaLib.getId());
                params.setAnaLibName(anaLib.getName());
                params.setInsLibId(finalInsLib.getId());
                params.setInsLibName(finalInsLib.getName());
                experimentTask.doProPro(task, exp, params);
            });
        }
        return Result.OK();
    }
}
