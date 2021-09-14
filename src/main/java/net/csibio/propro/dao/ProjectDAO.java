package net.csibio.propro.dao;

import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.query.ProjectQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ProjectDAO extends BaseDAO<ProjectDO, ProjectQuery> {

    public static String CollectionName = "project";

    @Autowired
    DataDAO dataDAO;

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<ProjectDO> getDomainClass() {
        return ProjectDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(ProjectQuery query) {
        Query dbQuery = new Query();
        if (StringUtils.isNotEmpty(query.getId())) {
            dbQuery.addCriteria(where("id").is(query.getId()));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            dbQuery.addCriteria(where("name").regex(query.getName(), "i"));
        }
        if (StringUtils.isNotEmpty(query.getGroup())) {
            dbQuery.addCriteria(where("group").is(query.getName()));
        }
        if (StringUtils.isNotEmpty(query.getAlias())) {
            dbQuery.addCriteria(where("alias").regex(query.getAlias(), "i"));
        }
        if (StringUtils.isNotEmpty(query.getOwner())) {
            dbQuery.addCriteria(where("owner").is(query.getOwner()));
        }
        if (StringUtils.isNotEmpty(query.getType())) {
            dbQuery.addCriteria(where("type").is(query.getType()));
        }
        return dbQuery;
    }

    //先创建项目内容,再创建DataDO表及相关索引
    @Override
    public ProjectDO insert(ProjectDO project) {
        mongoTemplate.insert(project, getCollectionName());
        dataDAO.createDataCollectionAndIndex(project.getId());
        return project;
    }

    //先删除项目数据,再删除数据表
    @Override
    public void removeById(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, getDomainClass(), getCollectionName());
        dataDAO.dropDataCollection(id);
    }
}
