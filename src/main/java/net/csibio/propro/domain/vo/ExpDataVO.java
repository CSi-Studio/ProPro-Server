package net.csibio.propro.domain.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class ExpDataVO {

    String expId;

    String peptideRef;

    //色谱图数据,cutInfoList与eicList大小一致,一一对应
    List<String> cutInfoList;

    Float[] rtArray;  //排序后的rt

    HashMap<String, float[]> intensityMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)
}
