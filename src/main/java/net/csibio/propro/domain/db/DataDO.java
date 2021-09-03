package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.score.PeakGroupScores;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@CompoundIndexes({
        @CompoundIndex(name = "overviewId_peptideRef", def = "{'overviewId':1,'peptideRef':1}"),
        @CompoundIndex(name = "overviewId_peptideRef_status", def = "{'overviewId':1,'peptideRef':1,'status':1}"),
        @CompoundIndex(name = "overviewId_peptideRef_decoy", def = "{'overviewId':1,'peptideRef':1,'decoy':1}", unique = true)
})
public class DataDO extends BaseDO {

    @Id
    String id;
    @Indexed
    String overviewId;
    @Indexed
    String peptideRef;
    @Indexed
    Boolean decoy = false; //是否是伪肽段

    Double libRt;  //该肽段片段的理论rt值,从标准库中冗余所得

    Integer status; //鉴定态

    String cutInfosFeature; //由cutInfoMap转换所得

    List<PeakGroupScores> scoreList;

    //压缩后的rt列表,对应rtArray
    byte[] rtBytes;
    //压缩后的intensityMap,对应intensityMap
    Map<String, byte[]> intMapBytes;

    //*******************非数据库字段,仅在计算过程中产生*******************************
    @Transient
    float[] rtArray;  //排序后的rt
    @Transient
    Map<String, float[]> intMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)
    @Transient
    Map<String, Float> cutInfoMap; //冗余的peptide切片信息,key为cutInfo,value为mz

}
