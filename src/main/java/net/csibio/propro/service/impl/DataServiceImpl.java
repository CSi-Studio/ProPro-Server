package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataDAO;
import net.csibio.propro.domain.bean.peptide.ProteinPeptide;
import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    @Override
    public void removeUnusedData(String overviewId, List<SimpleFeatureScores> simpleFeatureScoresList, Double fdr, String projectId) {
        List<SimpleFeatureScores> dataNeedToRemove = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = simpleFeatureScoresList.size() - 1; i >= 0; i--) {
            //如果fdr为空或者fdr小于指定的值,那么删除它
            if (simpleFeatureScoresList.get(i).getFdr() == null || simpleFeatureScoresList.get(i).getFdr() > fdr) {
                dataNeedToRemove.add(simpleFeatureScoresList.get(i));
                simpleFeatureScoresList.remove(i);
            }
        }


//        if (dataNeedToRemove.size() != 0) {
//            log.info("总计需要删除" + dataNeedToRemove.size() + "条数据");
//            dataNeedToRemove.forEach(sfs -> {
//                dataDAO.remove(new DataQuery().setOverviewId(overviewId).setPeptideRef(sfs.getPeptideRef()).setDecoy(sfs.getDecoy()), projectId);
//            });
//        }
        log.info("删除无用数据:" + dataNeedToRemove.size() + "条,总计耗时:" + (System.currentTimeMillis() - start) + "毫秒");
    }

    @Override
    public void batchUpdate(String overviewId, List<SimpleFeatureScores> sfsList, String projectId) {
        if (sfsList.size() == 0) {
            return;
        }

        sfsList.forEach(sfs -> {
            boolean res = dataDAO.update(projectId, overviewId, sfs.getPeptideRef(), sfs.getDecoy(),
                    sfs.getRt(), sfs.getIntensitySum(), sfs.getFragIntFeature(), sfs.getFdr(), sfs.getQValue());
            if (!res) {
                log.error("更新失败:" + sfs.getPeptideRef());
            }
        });
    }

    @Override
    public int countIdentifiedProteins(String overviewId, String projectId) {
        DataQuery query = new DataQuery();
        query.setOverviewId(overviewId);
        query.setDecoy(false);
        List<Integer> status = new ArrayList<>();
        status.add(IdentifyStatus.SUCCESS.getCode());
        query.setStatusList(status);
        List<ProteinPeptide> ppList = dataDAO.getAll(query, ProteinPeptide.class, projectId);
        HashSet<String> proteins = new HashSet<>();
        for (ProteinPeptide pp : ppList) {
            if (pp.getIsUnique() && (!pp.getIsUnique() || !pp.getProteinIdentifier().startsWith("1/"))) {
                continue;
            } else {
                proteins.add(pp.getProteinIdentifier());
            }
        }
        return proteins.size();
    }
}
