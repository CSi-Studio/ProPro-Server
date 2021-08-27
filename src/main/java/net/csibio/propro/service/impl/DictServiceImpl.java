package net.csibio.propro.service.impl;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.DictDAO;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.query.DictQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("dictService")
public class DictServiceImpl implements DictService {

    @Autowired
    DictDAO dictDAO;

    @Override
    public BaseDAO<DictDO, DictQuery> getBaseDAO() {
        return dictDAO;
    }

    @Override
    public void beforeInsert(DictDO dictDO) throws XException {
        if (dictDO.getName() == null) {
            throw new XException(ResultCode.DATA_ID_CANNOT_BE_EMPTY);
        }
    }

    @Override
    public void beforeUpdate(DictDO dictDO) throws XException {
        if (dictDO.getName() == null) {
            throw new XException(ResultCode.DATA_ID_CANNOT_BE_EMPTY);
        }
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //Do Nothing
    }


    @Cacheable(cacheNames = "DictGetId",key = "#id")
    @Override
    public DictDO getById(String id) {
        try {
            return getBaseDAO().getById(id);
        } catch (Exception e) {
            return null;
        }
    }

    @Cacheable(cacheNames = "DictGetAll",key = "#dictQuery.hashCode()")
    @Override
    public List<DictDO> getAll(DictQuery dictQuery) {
        return getBaseDAO().getAll(dictQuery);
    }
}
