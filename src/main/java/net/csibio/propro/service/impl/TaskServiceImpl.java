package net.csibio.propro.service.impl;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.TaskDAO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.TaskQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
@Service("taskService")
public class TaskServiceImpl implements TaskService {

    public final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    TaskDAO taskDAO;


    @Override
    public BaseDAO<TaskDO, TaskQuery> getBaseDAO() {
        return taskDAO;
    }

    @Override
    public void beforeInsert(TaskDO taskDO) throws XException {
        if (taskDO.getTaskTemplate() == null) {
            throw new XException(ResultCode.TASK_TEMPLATE_NOT_EXISTED);
        }
        if (taskDO.getName() == null) {
            throw new XException(ResultCode.TASK_NAME_CAN_NOT_BE_NULL);
        }
        taskDO.setCreateDate(new Date());
        taskDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(TaskDO taskDO) throws XException {
        if (taskDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (taskDO.getTaskTemplate() == null) {
            throw new XException(ResultCode.TASK_TEMPLATE_NOT_EXISTED);
        }
        if (taskDO.getName() == null) {
            throw new XException(ResultCode.TASK_NAME_CAN_NOT_BE_NULL);
        }
        taskDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String libraryId) throws XException {
        //Do Nothing
    }
}
