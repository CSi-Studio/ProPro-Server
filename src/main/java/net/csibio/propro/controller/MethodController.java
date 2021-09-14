package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.options.*;
import net.csibio.propro.domain.query.MethodQuery;
import net.csibio.propro.domain.vo.MethodUpdateVO;
import net.csibio.propro.service.MethodService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = {"Method Module"})
@RestController
@RequestMapping("method/")
public class MethodController {

    @Autowired
    MethodService methodService;

    @GetMapping(value = "/list")
    Result<List<MethodDO>> list(MethodQuery query) {
        Result<List<MethodDO>> result = methodService.getList(query);
        return result;
    }

    @PostMapping(value = "/add")
    Result add(MethodUpdateVO methodUpdateVO) {
        MethodDO method = new MethodDO();
        BeanUtils.copyProperties(methodUpdateVO, method);

        EicOptions eic = new EicOptions();
        IrtOptions irt = new IrtOptions();
        PeakFindingOptions peakFinding = new PeakFindingOptions();
        QuickFilterOptions quickFilter = new QuickFilterOptions();
        ScoreOptions score = new ScoreOptions();
        ClassifierOptions classifier = new ClassifierOptions();

        BeanUtils.copyProperties(methodUpdateVO, eic);
        BeanUtils.copyProperties(methodUpdateVO, irt);
        BeanUtils.copyProperties(methodUpdateVO, peakFinding);
        BeanUtils.copyProperties(methodUpdateVO, quickFilter);
        BeanUtils.copyProperties(methodUpdateVO, score);
        BeanUtils.copyProperties(methodUpdateVO, classifier);

        method.setEic(eic);
        method.setIrt(irt);
        method.setPeakFinding(peakFinding);
        method.setQuickFilter(quickFilter);
        method.setScore(score);
        method.setClassifier(classifier);

        Result result = methodService.insert(method);
        return result;
    }

    @PostMapping(value = "/update")
    Result update(MethodUpdateVO methodUpdateVO) {
        MethodDO existedMethod = methodService.getById(methodUpdateVO.getId());
        if (existedMethod == null) {
            return Result.Error(ResultCode.METHOD_NOT_EXISTED);
        }

        EicOptions eic = new EicOptions();
        IrtOptions irt = new IrtOptions();
        PeakFindingOptions peakFinding = new PeakFindingOptions();
        QuickFilterOptions quickFilter = new QuickFilterOptions();
        ScoreOptions score = new ScoreOptions();
        ClassifierOptions classifier = new ClassifierOptions();

        BeanUtils.copyProperties(methodUpdateVO, eic);
        BeanUtils.copyProperties(methodUpdateVO, irt);
        BeanUtils.copyProperties(methodUpdateVO, peakFinding);
        BeanUtils.copyProperties(methodUpdateVO, quickFilter);
        BeanUtils.copyProperties(methodUpdateVO, score);
        BeanUtils.copyProperties(methodUpdateVO, classifier);

        existedMethod.setName(methodUpdateVO.getName());
        existedMethod.setDescription(methodUpdateVO.getDescription());
        existedMethod.setEic(eic);
        existedMethod.setIrt(irt);
        existedMethod.setPeakFinding(peakFinding);
        existedMethod.setQuickFilter(quickFilter);
        existedMethod.setScore(score);
        existedMethod.setClassifier(classifier);
        return methodService.update(existedMethod);
    }

    @GetMapping(value = "/detail")
    Result<MethodDO> detail(@RequestParam("id") String id) {
        MethodDO method = methodService.getById(id);
        if (method == null) {
            return Result.Error(ResultCode.METHOD_NOT_EXISTED);
        }
        return Result.OK(method);
    }

    @GetMapping(value = "/remove")
    Result remove(@RequestParam(value = "methodIds") List<String> methodIds) {
        List<String> errorList = new ArrayList<>();
        if (methodIds != null && methodIds.size() > 0) {
            methodIds.forEach(methodId -> {
                Result res = methodService.removeById(methodId);
                if (res.isFailed()) {
                    errorList.add(res.getErrorMessage());
                }
            });
        }
        if (errorList.size() != 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        } else {
            return Result.OK();
        }
    }

}
