package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

@Data
public class MergedDataSum {

    DataSum data;
    int effectNum;

    public MergedDataSum(DataSum data, int effectNum) {
        this.data = data;
        this.effectNum = effectNum;
    }
}
