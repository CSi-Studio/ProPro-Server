package net.csibio.propro.utils;

import lombok.Data;

@Data
public class SourceNode {
    String id;
    String name;
    double[] value;
    int category;
    double symbolSize;
    String symbol;
}
