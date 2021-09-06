package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.bean.overview.OverviewV1;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Api(tags = {"Data Module"})
@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    ProjectService projectService;
    @Autowired
    MethodService methodService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "/list")
    Result list(DataQuery dataQuery) {
        if (dataQuery.getOverviewId() == null) {
            return Result.Error(ResultCode.OVERVIEW_ID_CAN_NOT_BE_EMPTY);
        }
        OverviewDO overview = overviewService.getById(dataQuery.getOverviewId());
        Result<List<BaseData>> result = dataService.getList(dataQuery, BaseData.class, overview.getProjectId());
        if (result.isFailed()) {
            return result;
        }

        List<BaseData> baseDataList = result.getData();

        List<ExpDataVO> dataList = new ArrayList<>();
        baseDataList.forEach(baseData -> {
            DataSumDO dataSum = dataSumService.getById(baseData.getId(), overview.getProjectId());
            ExpDataVO dataVO = new ExpDataVO(overview.getExpId());
            dataVO.merge(baseData, dataSum);
            dataList.add(dataVO);
        });
        return Result.OK(dataList);
    }

    @GetMapping(value = "/getExpData")
    Result getExpData(@RequestParam("projectId") String projectId,
                      @RequestParam("peptideRef") String peptideRef,
                      @RequestParam("onlyDefault") Boolean onlyDefault,
                      @RequestParam("expIds") List<String> expIds) {
        if (expIds == null || expIds.size() == 0) {
            return Result.Error(ResultCode.EXP_IDS_CANNOT_BE_EMPTY);
        }
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        long start = System.currentTimeMillis();
        List<ExpDataVO> dataList = new ArrayList<>();
        expIds.forEach(expId -> {
            OverviewQuery query = new OverviewQuery(projectId).setExpId(expId);
            if (onlyDefault) {
                query.setDefaultOne(true);
            }
            OverviewV1 overview = overviewService.getOne(query, OverviewV1.class);
            ExpDataVO data = dataService.getData(projectId, expId, overview.id(), peptideRef);
            dataList.add(data);
        });
        log.info("获取数据完毕,耗时:" + (System.currentTimeMillis() - start));
        return Result.OK(dataList);
    }
}
