package net.csibio.propro.dao;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.query.DataQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class DataDAO extends BaseMultiDAO<DataDO, DataQuery> {

    public static String CollectionName = "data";

    @Override
    protected String getCollectionName(String projectId) {
        if (StringUtils.isNotEmpty(projectId)) {
            return CollectionName + "-" + projectId;
        } else {
            return CollectionName;
        }
    }

    @Override
    protected Class<DataDO> getDomainClass() {
        return DataDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(DataQuery dataQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(dataQuery.getId())) {
            query.addCriteria(where("id").is(dataQuery.getId()));
        }
        if (StringUtils.isNotEmpty(dataQuery.getOverviewId())) {
            query.addCriteria(where("overviewId").is(dataQuery.getOverviewId()));
        }
        if (StringUtils.isNotEmpty(dataQuery.getPeptideRef())) {
            query.addCriteria(where("peptideRef").is(dataQuery.getPeptideRef()));
        }
        if (StringUtils.isNotEmpty(dataQuery.getProtein())) {
            query.addCriteria(where("protein").is(dataQuery.getProtein()));
        }
        if (dataQuery.getDecoy() != null) {
            query.addCriteria(where("decoy").is(dataQuery.getDecoy()));
        }
        if (dataQuery.getMzStart() != null && dataQuery.getMzEnd() != null) {
            query.addCriteria(where("mz").gte(dataQuery.getMzStart()).lt(dataQuery.getMzEnd()));
        }
        if (dataQuery.getFdrStart() != null || dataQuery.getFdrEnd() != null) {
            query.addCriteria(where("fdr").gte(dataQuery.getFdrStart() == null ? 0 : dataQuery.getFdrStart()).lte(dataQuery.getFdrEnd() == null ? 1 : dataQuery.getFdrEnd()));
        }
        if (dataQuery.getQValueStart() != null || dataQuery.getQValueEnd() != null) {
            query.addCriteria(where("qValue").gte(dataQuery.getQValueStart() == null ? 0 : dataQuery.getQValueStart()).lte(dataQuery.getQValueEnd() == null ? 1 : dataQuery.getQValueEnd()));
        }
        if (dataQuery.getStatusList() != null) {
            query.addCriteria(where("status").in(dataQuery.getStatusList()));
        }
        return query;
    }

    public Result createDataCollectionAndIndex(String projectId) {
        try {
            mongoTemplate.createCollection(getCollectionName(projectId));
            buildIndex(DataDO.class, projectId);
            return Result.OK();
        } catch (Exception e) {
            e.printStackTrace();
            mongoTemplate.dropCollection(getCollectionName(projectId));
            return Result.Error(ResultCode.CREATE_COLLECTION_ERROR.getCode(), e.getMessage());
        }
    }

    public Result dropDataCollection(String projectId) {
        try {
            mongoTemplate.dropCollection(getCollectionName(projectId));
            return Result.OK();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.DROP_COLLECTION_ERROR.getCode(), e.getMessage());
        }
    }
}
