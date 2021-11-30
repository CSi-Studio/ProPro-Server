package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.decoy.repeatCount.RepeatCount;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.run.BaseRun;
import net.csibio.propro.domain.bean.run.RunIrt;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.query.RunQuery;
import net.csibio.propro.domain.vo.RunVO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.RunService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.task.LibraryTask;
import net.csibio.propro.task.RunTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = {"Run Module"})
@RestController
@RequestMapping("/api/run/")
public class RunController {

  @Autowired RunService runService;
  @Autowired RunTask runTask;
  @Autowired LibraryService libraryService;
  @Autowired TaskService taskService;
  @Autowired LibraryTask libraryTask;
  @Autowired RepeatCount repeatCount;

  @GetMapping(value = "/listByProjectId")
  Result<List<IdName>> listByProjectId(@RequestParam("projectId") String projectId) {
    List<IdName> runList = runService.getAll(new RunQuery().setProjectId(projectId), IdName.class);
    return Result.OK(runList);
  }

  @GetMapping(value = "/list")
  Result<List<BaseRun>> list(RunQuery query) {
    if (StringUtils.isEmpty(query.getProjectId())) {
      return Result.Error(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
    }
    List<BaseRun> runList = runService.getAll(query, BaseRun.class);
    return Result.OK(runList);
  }

  @PostMapping(value = "/generateAlias")
  Result generateAlias(
      @RequestParam(value = "projectId") String projectId,
      @RequestParam(value = "prefix", defaultValue = "Run") String prefix,
      @RequestParam(value = "runIds", required = false) List<String> runIds) {
    List<RunDO> runList;
    if (runIds == null || runIds.size() == 0) {
      runList = runService.getAll(new RunQuery().setProjectId(projectId));
    } else {
      runList = runService.getAll(new RunQuery().setIds(runIds));
    }

    if (runList != null) {
      for (int i = 0; i < runList.size(); i++) {
        RunDO run = runList.get(i);
        run.setAlias(prefix + "-" + (i + 1) + "");
        runService.update(run);
      }
    }
    return Result.OK();
  }

  @PostMapping(value = "/edit")
  Result<RunDO> edit(
      @RequestParam("id") String id,
      @RequestParam(value = "alias", required = false) String alias,
      @RequestParam(value = "fragMode", required = false) String fragMode,
      @RequestParam(value = "group", required = false) String group,
      @RequestParam(value = "tags", required = false) List<String> tags) {
    RunDO run = runService.getById(id);
    if (run == null) {
      return Result.Error(ResultCode.RUN_NOT_EXISTED);
    }
    run.setAlias(alias);
    run.setGroup(group);
    run.setTags(tags);
    run.setFragMode(fragMode);
    runService.update(run);
    return Result.OK(run);
  }

  @PostMapping(value = "/batchEdit")
  Result<List<RunDO>> edit(
      @RequestParam("ids") List<String> ids,
      @RequestParam(value = "fragMode", required = false) String fragMode,
      @RequestParam(value = "group", required = false) String group,
      @RequestParam(value = "tags", required = false) List<String> tags) {
    if (ids == null || ids.isEmpty()) {
      return Result.Error(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
    }

    List<RunDO> runList = new ArrayList<>();
    for (String id : ids) {
      RunDO run = runService.getById(id);
      if (run == null) {
        return Result.Error(ResultCode.RUN_NOT_EXISTED);
      }
      if (StringUtils.isNotEmpty(group)) {
        run.setGroup(group);
      }
      if (tags != null && tags.size() != 0) {
        run.setTags(tags);
      }
      if (StringUtils.isNotEmpty(fragMode)) {
        run.setFragMode(fragMode);
      }
      runService.update(run);
      runList.add(run);
    }

    return Result.OK(runList);
  }

  @GetMapping(value = "/detail")
  Result<RunDO> detail(@RequestParam("id") String id) {
    RunDO run = runService.getById(id);
    if (run == null) {
      return Result.Error(ResultCode.RUN_NOT_EXISTED);
    }
    return Result.OK(run);
  }

  @GetMapping(value = "/getIrts")
  Result getIrts(@RequestParam("runList") List<String> runList) {
    if (runList.size() == 0) {
      return Result.Error(ResultCode.RUN_NOT_EXISTED);
    }
    RunVO runSample = runService.getOne(new RunQuery().setId(runList.get(0)), RunVO.class);
    List<RunIrt> irtAll = runService.getAllIrtByProjectId(runSample.getProjectId());
    List<RunIrt> filtered =
        irtAll.stream()
            .filter(runIrt -> runList.contains(runIrt.getId()))
            .collect(Collectors.toList());
    return Result.OK(filtered);
  }
}
