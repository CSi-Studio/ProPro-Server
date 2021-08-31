package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.experiment.BaseExp;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"Data Module"})
@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    TaskService taskService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "/list")
    Result list(@RequestParam("projectId") String projectId,
                @RequestParam("expIds") List<String> expIds,
                @RequestParam("protein") String protein,
                @RequestParam("peptide") String peptide
    ) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        List<BaseExp> expList = experimentService.getAll(new ExperimentQuery().setIds(expIds), BaseExp.class);
        if (expList.size() != expIds.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_NOT_EXISTED);
        }
        List<OverviewDO> overviewList = overviewService.getDefaultOverviewList(projectId, expIds);
        if (overviewList.size() != expIds.size()) {
            return Result.Error(ResultCode.SOME_EXPS_HAVE_NO_DEFAULT_OVERVIEW);
        }

        DataQuery query = new DataQuery();

        Result<List<DataDO>> result = dataService.getList(query, query.getProjectId());
        return result;
    }


}
