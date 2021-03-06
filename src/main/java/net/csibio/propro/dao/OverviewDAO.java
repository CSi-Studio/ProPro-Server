package net.csibio.propro.dao;

import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.OverviewQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class OverviewDAO extends BaseDAO<OverviewDO, OverviewQuery> {

    public static String CollectionName = "overview";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<OverviewDO> getDomainClass() {
        return OverviewDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(OverviewQuery query) {
        Query dbQuery = new Query();
        if (StringUtils.isNotEmpty(query.getProjectId())) {
            dbQuery.addCriteria(where("projectId").is(query.getProjectId()));
        }
        if (StringUtils.isNotEmpty(query.getId())) {
            dbQuery.addCriteria(where("id").is(query.getId()));
        } else if (query.getIds() != null && query.getIds().size() > 0) {
            dbQuery.addCriteria(where("id").in(query.getIds()));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            dbQuery.addCriteria(where("name").regex(query.getName(), "i"));
        }
        if (StringUtils.isNotEmpty(query.getInsLibId())) {
            dbQuery.addCriteria(where("insLibId").is(query.getInsLibId()));
        }
        if (StringUtils.isNotEmpty(query.getAnaLibId())) {
            dbQuery.addCriteria(where("anaLibId").is(query.getAnaLibId()));
        }
        if (StringUtils.isNotEmpty(query.getMethodId())) {
            dbQuery.addCriteria(where("methodId").lte(query.getMethodId()));
        }
        if (query.getDefaultOne() != null) {
            dbQuery.addCriteria(where("defaultOne").is(query.getDefaultOne()));
        }
        if (StringUtils.isNotEmpty(query.getRunId())) {
            dbQuery.addCriteria(where("runId").is(query.getRunId()));
        } else if (query.getRunIds() != null && query.getRunIds().size() > 0) {
            dbQuery.addCriteria(where("runId").in(query.getRunIds()));
        }
        return dbQuery;
    }
}
