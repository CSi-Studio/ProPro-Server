package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("dataService")
public class DataServiceImpl implements DataService {

    @Autowired
    DataDAO dataDAO;

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
//
//    @Override
//    public void batchUpdate(String overviewId, List<SimpleFeatureScores> sfsList, String projectId) {
//        if (sfsList.size() == 0) {
//            return;
//        }
//
//        sfsList.forEach(sfs -> {
//            boolean res = dataDAO.update(projectId, overviewId, sfs.getPeptideRef(), sfs.getDecoy(),
//                    sfs.getRt(), sfs.getIntensitySum(), sfs.getFragIntFeature(), sfs.getFdr(), sfs.getQValue());
//            if (!res) {
//                log.error("更新失败:" + sfs.getPeptideRef());
//            }
//        });
//    }

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
