package net.csibio.propro.dao;

import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.query.DictQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class DictDAO extends BaseDAO<DictDO, DictQuery> {

    public static String CollectionName = "dict";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<DictDO> getDomainClass() {
        return DictDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(DictQuery dictQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(dictQuery.getId())) {
            query.addCriteria(where("id").is(dictQuery.getId()));
        }
        if (StringUtils.isNotEmpty(dictQuery.getName())) {
            query.addCriteria(where("name").regex(dictQuery.getName()));
        }
        return query;
    }
}
