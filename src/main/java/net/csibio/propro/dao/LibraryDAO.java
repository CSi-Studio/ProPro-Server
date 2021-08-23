package net.csibio.propro.dao;

import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.query.LibraryQuery;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class LibraryDAO extends BaseDAO<LibraryDO, LibraryQuery> {

    public static String CollectionName = "library";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<LibraryDO> getDomainClass() {
        return LibraryDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(LibraryQuery libraryQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(libraryQuery.getId())) {
            query.addCriteria(where("id").is(libraryQuery.getId()));
        }
        if (StringUtils.isNotEmpty(libraryQuery.getName())) {
            query.addCriteria(where("name").regex(libraryQuery.getName(), "i"));
        }
        if (StringUtils.isNotEmpty(libraryQuery.getType())) {
            query.addCriteria(where("type").is(libraryQuery.getType()));
        }
        return query;
    }

    public List<LibraryDO> getSimpleAll(Integer type) {
        Document queryDoc = new Document();
        if (type != null) {
            queryDoc.put("type", type);
        }
        Document fieldsDoc = new Document();
        fieldsDoc.put("id", true);
        fieldsDoc.put("name", true);
        fieldsDoc.put("filePath", true);
        Query query = new BasicQuery(queryDoc, fieldsDoc);
        return mongoTemplate.find(query, LibraryDO.class, CollectionName);
    }

    public List<LibraryDO> getPublicSimpleAll(Integer type) {
        Document queryDoc = new Document();
        if (type != null) {
            queryDoc.put("type", type);
        }

        Document fieldsDoc = new Document();
        fieldsDoc.put("id", true);
        fieldsDoc.put("name", true);
        Query query = new BasicQuery(queryDoc, fieldsDoc);
        return mongoTemplate.find(query, LibraryDO.class, CollectionName);
    }

    public LibraryDO getByName(String name) {
        LibraryQuery query = new LibraryQuery();
        query.setName(name);
        return mongoTemplate.findOne(buildQuery(query), LibraryDO.class, CollectionName);
    }

}
