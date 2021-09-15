package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

@Data
public class BatchDataSum {

    DataSum data;
    int effectNum;

    public BatchDataSum(DataSum data, int effectNum) {
        this.data = data;
        this.effectNum = effectNum;
    }
}
