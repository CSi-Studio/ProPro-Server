package net.csibio.propro.domain.options;

import lombok.Data;

@Data
public class QuickFilterOptions {

    Double minShapeScore = 0.6d; // shape的筛选阈值,严格筛选建议在0.6左右,普通筛选建议在0.3
    Double minShapeWeightScore = 0.3d; //shape的筛选阈值,严格筛选建议在0.6左右,普通筛选建议在0.1

}
