package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.OverviewDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.peptide.ProteinPeptide;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.*;
import net.csibio.propro.excel.peptide.PeptideRow;
import net.csibio.propro.excel.peptide.PeptideSumStatus;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service("overviewService")
public class OverviewServiceImpl implements OverviewService {

    @Autowired
    OverviewDAO overviewDAO;
    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;

    @Override
    public BaseDAO<OverviewDO, OverviewQuery> getBaseDAO() {
        return overviewDAO;
    }

    @Override
    public void beforeInsert(OverviewDO overviewDO) throws XException {
        if (overviewDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        if (overviewDO.getName() == null) {
            throw new XException(ResultCode.OVERVIEW_NAME_CAN_NOT_BE_EMPTY);
        }
        overviewDO.setCreateDate(new Date());
        overviewDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(OverviewDO overviewDO) throws XException {
        if (overviewDO.getId() == null) {
            throw new XException(ResultCode.OVERVIEW_ID_CAN_NOT_BE_EMPTY);
        }
        if (overviewDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        if (overviewDO.getName() == null) {
            throw new XException(ResultCode.OVERVIEW_NAME_CAN_NOT_BE_EMPTY);
        }
        overviewDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        if (id == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        OverviewDO overview = getById(id);
        if (overview == null) {
            throw new XException(ResultCode.OVERVIEW_NOT_EXISTED);
        }

        dataService.remove(new DataQuery().setOverviewId(id), overview.getProjectId());
        dataSumService.remove(new DataSumQuery().setOverviewId(id), overview.getProjectId());
    }

    @Override
    public Result remove(OverviewQuery query) {
        //开始执行overview批量删除逻辑
        List<IdName> idNameList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        idNameList.forEach(idName -> {
            Result res = removeById(idName.id());
            if (res.isFailed()) {
                errorList.add("Remove Overview Failed:" + res.getErrorMessage());
            }
        });
        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        }
        return Result.OK();
    }

    @Override
    public Map<String, OverviewDO> getDefaultOverviews(List<String> expIds) {
        Map<String, OverviewDO> overviewMap = new HashMap<>();
        if (expIds != null && expIds.size() > 0) {
            expIds.forEach(expId -> {
                OverviewDO overview = getOne(new OverviewQuery().setExpId(expId).setDefaultOne(true), OverviewDO.class);
                overviewMap.put(expId, overview);
            });
        }

        return overviewMap;
    }

    @Override
    public Result resetDefaultOne(String expId) {
        HashMap<String, Object> query = new HashMap<>();
        query.put("expId", expId);
        HashMap<String, Object> field = new HashMap<>();
        field.put("defaultOne", false);
        return updateAll(query, field);
    }

    @Override
    public Result statistic(OverviewDO overview) {
        int matchedUniqueProteinsCount = dataSumService.countMatchedProteins(overview.getId(), overview.getProjectId(), true, 1);
        int matchedTotalProteinsCount = dataSumService.countMatchedProteins(overview.getId(), overview.getProjectId(), false, 1);
        int matchedUniquePeptidesCount = dataSumService.countMatchedPeptide(overview.getId(), overview.getProjectId(), true);
        int matchedTotalPeptidesCount = dataSumService.countMatchedPeptide(overview.getId(), overview.getProjectId(), false);
        log.info("最终鉴定蛋白数目(Unique)为:" + matchedUniqueProteinsCount);
        log.info("最终鉴定蛋白数目(含非Unique)为:" + matchedTotalProteinsCount);
        log.info("最终鉴定肽段数目(Unique)为:" + matchedUniquePeptidesCount);
        log.info("最终鉴定肽段数目(含非Unique)为:" + matchedTotalPeptidesCount);

        overview.getStatistic().put(StatConst.MATCHED_UNIQUE_PROTEIN_COUNT, matchedUniqueProteinsCount);
        overview.getStatistic().put(StatConst.MATCHED_TOTAL_PROTEIN_COUNT, matchedTotalProteinsCount);
        overview.getStatistic().put(StatConst.MATCHED_UNIQUE_PEPTIDE_COUNT, matchedUniquePeptidesCount);
        overview.getStatistic().put(StatConst.MATCHED_TOTAL_PEPTIDE_COUNT, matchedTotalPeptidesCount);
        return update(overview);
    }

    @Override
    public Result<List<PeptideRow>> report(String projectId) {
        List<IdName> idNameList = experimentService.getAll(new ExperimentQuery().setProjectId(projectId), IdName.class);
        return report(idNameList.stream().map(IdName::id).toList());
    }

    @Override
    public Result<List<PeptideRow>> report(List<String> expIds) {
        if (expIds.size() == 0) {
            return Result.Error(ResultCode.EXPERIMENT_ID_CANNOT_BE_EMPTY);
        }
        Map<String, OverviewDO> overviewMap = getDefaultOverviews(expIds);
        if (overviewMap.size() != expIds.size()) {
            return Result.Error(ResultCode.SOME_EXPERIMENT_HAVE_NO_DEFAULT_OVERVIEW);
        }
        if (overviewMap.values().stream().map(OverviewDO::getInsLibId).collect(Collectors.toSet()).size() > 1) {
            return Result.Error(ResultCode.OVERVIEWS_MUST_USE_THE_SAME_INS_LIBRARY);
        }
        if (overviewMap.values().stream().map(OverviewDO::getAnaLibId).collect(Collectors.toSet()).size() > 1) {
            return Result.Error(ResultCode.OVERVIEWS_MUST_USE_THE_SAME_ANA_LIBRARY);
        }

        List<OverviewDO> overviews = new ArrayList<>(overviewMap.values());
        String anaLibId = overviews.get(0).getAnaLibId();
        List<ProteinPeptide> ppList = peptideService.getAll(new PeptideQuery(anaLibId), ProteinPeptide.class);
        List<PeptideRow> rowList = new ArrayList<PeptideRow>();
        for (int i = 0; i < ppList.size(); i++) {
            PeptideRow row = new PeptideRow();
            row.setPeptide(ppList.get(i).peptideRef());
            row.setProteins(ppList.get(i).proteins());
            rowList.add(row);
        }

        for (int i = 0; i < overviews.size(); i++) {
            log.info("开始构建第" + (i + 1) + "个overview,一共有" + overviews.size() + "个需要处理");
            OverviewDO overview = overviews.get(i);
            List<PeptideSumStatus> sumList = dataSumService.getAll(new DataSumQuery(overview.getId()).setDecoy(false), PeptideSumStatus.class, overview.getProjectId());
            Map<String, PeptideSumStatus> peptideMap = new HashMap<>();
            if (sumList != null) {
                peptideMap = sumList.stream().collect(Collectors.toMap(PeptideSumStatus::peptideRef, Function.identity()));
            }
            Map<String, PeptideSumStatus> finalPeptideMap = peptideMap;
            rowList.forEach(row -> {
                String peptideRef = row.getPeptide();
                PeptideSumStatus sumStatus = finalPeptideMap.get(peptideRef);
                if (sumStatus != null) {
                    row.getSumList().add(sumStatus.sum());
                    row.getStatusList().add(sumStatus.status());
                } else {
                    row.getSumList().add(0d);
                    row.getStatusList().add(IdentifyStatus.NO_EIC_FIND.getCode());
                }
            });
        }
        rowList = rowList.stream().sorted(Comparator.comparing(row -> row.getProteins().get(0))).toList();
        return Result.OK(rowList);
    }
}
