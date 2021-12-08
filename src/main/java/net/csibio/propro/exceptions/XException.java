package net.csibio.propro.exceptions;

import lombok.Data;
import net.csibio.propro.constants.enums.ResultCode;

import java.util.List;

@Data
public class XException extends Exception {

    String errorMsg;

    ResultCode resultCode;

    List<String> errorList;

    public XException(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public XException(ResultCode resultCode) {
        this.errorMsg = resultCode.getMessage();
        this.resultCode = resultCode;
    }

    public XException(ResultCode resultCode, List<String> errorList) {
        this.resultCode = resultCode;
        this.errorList = errorList;
    }
}
