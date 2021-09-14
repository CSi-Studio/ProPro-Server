package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.excel.peptide.PeptideExcelBuilder;
import net.csibio.propro.excel.peptide.PeptideRow;
import net.csibio.propro.service.ExperimentService;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = {"Excel Module"})
@RestController
@RequestMapping("excel/")
public class ExcelController {

    @Autowired
    OverviewService overviewService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;

    @PostMapping(value = "report")
    Result report(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }

        List<IdName> expIdNameList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId), IdName.class);
        List<String> expIds = expIdNameList.stream().map(IdName::id).collect(Collectors.toList());
        List<String> expNames = expIdNameList.stream().map(IdName::name).collect(Collectors.toList());
        Result<List<PeptideRow>> result = overviewService.report(expIds);
        if (result.isFailed()) {
            return result;
        }

        PeptideExcelBuilder builder = new PeptideExcelBuilder(project.getName(), expNames, result.getData());
        builder.export();
        log.info("导出成功");
        return Result.OK();
    }
}
