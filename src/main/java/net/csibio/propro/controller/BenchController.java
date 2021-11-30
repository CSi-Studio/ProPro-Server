package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.lfqbench.LfqBench;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(tags = {"LFQBench"})
@RestController
@RequestMapping("/api/bench")
public class BenchController {

  @Autowired ProjectService projectService;
  @Autowired LfqBench lfqBench;

  @GetMapping(value = "/peptideRatio")
  Result peptideRatio(@RequestParam("projectId") String projectId) {
    ProjectDO project = projectService.getById(projectId);
    if (project == null) {
      return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
    }

    return lfqBench.buildPeptideRatio(project);
  }
}
