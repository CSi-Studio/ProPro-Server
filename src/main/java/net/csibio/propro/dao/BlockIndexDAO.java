package net.csibio.propro.dao;

import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.query.BlockIndexQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class BlockIndexDAO extends BaseDAO<BlockIndexDO, BlockIndexQuery> {

    public static String CollectionName = "blockIndex";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<BlockIndexDO> getDomainClass() {
        return BlockIndexDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(BlockIndexQuery query) {
        Query dbQuery = new Query();
        if (StringUtils.isNotEmpty(query.getExpId())) {
            dbQuery.addCriteria(where("expId").is(query.getExpId()));
        }
        if (StringUtils.isNotEmpty(query.getId())) {
            dbQuery.addCriteria(where("id").is(query.getId()));
        }
        if (query.getLevel() != null) {
            dbQuery.addCriteria(where("level").is(query.getLevel()));
        }
        if (query.getMzStart() != null) {
            dbQuery.addCriteria(where("range.start").is(query.getMzStart()));
        }
        if (query.getMzEnd() != null) {
            dbQuery.addCriteria(where("range.end").is(query.getMzEnd()));
        }
        if (query.getMz() != null) {
            dbQuery.addCriteria(where("range.start").lte(query.getMz()));
            dbQuery.addCriteria(where("range.end").gte(query.getMz()));
        }
        return dbQuery;
    }
}
