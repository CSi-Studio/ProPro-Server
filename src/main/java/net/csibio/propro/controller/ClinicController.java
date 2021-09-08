package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameAlias;
import net.csibio.propro.domain.bean.overview.OverviewV1;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.vo.ClinicPrepareDataVO;
import net.csibio.propro.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = {"Clinic Module"})
@RestController
@RequestMapping("clinic/")
public class ClinicController {

    @Autowired
    TaskService taskService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    ProjectService projectService;
    @Autowired
    MethodService methodService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "prepare")
    Result<ClinicPrepareDataVO> prepare(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        if (StringUtils.isEmpty(project.getInsLibId()) || StringUtils.isEmpty(project.getAnaLibId()) || StringUtils.isEmpty(project.getMethodId())) {
            return Result.Error(ResultCode.INS_ANA_METHOD_ID_CANNOT_BE_EMPTY_WHEN_USING_CLINIC);
        }
        LibraryDO anaLib = libraryService.getById(project.getAnaLibId());
        if (anaLib == null) {
            return Result.Error(ResultCode.ANA_LIBRARY_NOT_EXISTED);
        }
        LibraryDO insLib = libraryService.getById(project.getInsLibId());
        if (insLib == null) {
            return Result.Error(ResultCode.INS_LIBRARY_NOT_EXISTED);
        }
        MethodDO method = methodService.getById(project.getMethodId());
        if (method == null) {
            return Result.Error(ResultCode.METHOD_NOT_EXISTED);
        }
        List<IdNameAlias> expList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId), IdNameAlias.class);
        List<OverviewV1> totalOverviewList = overviewService.getAll(new OverviewQuery(projectId), OverviewV1.class);
        Map<String, List<OverviewV1>> overviewMap = totalOverviewList.stream().collect(Collectors.groupingBy(OverviewV1::expId));
        overviewMap.values().forEach(overviews -> {
            overviews = overviews.stream().sorted(Comparator.nullsLast(Comparator.comparing(OverviewV1::defaultOne))).toList();
        });

        ClinicPrepareDataVO data = new ClinicPrepareDataVO();
        data.setProject(project);
        data.setExpList(expList);
        data.setInsLib(new IdName(insLib.getId(), insLib.getName()));
        data.setAnaLib(new IdName(anaLib.getId(), anaLib.getName()));
        data.setMethod(method);
        data.setAnaProteins(anaLib.getProteins());
        data.setOverviewMap(overviewMap);
        return Result.OK(data);
    }
}
