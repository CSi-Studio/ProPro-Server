package net.csibio.propro.dao;

import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.query.MethodQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class MethodDAO extends BaseDAO<MethodDO, MethodQuery> {

    public static String CollectionName = "method";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<MethodDO> getDomainClass() {
        return MethodDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(MethodQuery query) {
        Query dbQuery = new Query();
        if (StringUtils.isNotEmpty(query.getId())) {
            dbQuery.addCriteria(where("id").is(query.getId()));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            dbQuery.addCriteria(where("name").is(query.getName()));
        }
        return dbQuery;
    }
}
