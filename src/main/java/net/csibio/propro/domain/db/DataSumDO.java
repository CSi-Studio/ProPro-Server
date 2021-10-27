package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.score.PeakGroupScore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
@CompoundIndexes({
        @CompoundIndex(name = "overviewId_peptideRef_decoy", def = "{'overviewId':1,'peptideRef':1,'decoy':1}", unique = true),
        @CompoundIndex(name = "overviewId_proteins_decoy", def = "{'overviewId':1,'proteins':1,'decoy':1}")
})
public class DataSumDO extends BaseDO {

    @Id
    String id;
    @Indexed
    String overviewId;
    @Indexed
    List<String> proteins;
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

    //距离realRt最近的光谱图的rt
    Double nearestRt;

    //对应的最终的主峰的打分
    Double totalScore;

    //Intensity Sum
    Double sum;

    Integer totalIons;

    //最终的定量值
    String fragIntFeature;

    public static DataSumDO buildByPeakGroupScore(List<String> proteins, String peptideRef, PeakGroupScore selectPeakGroup) {
        DataSumDO dataSum = new DataSumDO();
        dataSum.setPeptideRef(peptideRef);
        dataSum.setProteins(proteins);
        dataSum.setTotalIons(selectPeakGroup.getTotalIons());
        dataSum.setSum(selectPeakGroup.getIntensitySum());
        dataSum.setRealRt(selectPeakGroup.getRt());
        dataSum.setNearestRt(selectPeakGroup.getNearestRt());
        dataSum.setTotalScore(selectPeakGroup.getTotalScore());
        return dataSum;
    }
}
