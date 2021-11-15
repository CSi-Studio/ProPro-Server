package net.csibio.propro.domain.bean.data;

import lombok.Data;
import net.csibio.propro.domain.bean.score.PeakGroup;

import java.util.List;

@Data
public class BaseData {

    String id;

    String overviewId;

    String peptideRef;

    Boolean decoy = false; //是否是伪肽段

    List<String> proteins;

    List<PeakGroup> peakGroupList;

    Double fdr;

    Double qValue;

    Double irt;  //该肽段片段的理论rt时间(已经过irt校准)

    Integer status; //鉴定态

    String cutInfosFeature; //由cutInfoMap转换所得
}
