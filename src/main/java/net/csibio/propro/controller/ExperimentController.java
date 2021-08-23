package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.decoy.repeatCount.RepeatCount;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(value = "/list")
    Result<List<BaseExp>> list(ExperimentQuery query) {
        if (StringUtils.isEmpty(query.getProjectId())) {
            return Result.Error(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        List<BaseExp> expList = experimentService.getAll(query, BaseExp.class);
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
