package net.csibio.propro.dao;

import net.csibio.propro.domain.db.ProteinDO;
import net.csibio.propro.domain.query.ProteinQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ProteinDAO extends BaseDAO<ProteinDO, ProteinQuery> {

    public static String CollectionName = "protein";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<ProteinDO> getDomainClass() {
        return ProteinDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(ProteinQuery proteinQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(proteinQuery.getId())) {
            query.addCriteria(where("id").is(proteinQuery.getId()));
        }
        if (StringUtils.isNotEmpty(proteinQuery.getGene())) {
            query.addCriteria(where("gene").is(proteinQuery.getGene()));
        }
        if (StringUtils.isNotEmpty(proteinQuery.getIdentifier())) {
            query.addCriteria(where("identifier").is(proteinQuery.getIdentifier()));
        }
        if (proteinQuery.getReviewed() != null) {
            query.addCriteria(where("reviewed").is(proteinQuery.getReviewed()));
        }
        if (StringUtils.isNotEmpty(proteinQuery.getOrganism())) {
            query.addCriteria(where("organism").regex(proteinQuery.getOrganism(), "i"));
        }
        return query;
    }

}
