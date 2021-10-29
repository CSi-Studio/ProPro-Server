package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

import java.util.Map;

@Data
public class GroupStat {

    Double missingRatio;
    int proteins = 0;  //鉴定蛋白数目
    Map<String, DataSum> dataMap; //key为 protein-->peptideRef,例如 sp|P0A867|TALA_ECOLI-->HYHPQDATTNPSLLLK_3
    int hit1 = 0;
    int hit2 = 0;
    int hit3 = 0;
}
