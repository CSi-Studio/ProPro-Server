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
    public void removeUnuseData(String overviewId, List<SimpleFeatureScores> simpleFeatureScoresList, Double fdr, String projectId) {
        List<SimpleFeatureScores> dataNeedToRemove = new ArrayList<>();
        for (int i = simpleFeatureScoresList.size() - 1; i >= 0; i--) {
            //如果fdr为空或者fdr小于指定的值,那么删除它
            if (simpleFeatureScoresList.get(i).getFdr() == null || simpleFeatureScoresList.get(i).getFdr() > fdr) {
                dataNeedToRemove.add(simpleFeatureScoresList.get(i));
                simpleFeatureScoresList.remove(i);
            }
        }

        long start = System.currentTimeMillis();
        if (dataNeedToRemove.size() != 0) {
            dataDAO.batchRemove(overviewId, dataNeedToRemove, projectId);
        }
        log.info("删除无用数据:" + dataNeedToRemove.size() + "条,总计耗时:" + (System.currentTimeMillis() - start) + "毫秒");
    }

    @Override
    public void batchRemove(String overviewId, List<SimpleFeatureScores> featureScoresList, String projectId) {
        dataDAO.batchRemove(overviewId, featureScoresList, projectId);
    }

    @Override
    public void batchUpdate(String overviewId, List<SimpleFeatureScores> featureScoresList, String projectId) {
        dataDAO.batchUpdate(overviewId, featureScoresList, projectId);
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
            if (pp.getIsUnique() && (!pp.getIsUnique() || !pp.getProteinName().startsWith("1/"))) {
                continue;
            } else {
                proteins.add(pp.getProteinName());
            }
        }
        return proteins.size();
    }
}
