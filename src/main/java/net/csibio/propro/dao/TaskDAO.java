package net.csibio.propro.dao;

import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class TaskDAO extends BaseDAO<TaskDO, TaskQuery> {

    public static String CollectionName = "task";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class<TaskDO> getDomainClass() {
        return TaskDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(TaskQuery targetQuery) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(targetQuery.getId())) {
            query.addCriteria(where("id").is(targetQuery.getId()));
        }
        if (StringUtils.isNotEmpty(targetQuery.getExpId())) {
            query.addCriteria(where("expId").is(targetQuery.getExpId()));
        }
        if (StringUtils.isNotEmpty(targetQuery.getName())) {
            query.addCriteria(where("name").is(targetQuery.getName()));
        }
        if (StringUtils.isNotEmpty(targetQuery.getTaskTemplate())) {
            query.addCriteria(where("taskTemplate").is(targetQuery.getTaskTemplate()));
        }
        if (targetQuery.getStatusList() != null && targetQuery.getStatusList().size() != 0) {
            query.addCriteria(where("status").in(targetQuery.getStatusList()));
        }

        return query;
    }

}
