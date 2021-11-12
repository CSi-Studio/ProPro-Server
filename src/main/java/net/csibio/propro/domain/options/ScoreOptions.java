package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;

import java.util.List;

@Data
public class ScoreOptions {

    List<String> scoreTypes = ScoreType.getAllTypesName(); //打分类型,详情见net.csibio.propro.algorithm.score.ScoreType
    int maxCandidateIons = 10; //候选离子数目,会极大的影响计算速度
}
