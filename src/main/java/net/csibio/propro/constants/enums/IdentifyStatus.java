package net.csibio.propro.constants.enums;

public enum IdentifyStatus {

    //尚未鉴定
    WAIT(0, "WAIT"),
    //鉴定成功
    SUCCESS(1, "SUCCESS"),
    //鉴定失败
    FAILED(2, "FAILED"),
    //未满足鉴定条件,没有足够的肽段碎片
    NO_ENOUGH_FRAGMENTS(3, "NO_ENOUGH_FRAGMENTS"),
    //NO_PEAK_GROUP_FIND
    NO_PEAK_GROUP_FIND(4, "NO_PEAK_GROUP_FIND"),
    //仅在导出的时候会使用的状态,未进入打分轮,没有EIC信号
    NO_EIC_FIND(5, "NO_EIC_FIND"),
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
