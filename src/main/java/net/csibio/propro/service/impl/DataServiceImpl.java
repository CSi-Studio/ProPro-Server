package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.MzIntensityPairs;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.TreeMap;

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
    public ExpDataVO fetchEicByPeptideRef(ExperimentDO exp, PeptideCoord coord, AnalyzeParams params) {

        Result<TreeMap<Float, MzIntensityPairs>> rtMapResult = extractor.getRtMap(exp, coord);
        if (rtMapResult.isFailed()) {
            return null;
        }
        TreeMap<Float, MzIntensityPairs> rtMap = rtMapResult.getData();

        Double targetRt = exp.getIrt().getSi().realRt(coord.getRt());
        coord.setRtStart(targetRt - params.getMethod().getEic().getRtWindow());
        coord.setRtEnd(targetRt + params.getMethod().getEic().getRtWindow());
        DataDO dataDO = extractor.extractOne(coord, rtMap, params);
        if (dataDO == null) {
            return null;
        }

        //Step2. 常规选峰及打分,未满足条件的直接忽略
        scorer.scoreForOne(exp, dataDO, coord, rtMap, params);
        if (dataDO.getFeatureScoresList() == null) {
            return null;
        }
        
        DataSumDO dataSum = dataSumService.getOne(new DataSumQuery(params.getOverviewId()).setPeptideRef(coord.getPeptideRef()), DataSumDO.class, exp.getProjectId());
        DataDO realData = dataDO;
        ExpDataVO dataVO = new ExpDataVO();
        dataVO.setPeptideRef(realData.getPeptideRef());
        dataVO.setExpId(exp.getId());
        dataVO.setCutInfoList(realData.getCutInfos());
        dataVO.setRtArray(realData.getRtArray());
        dataVO.setIntensityMap(realData.getIntensityMap());

        return dataVO;
    }

//    @Override
//    public int countIdentifiedProteins(String overviewId, String projectId) {
//        DataQuery query = new DataQuery();
//        query.setOverviewId(overviewId);
//        query.setDecoy(false);
//        List<Integer> status = new ArrayList<>();
//        status.add(IdentifyStatus.SUCCESS.getCode());
////        query.setStatusList(status);
//        List<ProteinPeptide> ppList = dataDAO.getAll(query, ProteinPeptide.class, projectId);
//        HashSet<String> proteins = new HashSet<>();
//        for (ProteinPeptide pp : ppList) {
//            if (pp.getIsUnique() && (!pp.getIsUnique() || !pp.getProtein().startsWith("1/"))) {
//                continue;
//            } else {
//                proteins.add(pp.getProtein());
//            }
//        }
//        return proteins.size();
//    }
}
