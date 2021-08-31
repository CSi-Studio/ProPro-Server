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
    String dataId;
    @Indexed
    String peptideRef;
    @Indexed
    Boolean decoy = false;
    @Indexed
    Double fdr;
    @Indexed
    int status; //鉴定结果

    
}
