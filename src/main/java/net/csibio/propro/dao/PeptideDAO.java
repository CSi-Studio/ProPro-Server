package net.csibio.propro.dao;

import com.mongodb.BasicDBObject;
import net.csibio.propro.domain.bean.peptide.Protein;
import net.csibio.propro.domain.bean.peptide.SimplePeptide;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class PeptideDAO extends BaseDAO<PeptideDO, PeptideQuery> {

    public static String CollectionName = "peptide";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<PeptideDO> getDomainClass() {
        return PeptideDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(PeptideQuery peptideQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(peptideQuery.getId())) {
            query.addCriteria(where("id").is(peptideQuery.getId()));
        }
        if (peptideQuery.getIsUnique() != null) {
            query.addCriteria(where("isUnique").is(peptideQuery.getIsUnique()));
        }
        if (StringUtils.isNotEmpty(peptideQuery.getFullName())) {
            query.addCriteria(where("fullName").regex(peptideQuery.getFullName(), "i"));
        }
        if (StringUtils.isNotEmpty(peptideQuery.getLibraryId())) {
            query.addCriteria(where("libraryId").is(peptideQuery.getLibraryId()));
        }
        if (StringUtils.isNotEmpty(peptideQuery.getSequence())) {
            query.addCriteria(where("sequence").regex(peptideQuery.getSequence(), "i"));
        }
        if (StringUtils.isNotEmpty(peptideQuery.getPeptideRef())) {
            query.addCriteria(where("peptideRef").is(peptideQuery.getPeptideRef()));
        }
        if (StringUtils.isNotEmpty(peptideQuery.getProteinIdentifier())) {
            query.addCriteria(where("proteinIdentifier").is(peptideQuery.getProteinIdentifier()));
        }
        if (peptideQuery.getMzStart() != null) {
            query.addCriteria(where("mz").gte(peptideQuery.getMzStart()).lt(peptideQuery.getMzEnd()));
        }
        if (peptideQuery.getRtStart() != null) {
            query.addCriteria(where("rt").gte(peptideQuery.getRtStart()).lt(peptideQuery.getRtEnd()));
        }
        return query;
    }

    public List<PeptideDO> getAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        return mongoTemplate.find(query, PeptideDO.class, CollectionName);
    }

    public List<PeptideDO> getAllByLibraryIdAndProtein(String libraryId, String proteinIdentifier) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("proteinIdentifier").is(proteinIdentifier));
        return mongoTemplate.find(query, PeptideDO.class, CollectionName);
    }

    public PeptideDO getByLibraryIdAndPeptideRef(String libraryId, String peptideRef) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("peptideRef").is(peptideRef));
        return mongoTemplate.findOne(query, PeptideDO.class, CollectionName);
    }

    public SimplePeptide getTargetPeptideByDataRef(String libraryId, String peptideRef) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("peptideRef").is(peptideRef));
        return mongoTemplate.findOne(query, SimplePeptide.class, CollectionName);
    }

    public void deleteAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        mongoTemplate.remove(query, PeptideDO.class, CollectionName);
    }

    public List<Protein> getProteinList(PeptideQuery query) {
        AggregationResults<Protein> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Protein.class,
                        Aggregation.match(where("libraryId").is(query.getLibraryId())),
                        Aggregation.group("proteinIdentifier").
                                first("proteinIdentifier").as("identifier").
                                first("id").as("peptideId"),
                        Aggregation.skip((query.getCurrent() - 1) * query.getPageSize()),
                        Aggregation.limit(query.getPageSize())).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                Protein.class);
        return a.getMappedResults();
    }

    public void updateDecoyInfos(List<PeptideDO> peptideList) {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PeptideDO.class);
        for (PeptideDO peptide : peptideList) {
            Query query = new Query();
            query.addCriteria(Criteria.where("id").is(peptide.getId()));
            Update update = new Update();
            update.set("decoySequence", peptide.getDecoySequence());
            update.set("decoyUnimodMap", peptide.getDecoyUnimodMap());
            update.set("decoyFragments", peptide.getDecoyFragments());
            ops.updateOne(query, update);
        }
        ops.execute();
    }

    public long countByProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("proteinIdentifier").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public long countByUniqueProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.match(where("isUnique").is(true)),
                        Aggregation.group("proteinIdentifier").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public long countByPeptideRef(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("peptideRef").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);

        return a.getMappedResults().size();
    }
}
