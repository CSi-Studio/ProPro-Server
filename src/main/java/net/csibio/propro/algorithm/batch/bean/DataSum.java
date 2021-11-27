package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

import java.util.List;

@Data
public class DataSum {

    List<String> proteins;
    String peptideRef;
    Double intensitySum;
    Double fitIntSum;

    public Double getIntensitySum() {
        if (fitIntSum != null) {
            return fitIntSum;
        } else {
            return intensitySum;
        }
    }
}
