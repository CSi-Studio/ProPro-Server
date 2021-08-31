package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.score.FeatureScores;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashMap;
import java.util.List;

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
    //是否是伪肽段
    @Indexed
    Boolean decoy = false;

    Double libRt;  //该肽段片段的理论rt值,从标准库中冗余所得

    Integer status;
    
    //冗余的peptide切片信息
    List<String> cutInfos;

    List<FeatureScores> featureScoresList;

    //*******************非数据库字段,仅在计算过程中产生*******************************
    @Transient
    Float[] rtArray;  //排序后的rt
    @Transient
    HashMap<String, float[]> intensityMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)
    @Transient
    Float[] mzs; //每一个切片对应的mz,与cutInfos数组长度相同且一一对应
}
