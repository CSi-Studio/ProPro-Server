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
    /**
     * @see net.csibio.propro.constants.enums.IdentifyStatus
     */
    @Indexed
    int status; //鉴定结果

    @Indexed
    Double fdr; //最终给出的FDR打分

    Double qValue; //最终给出的qValue

    Double libRt;  //该肽段片段的理论rt值,从标准库中冗余所得
    Double realRt;//最终选出的最佳峰RT,即算法认为的实际rt
    Double libMz; //该肽段的前体mz,从标准库中冗余所得
    Double realMz;  //算法认为的实际mz
    HashMap<String, Float> mzMap = new HashMap<>();  //key为cutInfo, value为对应的mz

    List<FeatureScores> featureScoresList;

    Double intensitySum;

    //最终的定量值
    String fragIntFeature;

    //*******************非数据库字段*******************************
    //排序后的rt,仅在解压缩的时候使用,不存入数据库
    @Transient
    Float[] rtArray;

    //key为cutInfo, value为对应的intensity值,仅在解压缩的时候使用,不存入数据库
    @Transient
    HashMap<String, float[]> intensityMap = new HashMap<>();
}
