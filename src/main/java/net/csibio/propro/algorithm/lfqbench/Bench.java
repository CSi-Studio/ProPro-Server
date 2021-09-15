package net.csibio.propro.algorithm.lfqbench;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.lfqbench.bean.BenchStat;
import net.csibio.propro.algorithm.lfqbench.bean.PeptideRatio;
import net.csibio.propro.algorithm.lfqbench.bean.ProteinRatio;
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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component("bench")
public class Bench {

    public static final String LABEL_A = "A";
    public static final String LABEL_B = "B";
    public static final String HUMAN = "HUMAN";
    public static final String YEAS8 = "YEAS8";
    public static final String ECOLI = "ECOLI";
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


    public Result<BenchStat<PeptideRatio>> buildPeptideRatio(ProjectDO project) {
        List<BaseExp> expList = experimentService.getAll(new ExperimentQuery().setProjectId(project.getId()), BaseExp.class);
        List<BaseExp> expAList = expList.stream().filter(exp -> exp.getLabel().equals(LABEL_A)).collect(Collectors.toList());
        List<BaseExp> expBList = expList.stream().filter(exp -> exp.getLabel().equals(LABEL_B)).collect(Collectors.toList());
        Map<String, OverviewDO> overviewMap = overviewService.getDefaultOverviews(expList.stream().map(BaseExp::getId).collect(Collectors.toList()));
        if (overviewMap.size() != expList.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_HAVE_NO_DEFAULT_OVERVIEW);
        }
        Map<String, DataSumDO> dataMapForA = mergeData(project, expAList, overviewMap);
        Map<String, DataSumDO> dataMapForB = mergeData(project, expBList, overviewMap);
        List<PeptideRatio> humanPoints = new ArrayList<>();
        List<PeptideRatio> yeastPoints = new ArrayList<>();
        List<PeptideRatio> ecoliPoints = new ArrayList<>();
        dataMapForA.forEach((key, a) -> {
            if (dataMapForB.containsKey(key)) {
                DataSumDO b = dataMapForB.get(key);
                PeptideRatio peptideRatio = new PeptideRatio(key, Math.log(b.getSum()) / Math.log(2), Math.log(a.getSum() / b.getSum()) / Math.log(2));
                if (a.getProteins().get(0).endsWith(HUMAN)) {
                    humanPoints.add(peptideRatio);
                } else if (a.getProteins().get(0).endsWith(YEAS8)) {
                    yeastPoints.add(peptideRatio);
                } else {
                    ecoliPoints.add(peptideRatio);
                }
            }
        });

        BenchStat<PeptideRatio> points = new BenchStat<>(humanPoints, yeastPoints, ecoliPoints);
        DescriptiveStatistics human = new DescriptiveStatistics();
        humanPoints.forEach(p -> human.addValue(p.y()));

        DescriptiveStatistics yeast = new DescriptiveStatistics();
        yeastPoints.forEach(p -> yeast.addValue(p.y()));

        DescriptiveStatistics ecoli = new DescriptiveStatistics();
        ecoliPoints.forEach(p -> ecoli.addValue(p.y()));

        points.setIdentifyNumA(dataMapForA.size());
        points.setIdentifyNumB(dataMapForB.size());
        points.setHumanStat(human);
        points.setYeastStat(yeast);
        points.setEcoliStat(ecoli);
        return Result.OK(points);
    }

    public Result<BenchStat<ProteinRatio>> buildProteinRatio(ProjectDO project) {
        return null;
    }

    /**
     * 合并策略
     * 1. 合并同类型实验的时候,如果某一个实验没有检测到值,那么剔除该实验
     * 2. 最终的定量值为多个实验的平均值(不包括被剔除的实验)
     *
     * @param project
     * @return
     */
    private Map<String, DataSumDO> mergeData(ProjectDO project, List<BaseExp> expList, Map<String, OverviewDO> overviewMap) {
        Map<String, DataSumWrapper> dataMap = new HashMap<>();
        Map<String, DataSumDO> dataResultMap = new HashMap<>();
        for (BaseExp exp : expList) {
            log.info("开始处理实验" + exp.getAlias());
            List<DataSumDO> dataList = dataSumService.getAll(
                            new DataSumQuery().setOverviewId(overviewMap.get(exp.getId()).getId())
                                    .setDecoy(false)
                                    .setIsUnique(true)
                                    .setStatus(IdentifyStatus.SUCCESS.getCode()), project.getId())
                    .stream().filter(data -> !data.getProteins().get(0).startsWith("reverse")).collect(Collectors.toList());
            if (dataMap.isEmpty()) {
                dataMap = dataList.stream().collect(Collectors.toMap(DataSumDO::getPeptideRef, data -> new DataSumWrapper(data, 1)));
            } else {
                for (DataSumDO dataSumDO : dataList) {
                    DataSumWrapper dataSumWrapper = dataMap.get(dataSumDO.getPeptideRef());
                    if (dataSumWrapper == null) { //如果之前没有相关的信息,则加入到Map中
                        dataMap.put(dataSumDO.getPeptideRef(), new DataSumWrapper(dataSumDO, 1));
                    } else {
                        dataSumWrapper.data.setSum(dataSumWrapper.data.getSum() + dataSumDO.getSum());
                        dataSumWrapper.effectNum = dataSumWrapper.effectNum + 1; //记录有效实验的数目,用于计算平均值
                    }
                }
            }
        }
        dataMap.forEach((key, value) -> {
            value.data.setSum(value.data.getSum() / value.effectNum);
            dataResultMap.put(key, value.data);
        });
        return dataResultMap;
    }

    public class DataSumWrapper {
        public DataSumDO data;
        public int effectNum;

        public DataSumWrapper(DataSumDO data, int effectNum) {
            this.data = data;
            this.effectNum = effectNum;
        }
    }

}
