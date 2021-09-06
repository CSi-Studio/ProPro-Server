package net.csibio.propro.service;

import net.csibio.propro.domain.bean.score.FinalPeakGroupScore;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataSumQuery;

import java.util.List;

public interface DataSumService extends BaseMultiService<DataSumDO, DataSumQuery> {

    void buildDataSumList(List<FinalPeakGroupScore> sfsList, Double fdr, String overviewId, String projectId);

    int countMatchedProteins(String overviewId, String projectId, Boolean needUnique, int hit);

    int countMatchedPeptide(String overviewId, String projectId, Boolean needUnique);
}
