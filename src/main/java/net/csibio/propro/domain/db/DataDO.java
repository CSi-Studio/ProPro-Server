package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@CompoundIndexes({
        @CompoundIndex(name = "overviewId_peptideRef", def = "{'overviewId':1,'peptideRef':1}"),
        @CompoundIndex(name = "overviewId_proteins", def = "{'overviewId':1,'proteins':1}"),
        @CompoundIndex(name = "overviewId_peptideRef_status", def = "{'overviewId':1,'peptideRef':1,'status':1}"),
        @CompoundIndex(name = "overviewId_status", def = "{'overviewId':1,'status':1}"),
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
    @Indexed
    List<String> proteins;
    @Indexed
    Integer status; //鉴定态

    Double libRt;  //该肽段片段的理论rt值,从标准库中冗余所得

    Double irt; //该肽段的理论rt值,从标准库中冗余所得,并且经过了irt校准

    String cutInfosFeature; //由cutInfoMap转换所得
    String libIntFeature; //由cutInfoMap转换所得

    List<PeakGroup> peakGroupList;

    byte[] ionsHighBytes;

    byte[] ionsLowBytes;

    //压缩后的rt列表,对应rtArray
    byte[] rtBytes;
    //压缩后的ms1数据
    byte[] ms1IntsBytes;
    //precursor未电解离的部分
    byte[] selfIntsBytes;
    //压缩后的intensityMap,对应intensityMap
    Map<String, byte[]> intMapBytes;

    //*******************非数据库字段,仅在计算过程中产生*******************************
    @Transient
    float[] rtArray;  //排序后的rt
    @Transient
    Map<String, float[]> intMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)
    @Transient
    float[] ms1Ints; //ms1谱图
    @Transient
    float[] selfInts; //precursor未电解离的谱图
    @Transient
    Map<String, Float> cutInfoMap; //冗余的peptide切片信息,key为cutInfo,value为mz
    @Transient
    Map<String, Double> libIntMap; //冗余的peptide切片信息,key为cutInfo,value为intensity
    @Transient
    int[] ionsHigh; //用于存储每一张Spectrum的碎片强度值大于300的碎片数目
    @Transient
    int[] ionsLow; //用于存储每一张Spectrum的碎片强度值大于0的碎片数目

    public DataDO() {
    }

    public DataDO(PeptideCoord coord) {
        this.setProteins(coord.getProteins());
        this.setPeptideRef(coord.getPeptideRef());
        this.setDecoy(coord.isDecoy());
        this.setLibRt(coord.getRt());
        this.setIrt(coord.getIrt());
        try {
            this.setCutInfoMap(coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, f -> f.getMz().floatValue())));
        } catch (Exception e) {
            System.out.println(peptideRef);
        }
    }

    public DataDO clone() {
        DataDO clone = new DataDO();
        BeanUtils.copyProperties(this, clone);
        return clone;
    }

}
