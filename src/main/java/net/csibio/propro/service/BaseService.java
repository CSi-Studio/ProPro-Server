package net.csibio.propro.service;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.query.PageQuery;
import net.csibio.propro.exceptions.XException;

import java.util.HashMap;
import java.util.List;

public interface BaseService<T, Q extends PageQuery> {

    default T getById(String id) {
        try {
            return getBaseDAO().getById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default boolean exist(Q q) {
        try {
            return getBaseDAO().exists(q);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default Result removeById(String id) {
        if (id == null || id.isEmpty()) {
            return Result.Error(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            beforeRemove(id);
            getBaseDAO().removeById(id);
            return Result.OK();
        } catch (Exception e) {
            return Result.Error(ResultCode.DELETE_ERROR);
        }
    }

    default Result remove(Q q) {
        try {
            getBaseDAO().remove(q);
            return Result.OK();
        } catch (Exception e) {
            return Result.Error(ResultCode.DELETE_ERROR);
        }
    }

    default Result<T> insert(T t) {
        try {
            beforeInsert(t);
            getBaseDAO().insert(t);
            return Result.OK(t);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default Result<List<T>> insert(List<T> tList) {
        try {
            for (T t : tList) {
                beforeInsert(t);
            }
            List<T> successInsertList = getBaseDAO().insert(tList);
            return Result.OK(successInsertList);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default Result<List<T>> update(List<T> tList) {
        try {
            for (T t : tList) {
                beforeUpdate(t);
            }
            getBaseDAO().update(tList);
            return Result.OK(tList);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    default Result<T> update(T t) {
        try {
            beforeUpdate(t);
            getBaseDAO().update(t);
            return Result.OK(t);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    default Result updateFirst(HashMap<String, Object> queryMap, HashMap<String, Object> fieldMap) {
        boolean res = getBaseDAO().updateFirst(queryMap, fieldMap);
        if (res) {
            return Result.OK();
        } else {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    default Result updateAll(HashMap<String, Object> queryMap, HashMap<String, Object> fieldMap) {
        boolean res = getBaseDAO().updateAll(queryMap, fieldMap);
        if (res) {
            return Result.OK();
        } else {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    default <K> K getOne(Q q, Class<K> clazz) {
        return getBaseDAO().getOne(q, clazz);
    }

    default Result<List<T>> getList(Q q) {
        List<T> tList = getBaseDAO().getList(q);
        long totalCount = 0;
        if (q.getEstimateCount()) {
            totalCount = getBaseDAO().estimatedCount();
        } else {
            totalCount = getBaseDAO().count(q);
        }

        Result<List<T>> result = new Result<>(true);
        result.setData(tList);
        result.setTotalNum(totalCount);
        result.setPageSize(q.getPageSize());
        return result;
    }

    default <K> Result<List<K>> getList(Q q, Class<K> clazz) {
        List<K> tList = getBaseDAO().getList(q, clazz);
        long totalCount = 0;
        if (q.getEstimateCount()) {
            totalCount = getBaseDAO().estimatedCount();
        } else {
            totalCount = getBaseDAO().count(q);
        }
        Result<List<K>> result = new Result<>(true);
        result.setData(tList);
        result.setTotalNum(totalCount);
        result.setPageSize(q.getPageSize());
        return result;
    }

    default List<T> getAll(Q q) {
        return getBaseDAO().getAll(q);
    }

    default <K> List<K> getAll(Q q, Class<K> clazz) {
        return getBaseDAO().getAll(q, clazz);
    }

    default long count(Q q) {
        return getBaseDAO().count(q);
    }

    default long estimatedCount() {
        return getBaseDAO().estimatedCount();
    }

    BaseDAO<T, Q> getBaseDAO();

    void beforeInsert(T t) throws XException;

    void beforeUpdate(T t) throws XException;

    void beforeRemove(String id) throws XException;
}
