package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.service.ExperimentService;
import net.csibio.propro.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Api(tags = {"Clinic Module"})
@RestController
@RequestMapping("clinic/")
public class ClinicController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ProjectService projectService;

    @GetMapping(value = "/prepare")
    Result prepare(@RequestParam("expList") List<String> expList,
                   @RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);

        return Result.OK();
    }
}
