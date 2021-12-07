package net.csibio.propro.algorithm.lfqbench;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.batch.BatchFitter;
import net.csibio.propro.algorithm.batch.bean.DataSum;
import net.csibio.propro.algorithm.batch.bean.GroupStat;
import net.csibio.propro.algorithm.lfqbench.bean.BenchStat;
import net.csibio.propro.algorithm.lfqbench.bean.PeptideRatio;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.run.BaseRun;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.RunQuery;
import net.csibio.propro.service.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Component("lfqBench")
public class LfqBench {

    public static final String LABEL_A = "A";
    public static final String LABEL_B = "B";
    public static final String HUMAN = "HUMAN";
    public static final String YEAS8 = "YEAS8";
    public static final String ECOLI = "ECOLI";

    @Autowired
    ProjectService projectService;
    @Autowired
    RunService runService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    DataService dataService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    BatchFitter batchFitter;

    public Result<BenchStat<PeptideRatio>> buildPeptideRatio(ProjectDO project) {
        List<BaseRun> runList = runService.getAll(new RunQuery().setProjectId(project.getId()), BaseRun.class);
        List<BaseRun> runAList = runList.stream().filter(run -> run.getGroup().equals(LABEL_A)).collect(Collectors.toList());
        List<BaseRun> runBList = runList.stream().filter(run -> run.getGroup().equals(LABEL_B)).collect(Collectors.toList());
        Map<String, OverviewDO> overviewMap = overviewService.getDefaultOverviews(runList.stream().map(BaseRun::getId).collect(Collectors.toList()));
        if (overviewMap.size() != runList.size()) {
            return Result.Error(ResultCode.SOME_RUN_HAVE_NO_DEFAULT_OVERVIEW);
        }
        GroupStat statForA = batchFitter.merge(project, runAList, overviewMap);
        GroupStat statForB = batchFitter.merge(project, runBList, overviewMap);
        List<PeptideRatio> humanPoints = new ArrayList<>();
        List<PeptideRatio> yeastPoints = new ArrayList<>();
        List<PeptideRatio> ecoliPoints = new ArrayList<>();
        AtomicLong humanA = new AtomicLong(0);
        AtomicLong yeastA = new AtomicLong(0);
        AtomicLong ecoliA = new AtomicLong(0);
        AtomicLong humanB = new AtomicLong(0);
        AtomicLong yeastB = new AtomicLong(0);
        AtomicLong ecoliB = new AtomicLong(0);

        statForA.getDataMap().values().forEach(sum -> {
            if (sum.getProteins().get(0).endsWith(HUMAN)) {
                humanA.getAndIncrement();
            } else if (sum.getProteins().get(0).endsWith(YEAS8)) {
                yeastA.getAndIncrement();
            } else {
                ecoliA.getAndIncrement();
            }
        });

        statForB.getDataMap().values().forEach(sum -> {
            if (sum.getProteins().get(0).endsWith(HUMAN)) {
                humanB.getAndIncrement();
            } else if (sum.getProteins().get(0).endsWith(YEAS8)) {
                yeastB.getAndIncrement();
            } else {
                ecoliB.getAndIncrement();
            }
        });

        statForA.getDataMap().forEach((key, a) -> {
            if (statForB.getDataMap().containsKey(key)) {
                DataSum b = statForB.getDataMap().get(key);
                PeptideRatio peptideRatio = new PeptideRatio(key, Math.log(b.getIntensitySum()) / Math.log(2), Math.log(a.getIntensitySum() / b.getIntensitySum()) / Math.log(2));
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
        humanPoints.forEach(p -> {
                    if (!Double.isInfinite(p.y())) {
                        human.addValue(p.y());
                    }
                }
        );


        DescriptiveStatistics yeast = new DescriptiveStatistics();
        yeastPoints.forEach(p -> {
            if (!Double.isInfinite(p.y())) {
                yeast.addValue(p.y());
            }
        });

        DescriptiveStatistics ecoli = new DescriptiveStatistics();
        ecoliPoints.forEach(p -> {
            if (!Double.isInfinite(p.y())) {
                ecoli.addValue(p.y());
            }
        });

        points.setIdentifyNumA(statForA.getDataMap().size());
        points.setMissingRatioA(statForA.getMissingRatio());
        points.setHit1A(statForA.getHit1());
        points.setHit2A(statForA.getHit2());
        points.setHit3A(statForA.getHit3());
        points.setIdentifyProteinNumA(statForA.getProteins());

        points.setIdentifyNumB(statForB.getDataMap().size());
        points.setMissingRatioB(statForB.getMissingRatio());
        points.setHit1B(statForB.getHit1());
        points.setHit2B(statForB.getHit2());
        points.setHit3B(statForB.getHit3());
        points.setIdentifyProteinNumB(statForB.getProteins());

        points.setHumanStat(human);
        points.setYeastStat(yeast);
        points.setEcoliStat(ecoli);
        points.setHumanA(humanA.get());
        points.setYeastA(yeastA.get());
        points.setEcoliA(ecoliA.get());
        points.setHumanB(humanB.get());
        points.setYeastB(yeastB.get());
        points.setEcoliB(ecoliB.get());
        return Result.OK(points);
    }
}
