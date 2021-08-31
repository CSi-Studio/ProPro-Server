package net.csibio.propro.service;

import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataSumQuery;

import java.util.List;

public interface DataSumService extends BaseMultiService<DataSumDO, DataSumQuery> {

    void buildDataSumList(List<SimpleFeatureScores> sfsList, Double fdr, String overviewId, String projectId);
}
