package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.RunDataVO;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("dataService")
public class DataServiceImpl implements DataService {

    @Autowired
    DataDAO dataDAO;
    @Autowired
    DataSumService dataSumService;
    @Autowired
    Extractor extractor;
    @Autowired
    Scorer scorer;
    @Autowired
    PeptideService peptideService;
    @Autowired
    SimulateService simulateService;
    @Autowired
    RunService runService;
    @Autowired
    OverviewService overviewService;

    @Override
    public BaseMultiDAO<DataDO, DataQuery> getBaseDAO() {
        return dataDAO;
    }

    @Override
    public void beforeInsert(DataDO dataDO, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public void beforeUpdate(DataDO dataDO, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public void beforeRemove(String id, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public RunDataVO getDataFromDB(String projectId, String runId, String overviewId, String peptideRef) {
        RunDataVO dataVO = new RunDataVO(runId, overviewId, peptideRef);
        DataDO data = getOne(new DataQuery(overviewId).setPeptideRef(peptideRef).setDecoy(false), DataDO.class, projectId);
        if (data == null) {
            return null;
        }
        DataSumDO dataSum = dataSumService.getById(data.getId(), projectId);
        dataVO.merge(data, dataSum);
        return dataVO;
    }

    @Override
    public Result<RunDataVO> predictDataFromFile(RunDO run, PeptideDO peptide, Boolean changeCharge, String peakPickerMethod, String overviewId) {
        if (peptide == null) {
            return Result.Error(ResultCode.PEPTIDE_NOT_EXIST);
        }
        OverviewDO overview = overviewService.getById(overviewId);
        if (overview == null) {
            return Result.Error(ResultCode.OVERVIEW_NOT_EXISTED);
        }

        AnalyzeParams params = new AnalyzeParams(new MethodDO().init());
        params.setChangeCharge(changeCharge);
        params.setOverviewId(overviewId);
        params.getMethod().getPeakFinding().setPeakFindingMethod(peakPickerMethod);
        
        PeptideCoord coord = peptide.toTargetPeptide();
        if (changeCharge) {
            if (peptide.getCharge() == 2) {
                coord.setMz(coord.getMz() * 2 / 3);
                coord.setCharge(3);
                coord.setPeptideRef(coord.getPeptideRef().replace("2", "3"));
            } else {
                coord.setMz(coord.getMz() * 3 / 2);
                coord.setCharge(2);
                coord.setPeptideRef(coord.getPeptideRef().replace("3", "2"));
            }
        }

        Result<RunDataVO> result = extractor.predictOne(run, overview, coord, params);
        if (result.isFailed() && result.getErrorMessage().equals(ResultCode.BLOCK_INDEX_NOT_EXISTED.getCode())) {
            if (changeCharge) {
                if (peptide.getCharge() == 2) {
                    coord.setMz(coord.getMz() * 2);
                    coord.setCharge(1);
                    coord.setPeptideRef(coord.getPeptideRef().replace("2", "1"));
                } else {
                    coord.setMz(coord.getMz() * 3 / 4);
                    coord.setCharge(4);
                    coord.setPeptideRef(coord.getPeptideRef().replace("3", "4"));
                }
            }
            result = extractor.predictOne(run, overview, coord, params);
        }

        return result;
    }
}
