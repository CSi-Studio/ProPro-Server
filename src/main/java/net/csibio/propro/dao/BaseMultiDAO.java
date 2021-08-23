package net.csibio.propro.dao;

import net.csibio.propro.domain.query.PageQuery;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public abstract class BaseMultiDAO<T, Q extends PageQuery> {

    @Autowired
    MongoTemplate mongoTemplate;

    protected abstract String getCollectionName(String routerId);

    protected abstract Class<T> getDomainClass();

    protected abstract boolean allowSort();

    protected abstract Query buildQueryWithoutPage(Q query);

    public T getById(String id, String routerId) {
        return mongoTemplate.findById(id, getDomainClass(), getCollectionName(routerId));
    }

    public T getOne(Q query, String routerId) {
        return (T) mongoTemplate.findOne(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    public boolean exists(Q query, String routerId) {
        return mongoTemplate.exists(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    @SuppressWarnings("Carefully Using!!!")
    public List<T> getAll(Q query, String routerId) {
        return mongoTemplate.find(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    public <K> List<K> getAll(Q query, Class<K> clazz, String routerId) {
        return mongoTemplate.find(buildQueryWithoutPage(query), clazz, getCollectionName(routerId));
    }

    public List<T> getList(Q query, String routerId) {
        return mongoTemplate.find(buildQuery(query), getDomainClass(), getCollectionName(routerId));
    }

    public <K> List<K> getList(Q query, Class<K> clazz, String routerId) {
        return mongoTemplate.find(buildQuery(query), clazz, getCollectionName(routerId));
    }

    public long estimatedCount(String routerId) {
        return mongoTemplate.estimatedCount(getCollectionName(routerId));
    }

    public long count(Q query, String routerId) {
        return mongoTemplate.count(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    public T insert(T t, String routerId) {
        mongoTemplate.insert(t, getCollectionName(routerId));
        return t;
    }

    public List<T> insert(List<T> list, String routerId) {
        mongoTemplate.insert(list, getCollectionName(routerId));
        return list;
    }

    public T update(T t, String routerId) {
        mongoTemplate.save(t, getCollectionName(routerId));
        return t;
    }

    public List<T> update(List<T> list, String routerId) {
        mongoTemplate.save(list, getCollectionName(routerId));
        return list;
    }

    public boolean updateFirst(HashMap<String, Object> queryMap, HashMap<String, Object> fieldMap, String routerId) {

        if (queryMap == null || queryMap.size() == 0 || fieldMap == null || fieldMap.size() == 0) {
            return false;
        }

        Query query = new Query();
        queryMap.forEach((key, value) -> query.addCriteria(Criteria.where(key).is(value)));
        Update update = new Update();
        fieldMap.forEach(update::set);
        return mongoTemplate.updateFirst(query, update, getCollectionName(routerId)).wasAcknowledged();
    }

    public boolean updateAll(HashMap<String, Object> queryMap, HashMap<String, Object> fieldMap, String routerId) {
        if (queryMap == null || queryMap.size() == 0 || fieldMap == null || fieldMap.size() == 0) {
            return false;
        }

        Query query = new Query();
        queryMap.forEach((key, value) -> query.addCriteria(Criteria.where(key).is(value)));
        Update update = new Update();
        fieldMap.forEach(update::set);
        return mongoTemplate.updateMulti(query, update, getCollectionName(routerId)).wasAcknowledged();
    }

    public void removeById(String id, String routerId) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, getDomainClass(), getCollectionName(routerId));
    }

    public void remove(Q q, String routerId) {
        mongoTemplate.remove(buildQueryWithoutPage(q), getDomainClass(), getCollectionName(routerId));
    }

    public void buildIndex(Class<T> clazz, String routerId) {
        String collectionName = getCollectionName(routerId);
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        String[] indexFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Indexed.class))
                .map(Field::getName)
                .toArray(String[]::new);
        for (String indexField : indexFields) {
            if (StringUtils.hasText(indexField)) {
                indexOps.ensureIndex(new Index(indexField, Sort.Direction.ASC));
            }
        }
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().equals(CompoundIndexes.class)) {
                CompoundIndexes indexes = clazz.getDeclaredAnnotation(CompoundIndexes.class);
                CompoundIndex[] indexesArray = indexes.value();
                if (indexesArray.length > 0) {
                    for (int i = 0; i < indexesArray.length; i++) {
                        CompoundIndex index = indexesArray[i];
                        Document document = Document.parse(index.def());
                        CompoundIndexDefinition definition = new CompoundIndexDefinition(document);
                        if (index.unique()) {
                            indexOps.ensureIndex(definition.unique());
                        } else {
                            indexOps.ensureIndex(definition);
                        }
                    }
                }
            }
        }
    }

    protected Query buildQuery(Q targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);
        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());
        if (allowSort()) {
            if (targetQuery.getSortColumn() != null && targetQuery.getOrderBy() != null) {
                query.with(Sort.by(targetQuery.getOrderBy(), targetQuery.getSortColumn()));
            }
        }
        return query;
    }
}
