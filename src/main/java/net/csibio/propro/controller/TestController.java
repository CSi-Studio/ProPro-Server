package net.csibio.propro.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.learner.SemiSupervise;
import net.csibio.propro.algorithm.learner.Statistics;
import net.csibio.propro.algorithm.learner.classifier.Lda;
import net.csibio.propro.algorithm.learner.classifier.Xgboost;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.data.PeptideRef;
import net.csibio.propro.domain.bean.learner.ErrorStat;
import net.csibio.propro.domain.bean.learner.FinalResult;
import net.csibio.propro.domain.bean.learner.LearningParams;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.query.*;
import net.csibio.propro.service.*;
import net.csibio.propro.utils.PeptideUtil;
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
@RequestMapping("/api/test")
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
    Xgboost xgboost;
    @Autowired
    Statistics statistics;
    @Autowired
    Scorer scorer;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    BlockIndexService blockIndexService;

    @GetMapping(value = "/lms")
    Result lms() {
        List<IdName> projects = projectService.getAll(new ProjectQuery(), IdName.class);
        for (IdName project : projects) {
            List<OverviewDO> overviewList =
                    overviewService.getAll(new OverviewQuery().setProjectId(project.id()));
            for (OverviewDO overviewDO : overviewList) {
                //                overviewDO.setReselect(overviewDO.getReselect());
                //                overviewService.update(overviewDO);
            }
        }

        return Result.OK();
    }

    @GetMapping(value = "/lms2_1")
    Result lms2_1() {
        String projectId = "613f5d8262cbcf5bb4345270";
        List<IdName> idNameList =
                overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        List<String> overviewIds = idNameList.stream().map(IdName::id).collect(Collectors.toList());
        overviewIds.clear();
        overviewIds.add("619b3907130b5f12ee620956");

        log.info("??????overview" + overviewIds.size() + "???");
        boolean success = true;
        for (String overviewId : overviewIds) {
            OverviewDO overview = overviewService.getById(overviewId);
            LearningParams params = new LearningParams();
            params.setScoreTypes(overview.fetchScoreTypes());
            params.setFdr(overview.getParams().getMethod().getClassifier().getFdr());
            FinalResult finalResult = new FinalResult();

            // Step1. ???????????????
            log.info("???????????????");
            params.setType(overview.getType());
            // Step2. ????????????????????????????????????????????????
            log.info("????????????????????????");
            List<DataScore> peptideList =
                    dataService.getAll(
                            new DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()),
                            DataScore.class,
                            overview.getProjectId());
            if (peptideList == null || peptideList.size() == 0) {
                log.info("?????????????????????");
                return null;
            }
            log.info("???????????????????????????" + peptideList.size() + "???");

            log.info("???????????????????????????");
            // Step3. ?????????????????????
            xgboost.classifier(peptideList, params);

            // ????????????????????????????????????
            log.info("???????????????????????????????????????");
            List<SelectedPeakGroup> selectedPeakGroupListV1 = null;
            try {
                selectedPeakGroupListV1 = scorer.findBestPeakGroup(peptideList);
                statistics.errorStatistics(selectedPeakGroupListV1, params);
                semiSupervise.giveDecoyFdr(selectedPeakGroupListV1);
                // ???????????????????????????????????????????????????
                double minTotalScore =
                        selectedPeakGroupListV1.stream()
                                .filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr())
                                .max(Comparator.comparingDouble(SelectedPeakGroup::getFdr))
                                .get()
                                .getTotalScore();
                log.info("??????????????????????????????:" + minTotalScore + ";?????????????????????");
                List<SelectedPeakGroup> selectedPeakGroupListV2 = scorer.findBestPeakGroup(peptideList);
                // ????????????
                ErrorStat errorStat = statistics.errorStatistics(selectedPeakGroupListV2, params);
                semiSupervise.giveDecoyFdr(selectedPeakGroupListV2);

                long start = System.currentTimeMillis();
                // Step4. ???????????????????????????????????????????????????????????????, ???????????????DataSum?????????????????????????????????????????? fdr??????0.01????????????
                log.info(
                        "?????????????????????????????????????????????????????????,??????:"
                                + selectedPeakGroupListV2.size()
                                + "?????????,????????????????????????,FDR:"
                                + params.getFdr());
                minTotalScore =
                        selectedPeakGroupListV2.stream()
                                .filter(s -> s.getFdr() != null && s.getFdr() < params.getFdr())
                                .max(Comparator.comparingDouble(SelectedPeakGroup::getFdr))
                                .get()
                                .getTotalScore();

                log.info("?????????????????????:" + minTotalScore);
                dataSumService.buildDataSumList(
                        selectedPeakGroupListV2, params.getFdr(), overview, overview.getProjectId());
                log.info(
                        "??????Sum??????"
                                + selectedPeakGroupListV2.size()
                                + "??????????????????"
                                + (System.currentTimeMillis() - start)
                                + "??????");

                semiSupervise.targetDecoyDistribution(
                        selectedPeakGroupListV2, overview); // ??????Target Decoy???????????????
                overviewService.update(overview);
                overviewService.statistic(overview);

                finalResult.setAllInfo(errorStat);
                int count = ProProUtil.checkFdr(finalResult, params.getFdr());
                if (count < 20000) {
                    success = false;
                    break;
                }
                log.info("??????????????????,??????????????????" + count + "???");
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
                break;
            }
        }

        return Result.OK();
    }

    @GetMapping(value = "/lms3")
    Result lms3() {
        List<LibraryDO> libraryList = libraryService.getAll(new LibraryQuery());
        log.info("????????????:" + libraryList.size() + "???");
        AtomicInteger count = new AtomicInteger(1);
        libraryList.stream()
                .parallel()
                .forEach(
                        library -> {
                            log.info("???????????????:" + library.getName() + ":" + count.get() + "/" + libraryList.size());
                            count.getAndIncrement();
                            List<PeptideDO> peptideList =
                                    peptideService.getAll(new PeptideQuery(library.getId()));
                            peptideList.forEach(
                                    peptide -> {
                                        fragmentFactory.calcFingerPrints(peptide);
                                        peptideService.update(peptide);
                                    });
                            log.info("???" + library.getName() + "????????????");
                        });
        return Result.OK();
    }

    @GetMapping(value = "/lms4")
    Result lms4() {
        String projectId = "613f5d8262cbcf5bb4345270";
        List<IdName> idNameList =
                overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        idNameList = idNameList.subList(0, 1);
        for (IdName idName : idNameList) {
            String overviewId = idName.id();
            OverviewDO overview = overviewService.getById(overviewId);

            log.info("????????????????????????");
            Map<String, DataScore> peptideMap =
                    dataService
                            .getAll(
                                    new DataQuery()
                                            .setOverviewId(overviewId)
                                            .setStatus(IdentifyStatus.WAIT.getCode())
                                            .setDecoy(false),
                                    DataScore.class,
                                    overview.getProjectId())
                            .stream()
                            .collect(Collectors.toMap(DataScore::getId, Function.identity()));
            Map<String, DataSumDO> sumMap =
                    dataSumService
                            .getAll(
                                    new DataSumQuery().setOverviewId(overviewId).setDecoy(false),
                                    DataSumDO.class,
                                    overview.getProjectId())
                            .stream()
                            .collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));
            AtomicLong stat = new AtomicLong(0);
            List<String> findItList = new ArrayList<>();
            peptideMap
                    .values()
                    .forEach(
                            data -> {
                                DataSumDO sum = sumMap.get(data.getPeptideRef());
                                if (sum != null && sum.getStatus().equals(IdentifyStatus.SUCCESS.getCode())) {
                                    PeakGroup peakGroup =
                                            data.getPeakGroupList().stream()
                                                    .filter(peak -> peak.getSelectedRt().equals(sum.getSelectedRt()))
                                                    .findFirst()
                                                    .get();
                                    double pearson = peakGroup.get(ScoreType.Pearson, ScoreType.usedScoreTypes());
//                                    double apexPearson = peakGroup.get(ScoreType.ApexPearson, ScoreType.usedScoreTypes());
                                    double libDotprod = peakGroup.get(ScoreType.Dotprod, ScoreType.usedScoreTypes());
                                    //                    double isoOverlap = peakGroup.get(ScoreType.IsoOverlap,
                                    // ScoreType.usedScoreTypes());
                                    if (pearson < 0.2) {
                                        stat.getAndIncrement();
                                        findItList.add(sum.getPeptideRef());
                                    }
                                }
                            });
            log.info(overview.getRunName() + "-???????????????Peptide???:" + stat.get() + "???");
            log.info(JSON.toJSONString(findItList));
        }

        return Result.OK();
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    @GetMapping(value = "/lms5")
    Result lms5() {
        String projectId = "613f5d8262cbcf5bb4345270";
        List<IdName> idNameList =
                overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        idNameList = idNameList.subList(0, 1);
        for (IdName idName : idNameList) {
            String overviewId = idName.id();

            OverviewDO overview = overviewService.getById(overviewId);
            log.info("????????????????????????");
            //            Map<String, PeptideScore> dataMap = dataService.getAll(new
            // DataQuery().setOverviewId(overviewId).setStatus(IdentifyStatus.WAIT.getCode()).setDecoy(false), PeptideScore.class, overview.getProjectId()).stream().collect(Collectors.toMap(PeptideScore::getId, Function.identity()));
            Map<String, DataSumDO> sumMap =
                    dataSumService
                            .getAll(
                                    new DataSumQuery().setOverviewId(overviewId).setDecoy(false),
                                    DataSumDO.class,
                                    overview.getProjectId())
                            .stream()
                            .collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));
            RunDO run = runService.getById(overview.getRunId());
            log.info("??????????????????:" + run.getAlias());
            List<WindowRange> ranges = run.getWindowRanges();
            Set<String> similarPeptides = new HashSet<>();
            log.info("?????????????????????");
            for (WindowRange range : ranges) {
                TreeMap<Double, List<DataSumDO>> rtMap = new TreeMap<>();
                Map<String, PeptideDO> peptideMap =
                        peptideService
                                .getAll(
                                        new PeptideQuery(overview.getAnaLibId())
                                                .setMzStart(range.getStart())
                                                .setMzEnd(range.getEnd()))
                                .stream()
                                .collect(Collectors.toMap(PeptideDO::getPeptideRef, Function.identity()));
                BlockIndexDO index = blockIndexService.getMS2(run.getId(), range.getMz());
                for (Float rt : index.getRts()) {
                    rtMap.put((double) rt, new ArrayList<>());
                }
                for (PeptideDO peptide : peptideMap.values()) {
                    DataSumDO sum = sumMap.get(peptide.getPeptideRef());
                    if (sum != null && sum.getStatus().equals(IdentifyStatus.SUCCESS.getCode())) {
                        rtMap.get(sum.getSelectedRt()).add(sum);
                    }
                }

                List<List<DataSumDO>> sumListList = new ArrayList<>(rtMap.values());
                for (int i = 0; i < sumListList.size() - 4; i++) {
                    List<DataSumDO> sumList = new ArrayList<>();
                    sumList.addAll(sumListList.get(i));
                    sumList.addAll(sumListList.get(i + 1));
                    sumList.addAll(sumListList.get(i + 2));
                    sumList.addAll(sumListList.get(i + 3));
                    sumList.addAll(sumListList.get(i + 4));
                    if (sumList.size() <= 1) {
                        continue;
                    }
                    for (int k = 0; k < sumList.size(); k++) {
                        for (int j = k + 1; j < sumList.size(); j++) {
                            int sequenceLengthA =
                                    peptideMap.get(sumList.get(k).getPeptideRef()).getSequence().length();
                            int sequenceLengthB =
                                    peptideMap.get(sumList.get(j).getPeptideRef()).getSequence().length();
                            int finalLength = Math.min(sequenceLengthA, sequenceLengthB);
                            if (PeptideUtil.similar(
                                    peptideMap.get(sumList.get(k).getPeptideRef()),
                                    peptideMap.get(sumList.get(j).getPeptideRef()),
                                    finalLength <= 8 ? 5 : 6)) {
                                similarPeptides.add(
                                        sumList.get(k).getPeptideRef() + ":" + sumList.get(j).getPeptideRef());
                            }
                        }
                    }
                }
            }

            log.info("??????????????????:" + similarPeptides.size() + "???");
            similarPeptides.forEach(System.out::println);
        }

        return Result.OK();
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    @GetMapping(value = "/lms6")
    Result lms6() {
        String projectId = "613f5d8262cbcf5bb4345270";
        List<IdName> idNameList =
                overviewService.getAll(new OverviewQuery(projectId).setDefaultOne(true), IdName.class);
        idNameList = idNameList.subList(0, 1);
        for (IdName idName : idNameList) {
            String overviewId = idName.id();
            List<String> targetPeptideList = new ArrayList<>();
            OverviewDO overview = overviewService.getById(overviewId);
            log.info("????????????????????????--" + overview.getRunName());
            Map<String, DataSumDO> sumMap =
                    dataSumService
                            .getAll(
                                    new DataSumQuery().setOverviewId(overviewId).setDecoy(false),
                                    DataSumDO.class,
                                    overview.getProjectId())
                            .stream()
                            .collect(Collectors.toMap(DataSumDO::getPeptideRef, Function.identity()));
            Map<String, DataScore> dataMap = dataService.getAll(
                            new DataQuery().setOverviewId(overviewId).setDecoy(false),
                            DataScore.class,
                            overview.getProjectId())
                    .stream()
                    .collect(Collectors.toMap(DataScore::getPeptideRef, Function.identity()));
            List<DataSumDO> sumList = sumMap.values().stream()
                    .filter(sum -> sum.getStatus().equals(IdentifyStatus.SUCCESS.getCode()))
                    .toList();
            for (DataSumDO sum : sumList) {
                DataScore data = dataMap.get(sum.getPeptideRef());
                lda.scoreForPeakGroups(data.getPeakGroupList(),
                        overview.getWeights(),
                        overview.getParams().getMethod().getScore().getScoreTypes());
                List<PeakGroup> peakGroupList = data.getPeakGroupList().stream()
                        .filter(peak -> peak.getTotalScore() > overview.getMinTotalScore())
                        .toList();
                if (peakGroupList.size() >= 2) {
                    peakGroupList = peakGroupList.stream()
                            .sorted(Comparator.comparing(PeakGroup::getTotalScore).reversed())
                            .toList();
                    if (peakGroupList.get(0).getTotalScore() - peakGroupList.get(1).getTotalScore() < 0.1) {
                        targetPeptideList.add(data.getPeptideRef());
                    }
                }
            }
            log.info("?????????????????????:" + targetPeptideList.size());
            log.info(JSON.toJSONString(targetPeptideList));
        }


        return Result.OK();
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    @GetMapping(value = "/lms7")
    Result lms7() {
        String overviewIdOld = "61a492078363936500b07026";
        String overviewIdNew = "61a63585000a1b6efde040e1";
        OverviewDO overviewOld = overviewService.getById(overviewIdOld);
        OverviewDO overviewNew = overviewService.getById(overviewIdNew);
        List<PeptideRef> peptideRefsOld = dataSumService.getAll(new DataSumQuery(overviewOld.getId()).setDecoy(false).setStatus(IdentifyStatus.SUCCESS.getCode()), PeptideRef.class, overviewOld.getProjectId());
        List<PeptideRef> peptideRefsNew = dataSumService.getAll(new DataSumQuery(overviewNew.getId()).setDecoy(false).setStatus(IdentifyStatus.SUCCESS.getCode()), PeptideRef.class, overviewOld.getProjectId());
        Set<String> oldPeptides = peptideRefsOld.stream().map(PeptideRef::getPeptideRef).collect(Collectors.toSet());
        Set<String> newPeptides = peptideRefsNew.stream().map(PeptideRef::getPeptideRef).collect(Collectors.toSet());
        List<String> ????????????????????? = new ArrayList<>();
        List<String> ????????????????????? = new ArrayList<>();
        for (String oldPeptide : oldPeptides) {
            if (!newPeptides.contains(oldPeptide)) {
                ?????????????????????.add(oldPeptide);
            }
        }
        for (String newPeptide : newPeptides) {
            if (!oldPeptides.contains(newPeptide)) {
                ?????????????????????.add(newPeptide);
            }
        }
        log.info("?????????????????????" + ?????????????????????.size() + "");
        log.info(JSON.toJSONString(?????????????????????.subList(0, 100)));
        log.info("?????????????????????" + ?????????????????????.size() + "");
        log.info(JSON.toJSONString(?????????????????????.subList(0, 100)));
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
            pepList.forEach(
                    pep -> {
                        pep.getDecoyFragments()
                                .forEach(
                                        fragmentInfo -> {
                                            if (fragmentInfo.getMz() == null) {
                                                log.info(pep.getLibraryId() + "-" + pep.getPeptideRef() + "?????????");
                                            }
                                        });
                    });
            log.info("?????????" + i + "/" + batch);
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
