package net.csibio.propro.constants.enums;

public enum IdentifyStatus {

    //尚未鉴定
    WAIT(0, "WAIT"),
    //鉴定成功
    SUCCESS(1, "SUCCESS"),
    //鉴定失败
    FAILED(2, "FAILED"),
    //未满足鉴定条件
    NO_FIT(3, "NO_FIT"),
    ;

    int code;

    String desc;

    IdentifyStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

}
