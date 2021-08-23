package net.csibio.propro.domain.bean.peptide;

import lombok.Data;

@Data
public class FragmentInfo {

    /**
     * format : y3^2
     */
    String cutInfo;

    /**
     * 是否为预测碎片
     */
    Boolean predict;

    /**
     * 离子片段的荷质比MZ(Mono荷质比)
     */
    Double mz;

    Double intensity;

    /**
     * 离子片段的带电量
     */
    Integer charge;

    /**
     * 注释,see http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST#Annotation_syntax:
     */
    String annotations;

    public FragmentInfo() {
    }

    public FragmentInfo(String cutInfo, Double mz, Integer charge) {
        this.cutInfo = cutInfo;
        this.mz = mz;
        this.charge = charge;
    }

    public FragmentInfo(String cutInfo, Double mz, Double intensity, Integer charge) {
        this.cutInfo = cutInfo;
        this.mz = mz;
        this.intensity = intensity;
        this.charge = charge;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof FragmentInfo) {
            FragmentInfo fragment = (FragmentInfo) obj;
            if (cutInfo == null || fragment.getCutInfo() == null) {
                return false;
            }

            return (this.cutInfo.equals(fragment.getCutInfo()));
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        return cutInfo.hashCode();
    }
}
