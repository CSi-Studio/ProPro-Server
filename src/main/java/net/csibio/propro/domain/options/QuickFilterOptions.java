package net.csibio.propro.domain.options;

import lombok.Data;

@Data
public class QuickFilterOptions {

    Double minShapeScore = 0.6d; // shape的筛选阈值,一般建议在0.6左右
    Double minShapeWeightScore = 0.8d; //shape的筛选阈值,一般建议在0.8左右

}
