package net.csibio.propro.domain.vo;

import lombok.Data;

import java.util.Set;

@Data
public class PeptideUpdateVO {

    /**
     * Peptide ID
     */
    String id;

    /**
     * 对应蛋白质名称,默认为UniProt网站上可以搜索到的蛋白质名称
     */
    Set<String> proteins;

    /**
     * 肽段的荷质比MZ
     */
    Double mz;

    /**
     * 是否失效
     */
    Boolean disable = false;

    /**
     * 肽段的归一化RT
     */
    Double rt;

}
