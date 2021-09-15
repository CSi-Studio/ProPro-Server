package net.csibio.propro.algorithm.batch.bean;

import lombok.Data;

import java.util.List;

@Data
public class DataSum {

    List<String> proteins;
    String peptideRef;
    Double sum;
}
