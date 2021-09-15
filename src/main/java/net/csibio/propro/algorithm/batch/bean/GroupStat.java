package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

import java.util.Map;

@Data
public class GroupStat {

    Double missingRatio;
    int proteins = 0;  //鉴定蛋白数目
    Map<String, DataSum> dataMap;
    int hit1 = 0;
    int hit2 = 0;
    int hit3 = 0;
}
