package net.csibio.propro.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.learner.SemiSupervise;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.score.features.InitScorer;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.data.PeptideScore;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroupScore;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.*;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.ProProUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("test")
@Slf4j
public class TestController {

    @Autowired
    ProjectService projectService;
    @Autowired
    ProteinService proteinService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    RunService runService;
    @Autowired
    MethodService methodService;
    @Autowired
    SemiSupervise semiSupervise;
    @Autowired
    DataService dataService;
    @Autowired
    Lda lda;
    @Autowired
    Statistics statistics;
    @Autowired
    Scorer scorer;
    @Autowired
    InitScorer initScorer;

    @GetMapping(value = "/lms")
    Result lms() {
        List<IdName> projects = projectService.getAll(new ProjectQuery(), IdName.class);
        for (IdName project : projects) {
            List<OverviewDO> overviewList = overviewService.getAll(new OverviewQuery().setProjectId(project.id()));
            for (OverviewDO overviewDO : overviewList) {
//                overviewDO.setReselect(overviewDO.getReselect());
//                overviewService.update(overviewDO);
            }
        }

        return Result.OK();
    }

    @GetMapping(value = "/lms2")
    Result lms2() {
        String projectId = "6166a5fd6113c157a6431ab9";
        List<IdName> idNameList = overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        List<String> overviewIds = idNameList.stream().map(IdName::id).collect(Collectors.toList());
        overviewIds.clear();
        overviewIds.add("618e88d2ef9fa50368158ef8");
//        overviewIds.add("61713f52749794487dd90937");
//        overviewIds.add("61713f52749794487dd90938");
//        overviewIds.add("61713f52749794487dd90934");
//        overviewIds.add("61713f52749794487dd90935");
//        overviewIds.add("61713f52749794487dd90932");

        log.info("一共overview" + overviewIds.size() + "个");
        double k = 2d;
        double n = 0.1d;
        while (n < k) {
            n += 0.1;
            log.info("开始尝试n=" + n);
            boolean success = true;
            for (String overviewId : overviewIds) {
                OverviewDO overview = overviewService.getById(overviewId);
                LearningParams params = new LearningParams();
                params.setScoreTypes(overview.fetchScoreTypes());
                params.setFdr(overview.getParams().getMethod().getClassifier().getFdr());

                FinalResult finalResult = new FinalResult();

                //Step1. 数据预处理
                log.info("数据预处理");
                params.setType(overview.getType());
                //Step2. 从数据库读取全部含打分结果的数据
                log.info("开始获取打分数据");
                List<PeptideScore> peptideList = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()), PeptideScore.class, overview.getProjectId());
                if (peptideList == null || peptideList.size() == 0) {
                    log.info("没有合适的数据");
                    return null;
                }
                log.info("总计有待鉴定态肽段" + peptideList.size() + "个");

                log.info("重新计算初始分完毕");
                //Step3. 开始训练数据集
                HashMap<String, Double> weightsMap = lda.classifier(peptideList, params, overview.fetchScoreTypes());
                lda.score(peptideList, weightsMap, params.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);

                //进行第一轮严格意义的初筛
                log.info("开始第一轮严格意义上的初筛");
                List<SelectedPeakGroupScore> selectedPeakGroupListV1 = null;
                try {
                    selectedPeakGroupListV1 = scorer.findBestPeakGroupByTargetScoreType(peptideList, ScoreType.WeightedTotalScore.getName(), overview.fetchScoreTypes(), true);
                    statistics.errorStatistics(selectedPeakGroupListV1, params);
                    semiSupervise.giveDecoyFdr(selectedPeakGroupListV1);
                    //获取第一轮严格意义上的最小总分阈值
                    double minTotalScore = selectedPeakGroupListV1.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroupScore::getFdr)).get().getTotalScore();
                    log.info("初筛下的最小总分值为:" + minTotalScore + ";开始第二轮筛选");
                    List<SelectedPeakGroupScore> selectedPeakGroupListV2 = scorer.findBestPeakGroupByTargetScoreTypeAndMinTotalScore(peptideList,
                            ScoreType.WeightedTotalScore.getName(),
                            overview.getParams().getMethod().getScore().getScoreTypes(),
                            minTotalScore);
                    //重新统计
                    ErrorStat errorStat = statistics.errorStatistics(selectedPeakGroupListV2, params);
                    semiSupervise.giveDecoyFdr(selectedPeakGroupListV2);

