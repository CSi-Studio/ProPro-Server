package net.csibio.propro.algorithm.batch;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.batch.bean.BatchDataSum;
import net.csibio.propro.algorithm.batch.bean.DataSum;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.experiment.BaseExp;
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
import java.util.stream.Collectors;

@Slf4j
@Component("batchFitter")
public class BatchFitter {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ProjectService projectService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    DataService dataService;
    @Autowired
    OverviewService overviewService;

    /**
     * 合并同一个项目下指定label的实验结果
     *
     * @param project
     * @param groupLabel
     * @return
     */
    public Result<Map<String, DataSum>> merge(ProjectDO project, String groupLabel) {
        List<BaseExp> expList = experimentService.getAll(new ExperimentQuery().setProjectId(project.getId()).setLabel(groupLabel), BaseExp.class);
        Map<String, OverviewDO> overviewMap = overviewService.getDefaultOverviews(expList.stream().map(BaseExp::getId).collect(Collectors.toList()));
        if (overviewMap.size() != expList.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_HAVE_NO_DEFAULT_OVERVIEW);
        }
        Map<String, DataSum> dataMap = merge(project, expList, overviewMap);
        return Result.OK(dataMap);
    }

    public Map<String, DataSum> merge(ProjectDO project, List<BaseExp> expList, Map<String, OverviewDO> overviewMap) {
        Map<String, BatchDataSum> dataMap = new HashMap<>();
        Map<String, DataSum> dataResultMap = new HashMap<>();
        for (BaseExp exp : expList) {
            log.info("开始处理实验" + exp.getAlias());
            List<DataSum> dataList = dataSumService.getAll(
                            new DataSumQuery().setOverviewId(overviewMap.get(exp.getId()).getId())
                                    .setDecoy(false)
                                    .setIsUnique(true)
                                    .setStatus(IdentifyStatus.SUCCESS.getCode()), DataSum.class, project.getId())
                    .stream().filter(data -> !data.getProteins().get(0).startsWith("reverse")).collect(Collectors.toList());
            if (dataMap.isEmpty()) {
                dataMap = dataList.stream().collect(Collectors.toMap(DataSum::getPeptideRef, data -> new BatchDataSum(data, 1)));
            } else {
                for (DataSum dataSum : dataList) {
                    BatchDataSum batchDataSum = dataMap.get(dataSum.getPeptideRef());
                    if (batchDataSum == null) { //如果之前没有相关的信息,则加入到Map中
                        dataMap.put(dataSum.getPeptideRef(), new BatchDataSum(dataSum, 1));
                    } else {
                        batchDataSum.getData().setSum(batchDataSum.getData().getSum() + dataSum.getSum());
                        batchDataSum.setEffectNum(batchDataSum.getEffectNum() + 1); //记录有效实验的数目,用于计算平均值
                    }
                }
            }
        }
        dataMap.forEach((key, value) -> {
            value.getData().setSum(value.getData().getSum() / value.getEffectNum());
            dataResultMap.put(key, value.getData());
        });
        return dataResultMap;
    }
}
