package net.csibio.propro.domain.bean.peptide;

import lombok.Data;
import net.csibio.propro.domain.db.PeptideDO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-17 10:16
 * 具体的注释说明请参考PeptideDO类
 */
@Data
public class PeptideCoord {

    String id;

    String peptideRef;

    List<String> proteins;
    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列(不包含UniModId)
     */
    String sequence;
    Integer charge;

    /**
     * 对应的前体荷质比
     */
    Double mz;

    List<FragmentInfo> fragments;

    /**
     * 是否在蛋白中是unique类型的肽段
     */
    Boolean isUnique;

    /**
     * 库中的rt
     */
    Double rt;

    /**
     * 经过校准以后的rt值
     */
    Double irt;

    /**
     * 如果是伪肽段,则本字段代表的是伪肽段中unimod的位置
     * key为unimod在肽段中的位置,位置从0开始计数,value为unimod的Id(参见unimod.obo文件)
     */
    HashMap<Integer, String> unimodMap;

    /**
     * 伪肽段的信息
     */
    String decoySequence;
    HashMap<Integer, String> decoyUnimodMap;
    List<FragmentInfo> decoyFragments;

    /**
     * 是否作为伪肽段存在,不存储到数据库中
     */
    boolean decoy = false;

    /**
     * rtStart是在计算时使用的,并不会存在数据库中
     */
    double rtStart;
    /**
     * rtEnd是在计算时使用的,并不会存在数据库中
     */
    double rtEnd;


    public PeptideCoord() {
    }

    public PeptideCoord(PeptideDO peptide) {
        this.id = peptide.getId();
        this.peptideRef = peptide.getPeptideRef();
        this.mz = peptide.getMz();
        this.fragments = peptide.getFragments();
        this.rt = peptide.getRt();
        this.decoySequence = peptide.getDecoySequence();
        this.decoyUnimodMap = peptide.getDecoyUnimodMap();
        this.decoyFragments = peptide.getDecoyFragments();
    }

    public List<FragmentInfo> getFragments() {
        return decoy ? decoyFragments : fragments;
    }

    public Map<String, FragmentInfo> buildFragmentMap() {
        List<FragmentInfo> infos = decoy ? decoyFragments : fragments;
        return infos.stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, Function.identity()));
    }

    public HashMap<Integer, String> getUnimodMap() {
        return decoy ? decoyUnimodMap : unimodMap;
    }

    public String getSequence() {
        return decoy ? decoySequence : sequence;
    }

    //根据自身构建IntensityMap,key为cutInfo,value为对应的Intensity值
    public Map<String, Float> buildIntensityMap() {

        try {
            Map<String, Float> map = getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, f -> f.getIntensity().floatValue()));
            return map;
        } catch (Exception e) {
            System.out.printf(peptideRef);
            e.printStackTrace();
        }

        return null;
    }

    public void setRtRange(double start, double end) {
        this.rtStart = start;
        this.rtEnd = end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof PeptideCoord) {
            PeptideCoord target = (PeptideCoord) obj;
            if (this.getPeptideRef().equals(target.getPeptideRef())) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (peptideRef).hashCode();
    }
}
