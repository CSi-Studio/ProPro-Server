package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.*;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
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
    ExperimentService experimentService;

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
    public ExpDataVO getDataFromDB(String projectId, String expId, String overviewId, String peptideRef) {
        ExpDataVO dataVO = new ExpDataVO(expId, overviewId, peptideRef);
        DataDO data = getOne(new DataQuery(overviewId).setPeptideRef(peptideRef).setDecoy(false), DataDO.class, projectId);
        if (data == null) {
            return null;
        }
        DataSumDO dataSum = dataSumService.getById(data.getId(), projectId);
        dataVO.merge(data, dataSum);
        return dataVO;
    }

    @Override
    public Result<ExpDataVO> predictDataFromFile(ExperimentDO exp, String libraryId, String originalPeptide, Boolean changeCharge, String overviewId) {
        PeptideDO brother = peptideService.getOne(new PeptideQuery().setLibraryId(libraryId).setPeptideRef(originalPeptide), PeptideDO.class);
        if (brother == null) {
            return Result.Error(ResultCode.PEPTIDE_NOT_EXIST);
        }
        PeptideCoord coord = brother.toTargetPeptide();
        if (changeCharge) {
            if (brother.getCharge() == 2) {
                coord.setMz(coord.getMz() * 2 / 3);
            } else {
                coord.setMz(coord.getMz() * 3 / 4);
            }
        }
        coord.setMz(coord.getMz() * 3 / 4);
        AnalyzeParams params = new AnalyzeParams(new MethodDO().init());
        params.setOverviewId(overviewId);
        params.setPredict(true);
        Result<ExpDataVO> result = extractor.eppsPredictOne(exp, coord, params);
        return result;
    }
}
