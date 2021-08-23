package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = {"Overview Module"})
@RestController
@RequestMapping("overview/")
public class OverviewController {

    @Autowired
    OverviewService overviewService;
    @Autowired
    ProjectService projectService;

    @GetMapping(value = "/list")
    Result list(OverviewQuery query) {
        Result<List<OverviewDO>> result = overviewService.getList(query, OverviewDO.class);
        return result;
    }

    @GetMapping(value = "/detail")
    Result detail(@RequestParam("id") String id) {
        OverviewDO overviewDO = overviewService.getById(id);
        if (overviewDO == null) {
            return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
        }
        return Result.OK(overviewDO);
    }

    @PostMapping(value = "/update")
    Result<OverviewDO> update(
            @RequestParam("id") String id,
            @RequestParam("label") String label,
            @RequestParam("note") String note) {
        OverviewDO overview = overviewService.getById(id);
        if (overview == null) {
            return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
        }

        overview.setNote(note);
        overview.setLabel(label);
        return overviewService.update(overview);
    }

    @GetMapping(value = "/remove")
    Result<List<String>> remove(@RequestParam(value = "overviewIds") String overviewIds) {
        String[] overviewArray = overviewIds.split(SymbolConst.COMMA);
        Result<List<String>> result = new Result<List<String>>();
        List<String> errorList = new ArrayList<>();
        List<String> deletedIds = new ArrayList<>();
        for (String overviewId : overviewArray) {
            Result removeResult = overviewService.removeById(overviewId);
            if (removeResult.isSuccess()) {
                deletedIds.add(overviewId);
            } else {
                errorList.add("OverviewId:" + overviewId + "--" + removeResult.getErrorMessage());
            }
        }
        if (deletedIds.size() != 0) {
            result.setData(deletedIds);
            result.setSuccess(true);
        }
        if (errorList.size() != 0) {
            result.setErrorList(errorList);
        }
        return result;
    }
}
