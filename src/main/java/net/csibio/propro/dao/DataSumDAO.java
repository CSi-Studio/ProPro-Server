package net.csibio.propro.dao;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataSumQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class DataSumDAO extends BaseMultiDAO<DataSumDO, DataSumQuery> {

    public static String CollectionName = "dataSum";

    @Override
    protected String getCollectionName(String projectId) {
        if (StringUtils.isNotEmpty(projectId)) {
            return CollectionName + "-" + projectId;
        } else {
            return CollectionName;
        }
    }

    @Override
    protected Class<DataSumDO> getDomainClass() {
        return DataSumDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(DataSumQuery dataSumQuery) {
        Query query = new Query();
        if (dataSumQuery.getIds() != null && dataSumQuery.getIds().size() > 0) {
            query.addCriteria(where("id").in(dataSumQuery.getIds()));
        } else if (StringUtils.isNotEmpty(dataSumQuery.getId())) {
            query.addCriteria(where("id").is(dataSumQuery.getId()));
        }
        if (StringUtils.isNotEmpty(dataSumQuery.getOverviewId())) {
            query.addCriteria(where("overviewId").is(dataSumQuery.getOverviewId()));
        }
        if (StringUtils.isNotEmpty(dataSumQuery.getPeptideRef())) {
            query.addCriteria(where("peptideRef").is(dataSumQuery.getPeptideRef()));
        }
        if (dataSumQuery.getDecoy() != null) {
            query.addCriteria(where("decoy").is(dataSumQuery.getDecoy()));
        }
        if (dataSumQuery.getIsUnique() != null && dataSumQuery.getIsUnique()) {
            query.addCriteria(where("proteins.1").exists(false));
        }
        if (StringUtils.isNotEmpty(dataSumQuery.getProteins())) {
            query.addCriteria(where("proteins").is(dataSumQuery.getProteins()));
        }
//        if (dataQuery.getMzStart() != null && dataQuery.getMzEnd() != null) {
//            query.addCriteria(where("mz").gte(dataQuery.getMzStart()).lt(dataQuery.getMzEnd()));
//        }
        if (dataSumQuery.getFdrStart() != null || dataSumQuery.getFdrEnd() != null) {
            query.addCriteria(where("fdr").gte(dataSumQuery.getFdrStart() == null ? 0 : dataSumQuery.getFdrStart()).lte(dataSumQuery.getFdrEnd() == null ? 1 : dataSumQuery.getFdrEnd()));
        }
//        if (dataSumQuery.getQValueStart() != null || dataSumQuery.getQValueEnd() != null) {
//            query.addCriteria(where("qValue").gte(dataSumQuery.getQValueStart() == null ? 0 : dataSumQuery.getQValueStart()).lte(dataSumQuery.getQValueEnd() == null ? 1 : dataSumQuery.getQValueEnd()));
//        }

        if (dataSumQuery.getStatus() != null) {
            query.addCriteria(where("status").is(dataSumQuery.getStatus()));
        } else if (dataSumQuery.getStatusList() != null) {
            query.addCriteria(where("status").in(dataSumQuery.getStatusList()));
        }
        return query;
    }

    public Result createDataCollectionAndIndex(String projectId) {
        try {
            mongoTemplate.createCollection(getCollectionName(projectId));
            buildIndex(DataSumDO.class, projectId);
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
