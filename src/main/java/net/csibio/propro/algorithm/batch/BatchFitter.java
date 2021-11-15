package net.csibio.propro.algorithm.batch;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.batch.bean.DataSum;
import net.csibio.propro.algorithm.batch.bean.GroupStat;
import net.csibio.propro.algorithm.batch.bean.MergedDataSum;
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
    public Result<GroupStat> merge(ProjectDO project, String groupLabel) {
        List<BaseExp> expList = experimentService.getAll(new ExperimentQuery().setProjectId(project.getId()).setLabel(groupLabel), BaseExp.class);
        Map<String, OverviewDO> overviewMap = overviewService.getDefaultOverviews(expList.stream().map(BaseExp::getId).collect(Collectors.toList()));
        if (overviewMap.size() != expList.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_HAVE_NO_DEFAULT_OVERVIEW);
        }
        GroupStat stat = merge(project, expList, overviewMap);
        return Result.OK(stat);
    }

    public GroupStat merge(ProjectDO project, List<BaseExp> expList, Map<String, OverviewDO> overviewMap) {
        Map<String, MergedDataSum> dataMap = new HashMap<>();
        Map<String, DataSum> dataResultMap = new HashMap<>();
        for (BaseExp exp : expList) {
            List<DataSum> dataList = dataSumService.getAll(
                            new DataSumQuery().setOverviewId(overviewMap.get(exp.getId()).getId())
                                    .setDecoy(false)
                                    .setIsUnique(true)
                                    .setStatus(IdentifyStatus.SUCCESS.getCode()), DataSum.class, project.getId())
                    .stream().filter(data -> !data.getProteins().get(0).startsWith("reverse")).collect(Collectors.toList());
            if (dataMap.isEmpty()) {
                dataMap = dataList.stream().collect(Collectors.toMap(DataSum::getPeptideRef, data -> new MergedDataSum(data, 1)));
            } else {
                for (DataSum dataSum : dataList) {
                    MergedDataSum mergedDataSum = dataMap.get(dataSum.getPeptideRef());
                    if (mergedDataSum == null) { //如果之前没有相关的信息,则加入到Map中
                        dataMap.put(dataSum.getPeptideRef(), new MergedDataSum(dataSum, 1));
                    } else {
                        mergedDataSum.getData().setIntensitySum(mergedDataSum.getData().getIntensitySum() + dataSum.getIntensitySum());
                        mergedDataSum.setEffectNum(mergedDataSum.getEffectNum() + 1); //记录有效实验的数目,用于计算平均值
                    }
                }
            }
        }
        dataMap.forEach((key, value) -> {
            value.getData().setIntensitySum(value.getData().getIntensitySum() / value.getEffectNum());
            dataResultMap.put(value.getData().getProteins().get(0) + "-->" + key, value.getData());
        });
        GroupStat stat = new GroupStat();
        stat.setDataMap(dataResultMap);
        int validNum = dataMap.values().stream().mapToInt(MergedDataSum::getEffectNum).sum();

        int proteins = dataMap.values().stream().map(data -> data.getData().getProteins().get(0)).collect(Collectors.toSet()).size();
        stat.setMissingRatio(1 - validNum * 1.0 / (expList.size() * dataResultMap.size()));
        stat.setHit1((int) dataMap.values().stream().filter(data -> data.getEffectNum() == 1).count());
        stat.setHit2((int) dataMap.values().stream().filter(data -> data.getEffectNum() == 2).count());
        stat.setHit3((int) dataMap.values().stream().filter(data -> data.getEffectNum() == 3).count());
        stat.setProteins(proteins);
        return stat;
    }
}