                    long start = System.currentTimeMillis();
                    //Step4. 对于最终的打分结果和选峰结果保存到数据库中, 插入最终的DataSum表的数据为所有的鉴定结果以及 fdr小于0.01的伪肽段
                    log.info("将合并打分及定量结果反馈更新到数据库中,总计:" + selectedPeakGroupListV2.size() + "条数据,开始统计相关数据,FDR:" + params.getFdr());
                    minTotalScore = selectedPeakGroupListV2.stream().filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr()).max(Comparator.comparingDouble(SelectedPeakGroupScore::getFdr)).get().getTotalScore();

                    log.info("最小阈值总分为:" + minTotalScore);
                    dataSumService.buildDataSumList(selectedPeakGroupListV2, params.getFdr(), overview, overview.getProjectId());
                    log.info("插入Sum数据" + selectedPeakGroupListV2.size() + "条一共用时：" + (System.currentTimeMillis() - start) + "毫秒");
                    overview.setWeights(weightsMap);

                    semiSupervise.targetDecoyDistribution(selectedPeakGroupListV2, overview); //统计Target Decoy分布的函数
                    overviewService.update(overview);
                    overviewService.statistic(overview);

                    finalResult.setAllInfo(errorStat);
                    int count = ProProUtil.checkFdr(finalResult, params.getFdr());
                    if (count < 20000) {
                        success = false;
                        break;
                    }
                    log.info("合并打分完成,共找到新肽段" + count + "个");
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                    break;
                }

            }
            if (success) {
                log.info("成功啦:n=" + n);
                break;
            }
        }

        return Result.OK();
    }

    @GetMapping(value = "/lms3")
    Result lms3() {
        List<LibraryDO> libraryList = libraryService.getAll(new LibraryQuery());
        log.info("总计有库:" + libraryList.size() + "个");
        AtomicInteger count = new AtomicInteger(1);
        libraryList.stream().parallel().forEach(library -> {
            log.info("开始处理库:" + library.getName() + ":" + count.get() + "/" + libraryList.size());
            count.getAndIncrement();
            List<PeptideDO> peptideList = peptideService.getAll(new PeptideQuery(library.getId()));
            peptideList.forEach(peptide -> {
                peptide.setFragments(peptide.getFragments().stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList()));
                peptide.setDecoyFragments(peptide.getDecoyFragments().stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList()));
                peptideService.update(peptide);
            });
            log.info("库" + library.getName() + "处理完毕");
        });
        return Result.OK();
    }

    @GetMapping(value = "/lms4")
    Result lms4() {
        String projectId = "6166a5fd6113c157a6431ab9";
        List<IdName> idNameList = overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        idNameList = idNameList.subList(0, 1);
        for (IdName idName : idNameList) {
            String overviewId = idName.id();
            OverviewDO overview = overviewService.getById(overviewId);

            log.info("读取数据库信息中");
            Map<String, PeptideScore> peptideMap = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()).setDecoy(false), PeptideScore.class, overview.getProjectId()).stream().collect(Collectors.toMap(PeptideScore::getId, Function.identity()));
            Map<String, DataSumDO> sumMap = dataSumService.getAll(new DataSumQuery().setOverviewId(overviewId).setDecoy(false), DataSumDO.class, overview.getProjectId()).stream().collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));
            AtomicLong stat = new AtomicLong(0);
            List<String> findItList = new ArrayList<>();
            peptideMap.values().forEach(data -> {
                DataSumDO sum = sumMap.get(data.getPeptideRef());
                if (sum != null && !sum.getStatus().equals(IdentifyStatus.SUCCESS.getCode())) {
                    PeakGroup peakGroup = data.getPeakGroupList().stream().filter(peak -> peak.getApexRt().equals(sum.getApexRt())).findFirst().get();
                    double libPearson = peakGroup.get(ScoreType.LibraryCorr, ScoreType.usedScoreTypes());
                    double libDotprod = peakGroup.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes());
                    double isoOverlap = peakGroup.get(ScoreType.IsotopeOverlapScore, ScoreType.usedScoreTypes());
                    if (libDotprod > 0.99) {
                        stat.getAndIncrement();
                        findItList.add(sum.getPeptideRef());
                    }
                }
            });
            log.info(overview.getRunName() + "-符合要求的Peptide有:" + stat.get() + "个");
            log.info(JSON.toJSONString(findItList));
        }

        return Result.OK();
    }

    @GetMapping(value = "/lms5")
    Result lms5() {
        String projectId = "6166a5fd6113c157a6431ab9";
        List<IdName> idNameList = overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        idNameList = idNameList.subList(0, 1);
        for (IdName idName : idNameList) {
            String overviewId = idName.id();

            OverviewDO overview = overviewService.getById(overviewId);

            log.info("读取数据库信息中");
            Map<String, PeptideScore> peptideMap = dataService.getAll(new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()).setDecoy(false), PeptideScore.class, overview.getProjectId()).stream().collect(Collectors.toMap(PeptideScore::getId, Function.identity()));
            Map<String, DataSumDO> sumMap = dataSumService.getAll(new DataSumQuery().setOverviewId(overviewId).setDecoy(false), DataSumDO.class, overview.getProjectId()).stream().collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));


            AtomicLong stat = new AtomicLong(0);
            List<String> findItList = new ArrayList<>();
        }

        return Result.OK();
    }

    @GetMapping(value = "/checkFragmentMz")
    Result checkFragmentMz() {
        long count = peptideService.count(new PeptideQuery());
        log.info("Total Peptides:" + count);
        long batch = count / 2000 + 1;
        PeptideQuery query = new PeptideQuery();
        query.setPageSize(2000);
        for (int i = 0; i < batch; i++) {
            query.setPageNo(i + 1);
            Result<List<PeptideDO>> pepListRes = peptideService.getList(query);
            if (pepListRes.isFailed()) {
                log.error(pepListRes.getErrorMessage());
            }
            List<PeptideDO> pepList = pepListRes.getData();
            if (pepList.size() == 0) {
                break;
            }
            pepList.forEach(pep -> {
                pep.getDecoyFragments().forEach(fragmentInfo -> {
                    if (fragmentInfo.getMz() == null) {
                        log.info(pep.getLibraryId() + "-" + pep.getPeptideRef() + "有问题");
                    }
                });
            });
            log.info("已扫描" + i + "/" + batch);
        }

        return Result.OK();
    }


    public static File[] getFiles(String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        File file = classPathResource.getFile();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    getFiles(files[i].getPath());
                } else {
                }
            }
            return files;
        } else {
            File[] files = new File[0];
            return files;
        }
    }
}
