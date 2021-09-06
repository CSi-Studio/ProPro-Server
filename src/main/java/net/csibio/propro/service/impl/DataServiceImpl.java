package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.ExpDataVO;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import net.csibio.propro.utils.DataUtil;
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
    public ExpDataVO getData(String projectId, String expId, String overviewId, String peptideRef) {
        ExpDataVO dataVO = new ExpDataVO(expId, overviewId, peptideRef);
        DataDO data = getOne(new DataQuery(overviewId).setPeptideRef(peptideRef), DataDO.class, projectId);
        if (data != null) {
            DataUtil.decompress(data);
            dataVO.setRtArray(data.getRtArray());
            dataVO.setCutInfoMap(data.getCutInfoMap());
            dataVO.setIntMap(data.getIntMap());
            dataVO.setStatus(data.getStatus());
        } else {
            return dataVO;
        }
        DataSumDO dataSum = dataSumService.getById(data.getId(), projectId);
        if (dataSum != null) {
            dataVO.setFdr(dataSum.getFdr());
            dataVO.setQValue(dataSum.getQValue());
            dataVO.setStatus(dataSum.getStatus());
            dataVO.setSum(dataSum.getSum());
            dataVO.setFragIntFeature(dataSum.getFragIntFeature());
            dataVO.setRealRt(dataSum.getRealRt());
        }

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
