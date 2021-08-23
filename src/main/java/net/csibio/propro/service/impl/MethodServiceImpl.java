package net.csibio.propro.service.impl;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.MethodDAO;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.query.MethodQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.MethodService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("methodService")
public class MethodServiceImpl implements MethodService {

    @Autowired
    MethodDAO methodDAO;

    @Override
    public BaseDAO<MethodDO, MethodQuery> getBaseDAO() {
        return methodDAO;
    }

    @Override
    public void beforeInsert(MethodDO methodDO) throws XException {
        if (StringUtils.isEmpty(methodDO.getName())) {
            throw new XException(ResultCode.METHOD_NAME_CANNOT_BE_NULL);
        }
        methodDO.setCreateDate(new Date());
        methodDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(MethodDO methodDO) throws XException {
        if (methodDO.getName() == null) {
            throw new XException(ResultCode.METHOD_NAME_CANNOT_BE_NULL);
        }
        methodDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //Do Nothing
    }
}
