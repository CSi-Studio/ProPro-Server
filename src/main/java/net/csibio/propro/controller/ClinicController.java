package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.peak.GaussFilter;
import net.csibio.propro.algorithm.peak.SignalToNoiseEstimator;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameAlias;
import net.csibio.propro.domain.bean.overview.Overview4Clinic;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.options.SigmaSpacing;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.domain.vo.ClinicPrepareDataVO;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = {"Clinic Module"})
@RestController
@RequestMapping("clinic/")
public class ClinicController {

    @Autowired
    TaskService taskService;
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
    DataService dataService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    
    @GetMapping(value = "prepare")
    Result<ClinicPrepareDataVO> prepare(@RequestParam("projectId") String projectId) {
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            return Result.Error(ResultCode.PROJECT_NOT_EXISTED);
        }
        if (StringUtils.isEmpty(project.getInsLibId()) || StringUtils.isEmpty(project.getAnaLibId()) || StringUtils.isEmpty(project.getMethodId())) {
            return Result.Error(ResultCode.INS_ANA_METHOD_ID_CANNOT_BE_EMPTY_WHEN_USING_CLINIC);
        }
        LibraryDO anaLib = libraryService.getById(project.getAnaLibId());
        if (anaLib == null) {
            return Result.Error(ResultCode.ANA_LIBRARY_NOT_EXISTED);
        }
        LibraryDO insLib = libraryService.getById(project.getInsLibId());
        if (insLib == null) {
            return Result.Error(ResultCode.INS_LIBRARY_NOT_EXISTED);
        }
        MethodDO method = methodService.getById(project.getMethodId());
        if (method == null) {
            return Result.Error(ResultCode.METHOD_NOT_EXISTED);
        }
        List<IdNameAlias> expList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId), IdNameAlias.class);
        List<Overview4Clinic> totalOverviewList = overviewService.getAll(new OverviewQuery(projectId), Overview4Clinic.class);
        Map<String, List<Overview4Clinic>> overviewMap = totalOverviewList.stream().collect(Collectors.groupingBy(Overview4Clinic::expId));
        overviewMap.values().forEach(overviews -> {
            overviews = overviews.stream().sorted(Comparator.nullsLast(Comparator.comparing(Overview4Clinic::defaultOne))).toList();
        });

        ClinicPrepareDataVO data = new ClinicPrepareDataVO();
        data.setProject(project);
        if (expList.stream().filter(idNameAlias -> idNameAlias.alias() != null).count() == expList.size()) {
            data.setExpList(expList.stream().sorted(Comparator.comparing(IdNameAlias::alias)).collect(Collectors.toList()));
        } else {
            data.setExpList(expList.stream().sorted(Comparator.comparing(IdNameAlias::name)).collect(Collectors.toList()));
        }
        data.setInsLib(new IdName(insLib.getId(), insLib.getName()));
        data.setAnaLib(new IdName(anaLib.getId(), anaLib.getName()));
        data.setMethod(method);
        data.setProteins(anaLib.getProteins());
        data.setOverviewMap(overviewMap);
        return Result.OK(data);
    }

    /**
     * Core API
     * 1. If the EIC data exist. Get the data directly from the database
     * 2. Else predict the Y-Ion for the target peptide and analyze the EIC data from the Aird file
     *
     * @param projectId
     * @param libraryId
     * @param peptideRef
     * @param predict
     * @param onlyDefault
     * @param smooth
     * @param denoise
     * @param expIds
     * @return
     */
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
        for (int i = 0; i < expIds.size(); i++) {
            String expId = expIds.get(i);
            OverviewQuery query = new OverviewQuery(projectId).setExpId(expId);
            if (onlyDefault) {
                query.setDefaultOne(true);
            }
            Overview4Clinic overview = overviewService.getOne(query, Overview4Clinic.class);
            if (overview == null) {
                continue;
            }
            ExpDataVO data = null;
            //如果使用预测方法,则进行实时EIC获取
            if (predict) {
                ExperimentDO exp = experimentService.getById(expId);
                data = dataService.buildData(exp, libraryId, peptideRef);
                data.setExpId(exp.getId());
            } else {
                data = dataService.getData(projectId, expId, overview.id(), peptideRef);
            }
            dataList.add(data);
        }

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
        return Result.OK(dataList);
    }
}
