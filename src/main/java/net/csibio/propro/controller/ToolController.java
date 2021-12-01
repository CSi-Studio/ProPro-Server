package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.formula.FormulaCalculator;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Api(tags = {"Tool Module"})
@RestController
@RequestMapping("/api/tool")
@Slf4j
public class ToolController {

    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    FormulaCalculator formulaCalculator;

    @PostMapping(value = "/peptideCalc")
    Result peptideCalc(
            @RequestParam(value = "sequence") String sequence,
            @RequestParam(value = "type") String type,
            @RequestParam(value = "adjust", required = false, defaultValue = "0") int adjust,
            @RequestParam(value = "deviation", required = false, defaultValue = "0") double deviation,
            @RequestParam(value = "unimodIds", required = false) String unimodIds,
            @RequestParam(value = "charge", required = false, defaultValue = "1") int charge) {
        List<String> unimodList = null;
        if (unimodIds != null && !unimodIds.isEmpty()) {
            String[] unimodIdArray = unimodIds.split(",");
            unimodList = Arrays.asList(unimodIdArray);
        }

        // 默认偏差为0
        double monoMz =
                formulaCalculator.getMonoMz(sequence, type, charge, adjust, deviation, false, unimodList);
        double averageMz =
                formulaCalculator.getAverageMz(
                        sequence, type, charge, adjust, deviation, false, unimodList);

        HashMap<String, Object> output = new HashMap<>();
        output.put("monoMz", monoMz);
        output.put("averageMz", averageMz);

        return Result.OK(output);
    }
}
