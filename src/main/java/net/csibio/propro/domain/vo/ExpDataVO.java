package net.csibio.propro.domain.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExpDataVO {

    String expId;

    String peptideRef;

    float[] rtArray;  //排序后的rt

    Map<String, float[]> intMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)

    Map<String, Float> cutInfoMap; //冗余的peptide切片信息,key为cutInfo,value为mz

    Integer status;

    Double fdr;

    Double qValue;

    //最终鉴定的时间
    Double realRt;

    //Intensity Sum
    Double sum;

    //最终的定量值
    String fragIntFeature;

    public ExpDataVO() {
    }

    public ExpDataVO(String expId, String peptideRef) {
        this.expId = expId;
        this.peptideRef = peptideRef;
    }
}
