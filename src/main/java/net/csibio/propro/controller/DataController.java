package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.DataQuery;
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
    DataService dataService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "/list")
    Result list(@RequestParam("projectId") String projectId,
                @RequestParam("expIds") String expIds,
                @RequestParam("protein") String protein,
                @RequestParam("peptide") String peptide
    ) {
        ProjectDO project = projectService.getById(projectId);
        
        DataQuery query = new DataQuery();

        Result<List<DataDO>> result = dataService.getList(query, query.getProjectId());
        return result;
    }


}
