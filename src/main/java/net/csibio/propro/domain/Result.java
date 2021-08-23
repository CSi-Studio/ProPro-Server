package net.csibio.propro.domain;

import io.swagger.annotations.ApiModel;
import net.csibio.propro.constants.enums.ResultCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
@ApiModel(description = "Return Value")
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1738821497566027418L;

    /**
     * 是否执行成功
     */
    private boolean success = false;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误提示信息
     */
    private String errorMessage;

    /**
     * Http 返回状态
     */
    private int status;

    /**
     * 错误信息列表
     */
    private List<String> errorList;

    /**
     * 单值返回,泛型
     */
    private T data;

    /**
     * 备用存储字段,用于扩展多个返回类型的情况
     */
    private HashMap featureMap;

    public Pagination pagination = new Pagination();

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public long getTotalNum() {
        return pagination.getTotal();
    }

    public void setTotalNum(long totalNum) {
        pagination.setTotal(totalNum);
    }

    public int getPageSize() {
        return pagination.getPageSize();
    }

    public void setPageSize(int pageSize) {
        pagination.setPageSize(pageSize);
    }

    public long getCurrent() {
        return pagination.getCurrent();
    }

    public void setCurrent(long current) {
        pagination.setCurrent(current);
    }

    public Result() {
    }

    public Result(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public T getData() {
        return data;
    }

    public void setFeatureMap(HashMap featureMap) {
        this.featureMap = featureMap;
    }

    public HashMap getFeatureMap() {
        if (featureMap == null) {
            featureMap = new HashMap();
        }
        return featureMap;
    }

    public void put(String key, Object value) {
        getFeatureMap().put(key, value);
    }

    public Object get(String key) {
        return getFeatureMap().get(key);
    }

    public Result<T> setData(T value) {
        this.data = value;
        return this;
    }

    public static Result OK() {
        Result r = new Result();
        r.setSuccess(true);
        return r;
    }

    public static <T> Result<T> OK(T model) {
        Result<T> r = new Result<T>();
        r.setSuccess(true);
        r.setData(model);
        return r;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Result setResultCode(ResultCode code) {
        this.errorCode = code.getCode();
        this.errorMessage = code.getMessage();
        return this;
    }

    public static Result Error(String msg) {
        Result result = new Result();
        result.setSuccess(false);
        result.setErrorCode(ResultCode.EXCEPTION.getCode());
        result.setErrorMessage(msg);
        return result;
    }

    public static Result Error(String code, String msg) {
        Result result = new Result();
        result.setSuccess(false);
        result.setErrorCode(code);
        result.setErrorMessage(msg);
        return result;
    }

    public static Result Error(ResultCode resultCode) {
        Result result = new Result();
        result.setErrorResult(resultCode.getCode(), resultCode.getMessage());
        return result;
    }

    public static Result Error(ResultCode resultCode, List<String> errorList) {
        Result result = new Result();
        result.setErrorList(errorList);
        result.setErrorResult(resultCode.getCode(), resultCode.getMessage());
        return result;
    }

    public static Result Error(ResultCode resultCode, int status) {
        Result result = new Result();
        result.setErrorResult(resultCode.getCode(), resultCode.getMessage());
        result.setStatus(status);
        return result;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTotalPage() {
        if (getPageSize() > 0 && getTotalNum() > 0) {
            return (getTotalNum() % getPageSize() == 0L ? (getTotalNum() / getPageSize()) : (getTotalNum() / getPageSize() + 1));
        } else {
            return 0;
        }
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public Result setErrorList(List<String> errorList) {
        this.errorList = errorList;
        return this;
    }

    public void addErrorMsg(String errorMsg) {
        if (errorList == null) {
            errorList = new ArrayList<>();
        }
        errorList.add(errorMsg);
    }


    private Result setErrorResult(String code, String msg) {
        this.success = false;
        this.errorCode = code;
        this.errorMessage = msg;
        return this;
    }
}
