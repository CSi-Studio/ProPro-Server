package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.peak.GaussFilter;
import net.csibio.propro.algorithm.peak.SignalToNoiseEstimator;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.bean.overview.OverviewV1;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
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
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;

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

    @PostMapping(value = "/getExpData")
    Result getExpData(@RequestParam("projectId") String projectId,
                      @RequestParam(value = "libraryId", required = false) String libraryId,
                      @RequestParam("peptideRef") String peptideRef,
                      @RequestParam("predict") Boolean predict,
                      @RequestParam("onlyDefault") Boolean onlyDefault,
                      @RequestParam(value = "smooth", required = false) Boolean smooth,
                      @RequestParam(value = "denoise", required = false) Boolean denoise,
                      @RequestParam("expIds") List<String> expIds) {
        List<ExpDataVO> dataList = new ArrayList<>();
        expIds.forEach(expId -> {
            OverviewQuery query = new OverviewQuery(projectId).setExpId(expId);
            if (onlyDefault) {
                query.setDefaultOne(true);
            }
            OverviewV1 overview = overviewService.getOne(query, OverviewV1.class);
            ExpDataVO data = null;
            //如果使用预测方法,则进行实时EIC获取
            if (predict) {
                ExperimentDO exp = experimentService.getById(expId);
                data = dataService.buildData(exp, libraryId, peptideRef);
            } else {
                data = dataService.getData(projectId, expId, overview.id(), peptideRef);
            }
            dataList.add(data);
        });
        if (smooth) {
            dataList.forEach(data -> {
                SigmaSpacing ss = SigmaSpacing.create();
                HashMap<String, float[]> smoothInt = GaussFilter.filter(data.getRtArray(), (HashMap<String, float[]>) data.getIntMap(), ss);
                data.setIntMap(smoothInt);
            });
        }
        if (denoise) {
            dataList.forEach(data -> {
                HashMap<String, float[]> denoiseIntMap = new HashMap<>();
                float[] rt = data.getRtArray();
                for (String cutInfo : data.getIntMap().keySet()) {
                    double[] noises200 = signalToNoiseEstimator.computeSTN(rt, data.getIntMap().get(cutInfo), 200, 30);
                    float[] denoiseInt = new float[noises200.length];
                    for (int i = 0; i < noises200.length; i++) {
                        denoiseInt[i] = (float) (data.getIntMap().get(cutInfo)[i] * noises200[i] / (noises200[i] + 1));
                    }
                    denoiseIntMap.put(cutInfo, denoiseInt);
                }
                data.setIntMap(denoiseIntMap);
            });
        }
        if (predict) {
            log.info("EIC实时获取完毕");
        }
        return Result.OK(dataList);
    }
}
