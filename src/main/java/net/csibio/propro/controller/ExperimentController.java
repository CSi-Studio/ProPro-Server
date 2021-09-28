package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.decoy.repeatCount.RepeatCount;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.experiment.BaseExp;
import net.csibio.propro.domain.bean.experiment.ExpIrt;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.domain.vo.ExpVO;
import net.csibio.propro.service.ExperimentService;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.task.ExperimentTask;
import net.csibio.propro.task.LibraryTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = {"Experiment Module"})
@RestController
@RequestMapping("experiment/")
public class ExperimentController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ExperimentTask experimentTask;
    @Autowired
    LibraryService libraryService;
    @Autowired
    TaskService taskService;
    @Autowired
    LibraryTask libraryTask;
    @Autowired
    RepeatCount repeatCount;

    @GetMapping(value = "/listByProjectId")
    Result<List<IdName>> listByProjectId(@RequestParam("projectId") String projectId) {
        List<IdName> expList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId), IdName.class);
        return Result.OK(expList);
    }

    @GetMapping(value = "/list")
    Result<List<BaseExp>> list(ExperimentQuery query) {
        if (StringUtils.isEmpty(query.getProjectId())) {
            return Result.Error(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        List<BaseExp> expList = experimentService.getAll(query, BaseExp.class);
        return Result.OK(expList);
    }

    @PostMapping(value = "/generateAlias")
    Result generateAlias(@RequestParam(value = "projectId") String projectId,
                         @RequestParam(value = "prefix", defaultValue = "Exp") String prefix,
                         @RequestParam(value = "expIds", required = false) List<String> expIds) {
        List<ExperimentDO> expList;
        if (expIds == null || expIds.size() == 0) {
            expList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId));
        } else {
            expList = experimentService.getAll(new ExperimentQuery().setIds(expIds));
        }

        if (expList != null) {
            for (int i = 0; i < expList.size(); i++) {
                ExperimentDO exp = expList.get(i);
                exp.setAlias(prefix + "-" + (i + 1) + "");
                experimentService.update(exp);
            }
        }
        return Result.OK();
    }

    @PostMapping(value = "/edit")
    Result<ExperimentDO> edit(@RequestParam("id") String id,
                              @RequestParam(value = "alias", required = false) String alias,
                              @RequestParam(value = "fragMode", required = false) String fragMode,
                              @RequestParam(value = "label", required = false) String label,
                              @RequestParam(value = "tags", required = false) List<String> tags) {
        ExperimentDO exp = experimentService.getById(id);
        if (exp == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        exp.setAlias(alias);
        exp.setLabel(label);
        exp.setTags(tags);
        exp.setFragMode(fragMode);
        experimentService.update(exp);
        return Result.OK(exp);
    }

    @PostMapping(value = "/batchEdit")
    Result<List<ExperimentDO>> edit(@RequestParam("ids") List<String> ids,
                                    @RequestParam(value = "fragMode", required = false) String fragMode,
                                    @RequestParam(value = "label", required = false) String label,
                                    @RequestParam(value = "tags", required = false) List<String> tags) {
        if (ids == null || ids.isEmpty()) {
            return Result.Error(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        List<ExperimentDO> expList = new ArrayList<>();
        for (String id : ids) {
            ExperimentDO exp = experimentService.getById(id);
            if (exp == null) {
                return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
            }
            if (StringUtils.isNotEmpty(label)) {
                exp.setLabel(label);
            }
            if (tags != null && tags.size() != 0) {
                exp.setTags(tags);
            }
            if (StringUtils.isNotEmpty(fragMode)) {
                exp.setFragMode(fragMode);
            }
            experimentService.update(exp);
            expList.add(exp);
        }

        return Result.OK(expList);
    }

    @GetMapping(value = "/detail")
    Result<ExperimentDO> detail(@RequestParam("id") String id) {
        ExperimentDO exp = experimentService.getById(id);
        if (exp == null) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        return Result.OK(exp);
    }

    @GetMapping(value = "/getIrts")
    Result getIrts(@RequestParam("expList") List<String> expList) {
        if (expList.size() == 0) {
            return Result.Error(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        ExpVO expSample = experimentService.getOne(new ExperimentQuery().setId(expList.get(0)), ExpVO.class);
        List<ExpIrt> irtAll = experimentService.getAllIrtByProjectId(expSample.getProjectId());
        List<ExpIrt> filtered = irtAll.stream().filter(expIrt -> expList.contains(expIrt.getId())).collect(Collectors.toList());
        return Result.OK(filtered);
    }
}
