package net.csibio.propro.domain.bean.data;

import lombok.Data;

import java.util.List;

@Data
public class BaseData {

    String id;

    String overviewId;

    String peptideRef;

    Boolean decoy = false; //是否是伪肽段

    List<String> proteins;

    Double libRt;  //该肽段片段的理论rt值,从标准库中冗余所得

    Integer status; //鉴定态

    String cutInfosFeature; //由cutInfoMap转换所得
}
