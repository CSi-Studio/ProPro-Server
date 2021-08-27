package net.csibio.propro.service.impl;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.DictDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.query.DictQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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



    @Override
    public DictDO getById(String id) {
        try {
            return getBaseDAO().getById(id);
        } catch (Exception e) {
            return null;
        }
    }

    @Cacheable(cacheNames = "DictGetAll",key="#dictQuery.hashCode()")
    @Override
    public List<DictDO> getAll(DictQuery dictQuery) {
        return getBaseDAO().getAll(dictQuery);
    }

    @CacheEvict(cacheNames = "DictGetAll",allEntries=true)
    @Override
    public Result<DictDO> update(DictDO dictDO) {
        try {
            beforeUpdate(dictDO);
            getBaseDAO().update(dictDO);
            System.out.println("执行getall");
            return Result.OK(dictDO);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    @Override
    @CacheEvict(cacheNames = "DictGetAll",allEntries=true)
    public Result remove(DictQuery dictQuery) {
        return DictService.super.remove(dictQuery);
    }

    @Override
    @CacheEvict(cacheNames = "DictGetAll",allEntries=true)
    public Result<DictDO> insert(DictDO dictDO) {
        try {
            beforeInsert(dictDO);
            getBaseDAO().insert(dictDO);
            return Result.OK(dictDO);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }
}
