package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@CompoundIndexes({
        @CompoundIndex(name = "overviewId_peptideRef_decoy", def = "{'overviewId':1,'peptideRef':1,'decoy':1}", unique = true)
})
public class DataSumDO extends BaseDO {

    @Id
    String id;
    @Indexed
    String overviewId;
    @Indexed
    String peptideRef;
    @Indexed
    Boolean decoy = false;
    @Indexed
    Double fdr;
    @Indexed
    Integer status; //鉴定结果

    Double qValue;

    //最终鉴定的时间
    Double realRt;

    //Intensity Sum
    Double sum;

    //最终的定量值
    String fragIntFeature;
}
