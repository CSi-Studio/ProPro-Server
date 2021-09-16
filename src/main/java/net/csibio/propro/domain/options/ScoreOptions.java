package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;

import java.util.List;

@Data
public class ScoreOptions {

    List<String> scoreTypes = ScoreType.getAllTypesName(); //打分类型,详情见net.csibio.propro.algorithm.score.ScoreType
    boolean diaScores = true; //是否使用DIA打分,如果使用DIA打分的话,需要提前读取Aird文件中的谱图信息以提升系统运算速度
}
