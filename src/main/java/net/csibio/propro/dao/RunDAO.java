package net.csibio.propro.dao;

import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.query.RunQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class RunDAO extends BaseDAO<RunDO, RunQuery> {

    public static String CollectionName = "run";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<RunDO> getDomainClass() {
        return RunDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(RunQuery query) {
        Query dbQuery = new Query();
        if (StringUtils.isNotEmpty(query.getId())) {
            dbQuery.addCriteria(where("id").is(query.getId()));
        } else if (query.getIds() != null && query.getIds().size() > 0) {
            dbQuery.addCriteria(where("id").in(query.getIds()));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            dbQuery.addCriteria(where("name").regex(query.getName(), "i"));
        }
        if (StringUtils.isNotEmpty(query.getLabel())) {
            dbQuery.addCriteria(where("label").is(query.getName()));
        }
        if (StringUtils.isNotEmpty(query.getProjectId())) {
            dbQuery.addCriteria(where("projectId").is(query.getProjectId()));
        }
        if (StringUtils.isNotEmpty(query.getType())) {
            dbQuery.addCriteria(where("type").is(query.getType()));
        }
        if (StringUtils.isNotEmpty(query.getProjectName())) {
            dbQuery.addCriteria(where("projectName").is(query.getProjectName()));
        }
        return dbQuery;
    }
}
