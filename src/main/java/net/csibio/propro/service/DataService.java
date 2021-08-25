package net.csibio.propro.service;

import net.csibio.propro.domain.bean.score.SimpleFeatureScores;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.query.DataQuery;

import java.util.List;

public interface DataService extends BaseMultiService<DataDO, DataQuery> {

    void removeUnusedData(String overviewId, List<SimpleFeatureScores> simpleFeatureScoresList, Double fdr, String projectId);

    /**
     * 更新同一个overview下的指定数据
     *
     * @param overviewId
     * @param featureScoresList
     * @param projectId
     */
    void batchUpdate(String overviewId, List<SimpleFeatureScores> featureScoresList, String projectId);

    int countIdentifiedProteins(String overviewId, String projectId);
}
