package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.overview.OverviewV1;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.service.OverviewService;
import net.csibio.propro.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
            @RequestParam(value = "defaultOne", required = false) Boolean defaultOne,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            @RequestParam(value = "note", required = false) String note) {
        OverviewDO overview = overviewService.getById(id);
        if (overview == null) {
            return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
        }
        //如果将默认设置为true,那么会清理改exp下的所有overview状态为false
        if (defaultOne) {
            List<OverviewV1> v1List = overviewService.getAll(new OverviewQuery().setExpId(overview.getExpId()), OverviewV1.class);
            HashMap<String, Object> query = new HashMap<>();
            query.put("expId", overview.getExpId());

            HashMap<String, Object> field = new HashMap<>();
            field.put("defaultOne", false);
            overviewService.updateAll(query, field);
        }

        overview.setDefaultOne(defaultOne);
        overview.setNote(note);
        overview.setTags(tags);
        return overviewService.update(overview);
    }
    
    @PostMapping(value = "/batchUpdate")
    Result batchUpdate(
            @RequestParam("ids") List<String> ids,
            @RequestParam(value = "defaultOne", required = false) Boolean defaultOne,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            @RequestParam(value = "note", required = false) String note) {
        for (int i = 0; i < ids.size(); i++) {
            OverviewDO overview = overviewService.getById(ids.get(i));
            if (overview == null) {
                return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
            }
            if (defaultOne != null) {
                overview.setDefaultOne(defaultOne);
            }
            if (note != null) {
                overview.setNote(note);
            }
            if (tags != null) {
                overview.setTags(tags);
            }

            overviewService.update(overview);
        }
        return Result.OK();
    }

    @PostMapping(value = "/remove")
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
