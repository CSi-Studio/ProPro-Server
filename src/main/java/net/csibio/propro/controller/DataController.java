package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    Result list(DataSumQuery dataQuery) {
        if (dataQuery.getOverviewId() == null) {
            return Result.Error(ResultCode.OVERVIEW_ID_CAN_NOT_BE_EMPTY);
        }
        OverviewDO overview = overviewService.getById(dataQuery.getOverviewId());
        dataQuery.setSortColumn("status").setOrderBy(Sort.Direction.ASC);
        Result<List<DataSumDO>> res = dataSumService.getList(dataQuery, overview.getProjectId());
        if (res.isFailed()) {
            return res;
        }

        List<DataSumDO> dataSumList = res.getData();

        List<ExpDataVO> dataList = new ArrayList<>();
        dataSumList.forEach(dataSum -> {
            BaseData baseData = dataService.getById(dataSum.getId(), BaseData.class, overview.getProjectId());
            ExpDataVO dataVO = new ExpDataVO(overview.getExpId());
            dataVO.merge(baseData, dataSum);
            dataList.add(dataVO);
        });
        Result<List<ExpDataVO>> result = new Result<>(true);
        result.setPagination(res.getPagination());
        result.setData(dataList);
        return result;
    }
}
