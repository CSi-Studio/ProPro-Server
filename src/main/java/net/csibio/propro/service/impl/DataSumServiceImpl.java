package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.dao.BaseMultiDAO;
import net.csibio.propro.dao.DataSumDAO;
import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataSumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("dataSumService")
public class DataSumServiceImpl implements DataSumService {

    @Autowired
    DataSumDAO dataSumDAO;

    @Override
    public BaseMultiDAO<DataSumDO, DataSumQuery> getBaseDAO() {
        return dataSumDAO;
    }

    @Override
    public void beforeInsert(DataSumDO dataSumDO, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public void beforeUpdate(DataSumDO dataSumDO, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public void beforeRemove(String id, String projectId) throws XException {
        //Do Nothing
    }

    @Override
    public void buildDataSumList(List<SimpleFeatureScores> sfsList, Double fdr, String overviewId, String projectId) {
        List<DataSumDO> sumList = new ArrayList<>();
        sfsList.forEach(sfs -> {
            DataSumDO sum = new DataSumDO();
            sum.setOverviewId(overviewId);
            sum.setId(sfs.getId());
            sum.setDecoy(sfs.getDecoy());
            sum.setFdr(sfs.getFdr());
            sum.setQValue(sfs.getQValue());
            sum.setFragIntFeature(sfs.getFragIntFeature());
            sum.setRealRt(sfs.getRt());
            sum.setPeptideRef(sfs.getPeptideRef());
            sum.setSum(sfs.getIntensitySum());
            if (!sfs.getDecoy()) {
                //投票策略
                if (sfs.getFdr() <= 0.01) {
                    sum.setStatus(IdentifyStatus.SUCCESS.getCode());
                } else {
                    sum.setStatus(IdentifyStatus.FAILED.getCode());
                }
            }
            sumList.add(sum);
        });
        insert(sumList, projectId);
    }
}
