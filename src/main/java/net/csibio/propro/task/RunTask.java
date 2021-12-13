package net.csibio.propro.task;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.irt.Irt;
import net.csibio.propro.algorithm.irt.IrtByInsLib;
import net.csibio.propro.algorithm.learner.SemiSupervise;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.exceptions.XException;
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
    @Async(value = "csiExecutor")
    public void doWorkflow(TaskDO taskDO, RunDO run, AnalyzeParams params) {
        try {
            long start = System.currentTimeMillis();
            //Step1. 如果还没有计算irt,先执行计算irt的步骤.
            if (run.getIrt() == null || (params.getForceIrt() && params.getInsLibId() != null)) {
                doIrt(taskDO, run, params);
                LogUtil.log("Irt消耗时间", start);
            }

            //Step2 全新套路
            doEpps(taskDO, run, params);

            //Step6. 机器学习,LDA分类
            logger.info(run.getAlias() + "开始执行机器学习部分");
            LearningParams ap = new LearningParams();
            ap.setScoreTypes(params.getMethod().getScore().getScoreTypes());
            ap.setFdr(params.getMethod().getClassifier().getFdr());
            ap.setClassifier(params.getMethod().getClassifier().getAlgorithm());
            FinalResult finalResult = semiSupervise.doSemiSupervise(params.getOverviewId(), ap);
            taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒");
            if (finalResult.getMatchedUniqueProteinCount() != null && finalResult.getMatchedUniqueProteinCount() != 0) {
                taskDO.addLog("Peptide/Protein Rate:" + finalResult.getMatchedPeptideCount() / finalResult.getMatchedUniqueProteinCount());
            }

            //Step7. Reselect ions
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        } catch (XException xe) {
            taskDO.finish(TaskStatus.FAILED.getName(), xe.getErrorMsg());
            taskService.update(taskDO);
            xe.printStackTrace();
        }

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
        Irt irt = irtByInsLib;
        for (RunDO run : runs) {
            taskDO.addLog("Start Analyzing for iRT: " + run.getName());
            taskDO.addBindingRun(run.getId());
            taskService.update(taskDO);
            try {
                IrtResult irtResult = irt.align(run, params);
                SlopeIntercept slopeIntercept = irtResult.getSi();
                taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
            } catch (XException xe) {
                taskDO.addLog("Irt Error:" + xe.getErrorMsg());
            }

        }
    }

    public void doIrt(TaskDO taskDO, RunDO run, AnalyzeParams params) throws XException {
        Irt irt = irtByInsLib;
        taskDO.addLog("Start Analyzing for iRT: " + run.getName());
        taskDO.addBindingRun(run.getId());
        taskService.update(taskDO);
        IrtResult irtResult = irt.align(run, params);
        SlopeIntercept slopeIntercept = irtResult.getSi();
        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
    }

    public void doEpps(TaskDO taskDO, RunDO run, AnalyzeParams params) throws XException {
        long start = System.currentTimeMillis();
        taskDO.addLog("Irt Result:" + run.getIrt().getSi().getFormula()).addLog("入参准备完毕,开始提取数据(打分)");
        taskService.update(taskDO);
        params.setTaskDO(taskDO);
        OverviewDO overview = extractor.extractRun(run, params);
        taskService.update(taskDO);
        taskDO.addLog("处理完毕,EPPS总耗时:" + (System.currentTimeMillis() - start) / 1000 + "秒,开始进行合并打分.....");
    }
}
