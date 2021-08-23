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
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Slf4j
@Component("experimentTask")
public class ExperimentTask extends BaseTask {

    @Autowired
    ExperimentService experimentService;
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
    public void uploadAird(List<ExperimentDO> exps, TaskDO taskDO) {
        try {
            taskDO.start().setStatus(TaskStatus.RUNNING.getName());
            taskService.update(taskDO);
            for (ExperimentDO exp : exps) {
                experimentService.uploadAirdFile(exp, taskDO);
                experimentService.update(exp);
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
     * experimentDO
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
    @Async(value = "extractorExecutor")
    public void doProPro(TaskDO taskDO, ExperimentDO exp, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        //如果还没有计算irt,先执行计算irt的步骤.
        if (exp.getIrt() == null || (params.getForceIrt() && params.getIrtLibraryId() != null)) {
            boolean exeResult = doIrt(taskDO, exp, params);
            if (!exeResult) {
                return;
            }
            log.info("Irt消耗时间:" + (System.currentTimeMillis() - start));
        }

        boolean eppsResult = doEpps(taskDO, exp, params);
        if (!eppsResult) {
            return;
        }

        LearningParams ap = new LearningParams();
        ap.setScoreTypes(params.getMethod().getScore().getScoreTypes());
        ap.setFdr(params.getMethod().getClassifier().getFdr());
        FinalResult finalResult = semiSupervise.doSemiSupervise(params.getOverviewId(), ap);
        taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + finalResult.getMatchedPeptideCount() + "最终识别的蛋白数目为:" + finalResult.getMatchedProteinCount());
        if (finalResult.getMatchedProteinCount() != null && finalResult.getMatchedProteinCount() != 0) {
            taskDO.addLog("Peptide/Protein Rate:" + finalResult.getMatchedPeptideCount() / finalResult.getMatchedProteinCount());
        }
    }

    @Async(value = "extractorExecutor")
    public void irt(TaskDO taskDO, List<ExperimentDO> exps, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        doIrt(taskDO, exps, params);
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
        log.info("所有实验IRT计算完毕,耗时:" + (System.currentTimeMillis() - start));
    }

    public void doIrt(TaskDO taskDO, List<ExperimentDO> exps, AnalyzeParams params) {
        Irt irt = params.getMethod().getIrt().isUseAnaLibForIrt() ? irtByAnaLib : irtByInsLib;
        for (ExperimentDO exp : exps) {
            taskDO.addLog("Start Analyzing for iRT: " + exp.getName());
            taskDO.addBindingExp(exp.getId());
            taskService.update(taskDO);
            Result<IrtResult> result = irt.align(exp, params);

            if (result.isFailed()) {
                taskDO.addLog("iRT计算失败:" + result.getErrorMessage());
                taskService.update(taskDO);
                continue;
            }
            SlopeIntercept slopeIntercept = result.getData().getSi();
            taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        }
    }

    public boolean doIrt(TaskDO taskDO, ExperimentDO exp, AnalyzeParams params) {
        Irt irt = params.getMethod().getIrt().isUseAnaLibForIrt() ? irtByAnaLib : irtByInsLib;
        taskDO.addLog("Start Analyzing for iRT: " + exp.getName());
        taskDO.addBindingExp(exp.getId());
        taskService.update(taskDO);
        Result<IrtResult> result = irt.align(exp, params);
        if (result.isFailed()) {
            taskDO.finish(TaskStatus.FAILED.getName(), "iRT计算失败:" + result.getErrorMessage());
            taskService.update(taskDO);
            return false;
        }
        SlopeIntercept slopeIntercept = result.getData().getSi();
        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        return true;
    }

    public boolean doEpps(TaskDO taskDO, ExperimentDO exp, AnalyzeParams params) {
        long start = System.currentTimeMillis();
        taskDO.addLog("Irt Result:" + exp.getIrt().getSi().getFormula()).addLog("入参准备完毕,开始提取数据(打分)");
        taskService.update(taskDO);
        params.setTaskDO(taskDO);
        Result result = extractor.extract(exp, params);
        taskService.update(taskDO);
        if (result.isFailed()) {
            taskDO.finish(TaskStatus.FAILED.getName(), "任务执行失败:" + result.getErrorMessage());
            taskService.update(taskDO);
            return false;
        }
        taskDO.addLog("处理完毕,EPPS总耗时:" + (System.currentTimeMillis() - start) + "毫秒,开始进行合并打分.....");
        log.info("EPPS耗时:" + (System.currentTimeMillis() - start));
        return true;
    }
}
