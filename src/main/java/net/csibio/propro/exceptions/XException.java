package net.csibio.propro.exceptions;

import lombok.Data;
import net.csibio.propro.constants.enums.ResultCode;

import java.util.List;

@Data
public class XException extends Exception {

    ResultCode resultCode;

    List<String> errorList;

    public XException(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public XException(ResultCode resultCode, List<String> errorList) {
        this.resultCode = resultCode;
        this.errorList = errorList;
    }
}
