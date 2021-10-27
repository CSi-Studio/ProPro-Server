package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.LibraryType;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameType;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.*;
import net.csibio.propro.domain.vo.ProjectBeforeAddVO;
import net.csibio.propro.domain.vo.ProjectUpdateVO;
import net.csibio.propro.domain.vo.ProjectVO;
import net.csibio.propro.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Api(tags = {"Project Module"})
@RestController
@RequestMapping("/project")
@Slf4j
public class ProjectController {

    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    MethodService methodService;
    @Autowired
    OverviewService overviewService;

    @GetMapping(value = "/list")
    Result list(ProjectQuery query) {
        long s = System.currentTimeMillis();
        query.setSortColumn("group");
        query.setOrderBy(Sort.Direction.ASC);
        Result<List<ProjectVO>> result = projectService.getList(query, ProjectVO.class);
        if (result.isSuccess()) {
            result.getData().forEach(projectVO -> {
                if (projectVO.getAnaLibId() != null) {
                    LibraryDO anaLib = libraryService.getById(projectVO.getAnaLibId());
                    if (anaLib != null) {
                        projectVO.setAnaLibName(anaLib.getName());
                    }
                }

                if (projectVO.getInsLibId() != null) {
                    LibraryDO insLib = libraryService.getById(projectVO.getInsLibId());
                    if (insLib != null) {
                        projectVO.setInsLibName(insLib.getName());
                    }
                }

                if (projectVO.getMethodId() != null) {
                    MethodDO method = methodService.getById(projectVO.getMethodId());
                    if (method != null) {
                        projectVO.setMethodName(method.getName());
                    }
                }

                projectVO.setExpCount(experimentService.count(new ExperimentQuery().setProjectId(projectVO.getId())));
                projectVO.setOverviewCount(overviewService.count(new OverviewQuery().setProjectId(projectVO.getId())));
            });
        }
        log.info("耗时:" + (System.currentTimeMillis() - s));
        return result;
    }

    @GetMapping(value = "/beforeAdd")
    Result<ProjectBeforeAddVO> beforeAdd() {
        List<String> unloadsProjectList = projectService.listUnloadProjects();
        List<IdNameType> allLibs = libraryService.getAll(new LibraryQuery(), IdNameType.class);
        ProjectBeforeAddVO vo = new ProjectBeforeAddVO();
        vo.setUnloads(unloadsProjectList);
        vo.setInsLibs(allLibs.stream().filter(idNameType -> idNameType.type() != null && idNameType.type().equals(LibraryType.INS.getName())).collect(Collectors.toList()));
        vo.setAnaLibs(allLibs.stream().filter(idNameType -> idNameType.type() != null && idNameType.type().equals(LibraryType.ANA.getName())).collect(Collectors.toList()));
        vo.setMethods(methodService.getAll(new MethodQuery(), IdName.class));
        return Result.OK(vo);
    }

    @PostMapping(value = "/add")
    Result add(ProjectUpdateVO projectUpdateVO) {
        ProjectDO project = new ProjectDO();
        BeanUtils.copyProperties(projectUpdateVO, project);
        Result result = projectService.insert(project);
        if (result.isFailed()) {
            return result;
        }
        return projectService.scan(project);
    }

    @PostMapping(value = "/scan")
    Result scan(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        return projectService.scan(project);
    }

    @PostMapping(value = "/update")
    Result update(ProjectUpdateVO projectUpdateVO) {
        ProjectDO project = projectService.getById(projectUpdateVO.getId());
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        project.setDescription(projectUpdateVO.getDescription());
        project.setGroup(projectUpdateVO.getGroup());
        project.setOwner(projectUpdateVO.getOwner());
        project.setType(projectUpdateVO.getType());
        project.setAnaLibId(projectUpdateVO.getAnaLibId());
        project.setInsLibId(projectUpdateVO.getInsLibId());
        project.setMethodId(projectUpdateVO.getMethodId());
        project.setAlias(projectUpdateVO.getAlias());
        project.setTags(projectUpdateVO.getTags());
        return projectService.update(project);
    }

    /**
     * 删除项目下每一个实验的Irt结果
     *
     * @param projectId
     * @return
     */
    @GetMapping(value = "/removeIrt")
    Result removeIrt(@RequestParam("projectId") String projectId) {
        List<ExperimentDO> expList = experimentService.getAllByProjectId(projectId);
        if (expList == null) {
            return Result.Error(ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
        }
        for (ExperimentDO experimentDO : expList) {
            experimentDO.setIrt(null);
            experimentService.update(experimentDO);
        }
        return Result.OK();
    }

    @GetMapping(value = "/removeAnalyse")
    Result removeAnalyse(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        List<IdName> idNameList = overviewService.getAll(new OverviewQuery().setProjectId(project.getId()), IdName.class);
        for (IdName idName : idNameList) {
            overviewService.removeById(idName.id());
        }
        return Result.OK();
    }

    @GetMapping(value = "/remove")
    Result remove(@RequestParam("projectId") String projectId) {
        return projectService.removeById(projectId);
    }

    @GetMapping(value = "/peptideRatio")
    Result peptideRatio(@RequestParam("projectId") String projectId) {
        return projectService.removeById(projectId);
    }
}
