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
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.MethodQuery;
import net.csibio.propro.domain.query.RunQuery;
import net.csibio.propro.domain.vo.PrepareAnalyzeVO;
import net.csibio.propro.service.*;
import net.csibio.propro.task.RunTask;
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
    RunService runService;
    @Autowired
    TaskService taskService;
    @Autowired
    RunTask runTask;
    @Autowired
    OverviewService overviewService;

    @GetMapping(value = "/scoreTypes")
    Result scoreTypes() {
        return Result.OK(ScoreType.getAllTypesName());
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
                   @RequestParam("runIdList") List<String> runIdList,
                   @RequestParam("methodId") String methodId,
                   @RequestParam("anaLibId") String anaLibId,
                   @RequestParam(value = "note", required = false) String note,
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
        List<RunDO> runList = runService.getAll(new RunQuery().setProjectId(projectId).setIds(runIdList));
        LibraryDO finalInsLib = insLib;

        if (onlyIrt) {
            TaskDO task = new TaskDO(TaskTemplate.IRT, "Analyze-IRT" + project.getName());
            taskService.insert(task);
            AnalyzeParams params = new AnalyzeParams(method);
            params.setAnaLibId(anaLib.getId());
            params.setAnaLibName(anaLib.getName());
            params.setInsLibId(finalInsLib.getId());
            params.setInsLibName(finalInsLib.getName());
            runTask.doIrt(task, runList, params);
        } else {
            for (RunDO run : runList) {
                TaskDO task = new TaskDO(TaskTemplate.EXTRACT_PEAKPICK_SCORE, "Analyze-EPPS-" + project.getName());
                taskService.insert(task);
                AnalyzeParams params = new AnalyzeParams(method);
                params.setAnaLibId(anaLib.getId());
                params.setAnaLibName(anaLib.getName());
                params.setInsLibId(finalInsLib.getId());
                params.setInsLibName(finalInsLib.getName());
                params.setNote(note);
                runTask.doCSi(task, run, params);
            }
        }
        return Result.OK();
    }

    @PostMapping(value = "/reselect")
    Result reselect(@RequestParam(value = "overviewIds") List<String> overviewIds) {
        for (String overviewId : overviewIds) {
            OverviewDO baseOverview = overviewService.getById(overviewId);
            if (baseOverview == null) {
                return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
            }
            ProjectDO project = projectService.getById(baseOverview.getProjectId());
            if (project == null) {
                return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
            }
            RunDO run = runService.getById(baseOverview.getRunId());
            if (run == null) {
                return Result.Error(ResultCode.RUN_NOT_EXISTED);
            }
            MethodDO method = methodService.getById(baseOverview.getParams().getMethod().getId());
            if (method == null) {
                return Result.Error(ResultCode.METHOD_NOT_EXISTED);
            }

            LibraryDO anaLib = libraryService.getById(baseOverview.getParams().getAnaLibId());
            if (anaLib == null) {
                return Result.Error(ResultCode.ANA_LIBRARY_NOT_EXISTED);
            }
            LibraryDO insLib = null;
            if (!method.getIrt().isUseAnaLibForIrt()) {
                insLib = libraryService.getById(baseOverview.getParams().getInsLibId());
                if (insLib == null) {
                    return Result.Error(ResultCode.INS_LIBRARY_NOT_EXISTED);
                }
            } else {
                insLib = anaLib;
            }

            TaskDO task = new TaskDO(TaskTemplate.RESELECT, "Analyze-Reselect-" + project.getName());
            taskService.insert(task);
            AnalyzeParams params = new AnalyzeParams(method);
            params.setBaseOverviewId(overviewId);
            params.setBaseOverview(baseOverview);
            params.setReselect(true);
            params.getMethod().getClassifier().setFdr(0.01);
            params.setAnaLibId(anaLib.getId());
            params.setAnaLibName(anaLib.getName());
            params.setInsLibId(insLib.getId());
            params.setInsLibName(insLib.getName());
            runTask.doProPro(task, run, params);
        }

        return Result.OK();
    }
}
