package net.csibio.propro.algorithm.lfqbench;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.experiment.BaseExp;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.domain.query.ExperimentQuery;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component("bench")
public class Bench {

    public static final String LABEL_A = "A";
    public static final String LABEL_B = "B";
    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    DataService dataService;
    @Autowired
    OverviewService overviewService;

    Result buildPeptideRatio(ProjectDO project) {
        List<BaseExp> expList = experimentService.getAll(new ExperimentQuery().setProjectId(project.getId()), BaseExp.class);
        List<BaseExp> expAList = expList.stream().filter(exp -> exp.getLabel().equals(LABEL_A)).collect(Collectors.toList());
        List<BaseExp> expBList = expList.stream().filter(exp -> exp.getLabel().equals(LABEL_B)).collect(Collectors.toList());
        Map<String, OverviewDO> overviewMap = overviewService.getDefaultOverviews(expList.stream().map(BaseExp::getId).collect(Collectors.toList()));
        if (overviewMap.size() != expList.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_HAVE_NO_DEFAULT_OVERVIEW);
        }
        Map<String, DataSumDO> dataMapForA = mergeData(project, expAList, overviewMap);
        Map<String, DataSumDO> dataMapForB = mergeData(project, expBList, overviewMap);
        return null;
    }

    List buildProteinRatio(ProjectDO project) {
        return null;
    }

    private Map<String, DataSumDO> mergeData(ProjectDO project, List<BaseExp> expList, Map<String, OverviewDO> overviewMap) {
        Map<String, DataSumDO> dataMapForA = new HashMap<>();
        for (int i = 0; i < expList.size(); i++) {
            BaseExp exp = expList.get(i);
            log.info("开始处理实验" + exp.getAlias());
            List<DataSumDO> dataList = dataSumService.getAll(new DataSumQuery().setOverviewId(overviewMap.get(exp.getId()).getId()).setDecoy(false).setStatus(IdentifyStatus.SUCCESS.getCode()), project.getId());
            if (dataMapForA.isEmpty()) {
                dataMapForA = dataList.stream().collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));
            } else {
                for (int j = 0; j < dataList.size(); j++) {
                    DataSumDO existedDataSum = dataMapForA.get(dataList.get(j).getPeptideRef());
                    existedDataSum.setSum(existedDataSum.getSum() + dataList.get(j).getSum());
                }
            }
        }

        return dataMapForA;
    }
}
