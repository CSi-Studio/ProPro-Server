package net.csibio.propro.service.impl;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.OverviewDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.query.OverviewQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.OverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("overviewService")
public class OverviewServiceImpl implements OverviewService {

    @Autowired
    OverviewDAO overviewDAO;
    @Autowired
    DataService dataService;

    @Override
    public BaseDAO<OverviewDO, OverviewQuery> getBaseDAO() {
        return overviewDAO;
    }

    @Override
    public void beforeInsert(OverviewDO overviewDO) throws XException {
        if (overviewDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        if (overviewDO.getName() == null) {
            throw new XException(ResultCode.OVERVIEW_NAME_CAN_NOT_BE_EMPTY);
        }
        overviewDO.setCreateDate(new Date());
        overviewDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(OverviewDO overviewDO) throws XException {
        if (overviewDO.getId() == null) {
            throw new XException(ResultCode.OVERVIEW_ID_CAN_NOT_BE_EMPTY);
        }
        if (overviewDO.getProjectId() == null) {
            throw new XException(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        if (overviewDO.getName() == null) {
            throw new XException(ResultCode.OVERVIEW_NAME_CAN_NOT_BE_EMPTY);
        }
        overviewDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        if (id == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        OverviewDO overview = getById(id);
        if (overview == null) {
            throw new XException(ResultCode.OVERVIEW_NOT_EXISTED);
        }

        dataService.remove(new DataQuery().setOverviewId(id), overview.getProjectId());
    }

    @Override
    public Result remove(OverviewQuery query) {
        //开始执行overview批量删除逻辑
        List<IdName> idNameList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        idNameList.forEach(idName -> {
            Result res = removeById(idName.id());
            if (res.isFailed()) {
                errorList.add("Remove Overview Failed:" + res.getErrorMessage());
            }
        });
        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        }
        return Result.OK();
    }
}
