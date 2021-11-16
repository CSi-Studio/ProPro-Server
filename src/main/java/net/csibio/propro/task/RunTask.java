package net.csibio.propro.task;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.irt.Irt;
import net.csibio.propro.algorithm.irt.IrtByAnaLib;
import net.csibio.propro.algorithm.irt.IrtByInsLib;
import net.csibio.propro.algorithm.learner.SemiSupervise;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Slf4j
@Component("runTask")
public class RunTask extends BaseTask {

    @Autowired
    RunService runService;
    @Autowired
    DataService dataService;
    @Autowired
    Scorer scorer;
    @Autowired
    SemiSupervise semiSupervise;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    LibraryService libraryService;
    @Autowired
    IrtByAnaLib irtByAnaLib;
    @Autowired
    IrtByInsLib irtByInsLib;
    @Autowired
    OverviewService overviewService;
    @Autowired
    Extractor extractor;

    @Async(value = "uploadFileExecutor")
    public void uploadAird(List<RunDO> runs, TaskDO taskDO) {
        try {
            taskDO.start().setStatus(TaskStatus.RUNNING.getName());
            taskService.update(taskDO);
            for (RunDO run : runs) {
                runService.uploadAirdFile(run, taskDO);
                runService.update(run);
            }
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        } catch (Exception e) {
            e.printStackTrace();
            taskDO.addLog(e.getMessage());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }
    }

    /**
     * WorkflowParams 包含
     * runDO
     * libraryId
     * slopeIntercept
     * ownerName
     * rtExtractWindow
     * mzExtractWindow
     * useEpps
     * scoreTypes
     * sigmaSpacing
     * shapeScoreThreshold
     * shapeScoreWeightThreshold
     *
     * @return
     */
    @Async(value = "eicExecutor")
    public void doProPro(TaskDO taskDO, RunDO run, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        //Step1. 如果还没有计算irt,先执行计算irt的步骤.
        if (run.getIrt() == null || (params.getForceIrt() && params.getIrtLibraryId() != null)) {
            boolean exeResult = doIrt(taskDO, run, params);
            if (!exeResult) {
                return;
            }
            LogUtil.log("Irt消耗时间", start);
        }

        //Step2,3,4,5 EIC,选峰,选峰组,打分四个关键步骤
        boolean eppsResult = doEpps(taskDO, run, params);
        if (!eppsResult) {
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
            return;
        }

        //Step6. 机器学习,LDA分类
        logger.info(run.getAlias() + "开始执行机器学习部分");
        LearningParams ap = new LearningParams();
        ap.setScoreTypes(params.getMethod().getScore().getScoreTypes());
        ap.setFdr(params.getMethod().getClassifier().getFdr());
        FinalResult finalResult = semiSupervise.doSemiSupervise(params.getOverviewId(), ap);
        taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒");
        log.info("流程执行完毕,总耗时:" + ((System.currentTimeMillis() - start) / 1000) + "秒");
        if (finalResult.getMatchedUniqueProteinCount() != null && finalResult.getMatchedUniqueProteinCount() != 0) {
            taskDO.addLog("Peptide/Protein Rate:" + finalResult.getMatchedPeptideCount() / finalResult.getMatchedUniqueProteinCount());
        }

        //Step7. Reselect ions
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "csiExecutor")
    public void doCSi(TaskDO taskDO, RunDO run, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        //Step1. 如果还没有计算irt,先执行计算irt的步骤.
        if (run.getIrt() == null || (params.getForceIrt() && params.getIrtLibraryId() != null)) {
            boolean exeResult = doIrt(taskDO, run, params);
            if (!exeResult) {
                return;
            }
            LogUtil.log("Irt消耗时间", start);
        }

        //Step2 全新套路
        boolean csiResult = doEpps(taskDO, run, params);
        if (!csiResult) {
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
            return;
        }

        //Step6. 机器学习,LDA分类
        logger.info(run.getAlias() + "开始执行机器学习部分");
        LearningParams ap = new LearningParams();
        ap.setScoreTypes(params.getMethod().getScore().getScoreTypes());
        ap.setFdr(params.getMethod().getClassifier().getFdr());
        FinalResult finalResult = semiSupervise.doSemiSupervise(params.getOverviewId(), ap);
        taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒");
        log.info("流程执行完毕,总耗时:" + ((System.currentTimeMillis() - start) / 1000) + "秒");
        if (finalResult.getMatchedUniqueProteinCount() != null && finalResult.getMatchedUniqueProteinCount() != 0) {
            taskDO.addLog("Peptide/Protein Rate:" + finalResult.getMatchedPeptideCount() / finalResult.getMatchedUniqueProteinCount());
        }

        //Step7. Reselect ions
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "eicExecutor")
    public void irt(TaskDO taskDO, List<RunDO> runs, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        doIrt(taskDO, runs, params);
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
        log.info("所有实验IRT计算完毕,耗时:" + (System.currentTimeMillis() - start));
    }

    public void doIrt(TaskDO taskDO, List<RunDO> runs, AnalyzeParams params) {
        Irt irt = params.getMethod().getIrt().isUseAnaLibForIrt() ? irtByAnaLib : irtByInsLib;
        for (RunDO run : runs) {
            taskDO.addLog("Start Analyzing for iRT: " + run.getName());
            taskDO.addBindingRun(run.getId());
            taskService.update(taskDO);
            Result<IrtResult> result = irt.align(run, params);

            if (result.isFailed()) {
                taskDO.addLog("iRT计算失败:" + result.getErrorMessage());
                taskService.update(taskDO);
                continue;
            }
            SlopeIntercept slopeIntercept = result.getData().getSi();
            taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        }
    }

    public boolean doIrt(TaskDO taskDO, RunDO run, AnalyzeParams params) {
        Irt irt = params.getMethod().getIrt().isUseAnaLibForIrt() ? irtByAnaLib : irtByInsLib;
        taskDO.addLog("Start Analyzing for iRT: " + run.getName());
        taskDO.addBindingRun(run.getId());
        taskService.update(taskDO);
        Result<IrtResult> result = irt.align(run, params);
        if (result.isFailed()) {
            taskDO.finish(TaskStatus.FAILED.getName(), "iRT计算失败:" + result.getErrorMessage());
            taskService.update(taskDO);
            return false;
        }
        SlopeIntercept slopeIntercept = result.getData().getSi();
        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        return true;
    }

    public boolean doEpps(TaskDO taskDO, RunDO run, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        taskDO.addLog("Irt Result:" + run.getIrt().getSi().getFormula()).addLog("入参准备完毕,开始提取数据(打分)");
        taskService.update(taskDO);
        params.setTaskDO(taskDO);
        Result<OverviewDO> result = extractor.extract(run, params);
        taskService.update(taskDO);
        if (result.isFailed()) {
            taskDO.finish(TaskStatus.FAILED.getName(), "任务执行失败:" + result.getErrorMessage());
            taskService.update(taskDO);
            return false;
        }
        taskDO.addLog("处理完毕,EPPS总耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒,开始进行合并打分.....");
        log.info("EPPS耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒");
        return true;
    }
}
