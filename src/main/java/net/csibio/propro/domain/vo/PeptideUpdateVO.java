package net.csibio.propro.domain.vo;

import lombok.Data;

@Data
public class PeptideUpdateVO {

    /**
     * Peptide ID
     */
    String id;

    /**
     * 对应蛋白质名称,默认为UniProt网站上可以搜索到的蛋白质名称
     */
    String protein;

    /**
     * 肽段的荷质比MZ
     */
    Double mz;

    /**
     * 肽段的归一化RT
     */
    Double rt;

    /**
     * 该肽段是否是该蛋白的不重复肽段
     */
    Boolean isUnique;

}
