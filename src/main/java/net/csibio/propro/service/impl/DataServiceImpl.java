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
        DataSumDO dataSum = dataSumService.getById(data.getId(), projectId);
        dataVO.merge(data, dataSum);
        return dataVO;
    }
}
