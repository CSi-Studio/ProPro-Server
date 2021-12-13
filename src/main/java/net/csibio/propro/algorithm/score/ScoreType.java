package net.csibio.propro.algorithm.score;

import java.util.ArrayList;
import java.util.List;

public enum ScoreType {

    //    TotalScore("↑TotalScore", "根据权重算出的加权总分-加权总分的平均分"),
//    InitScore("↑InitScore", "初始打分"),
    IonsDelta("↓IonsDelta", "该次运算中所有峰中最高离子数总和 - 对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和"),
    IsoCorr("↑IsoCorr", "碎片的同位素存在的可能性"),
    IsoOverlap("↓IsoOverlap", "碎片是其他肽段同位素的可能性,feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和"),
    CorrShape("↑CorrShape", "互相关序列最大值的平均值"),
    CorrShapeW("↑CorrShapeW", "带权重的互相关序列最大值的平均值"),
    Pearson("↑Pearson", "对run和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差"),
    Elution("↑Elution", "对最佳碎片和其余碎片计算elution profile的Pearson相关性总和"),
    MS1("↑MS1", "对最佳碎片和MS1谱图计算elution profile的Pearson相关性"),
    SELF_DP("↑SELF_DP", "对最佳碎片和Precursor谱图计算elution profile的Dotprod"),
    SELF("↑SELF", "对最佳碎片和Precursor谱图计算elution profile的Pearson相关性"),
    MS1_SELF("↑MS1_SELF", "对MS1和Precursor谱图计算elution profile的Pearson相关性"),
    //    ApexPearson("↑ApexPearson", "对run和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差"),
    IntShift("↓IntShift", "LibraryIntensityShift"),
    Dotprod("↑Dotprod", "LibDotprod"),
    Rsmd("↓Rsmd", "LibRsmd,对归一化后 库强度与实验强度的差平均值Avg(|LibInt-RunInt|)"),
    Manhattan("↓Manhattan", "LibManhattan"),
    Sangle("↓Sangle", "LibSangle"),
    AvgSqr("↓AvgSqr", "LibRootMeanSquare"),
    CorrCoe("↓CorrCoe", "互相关偏移的mean + std"),
    CorrCoeW("↓CorrCoeW", "带权重的相关偏移sum"),
    MassDev("↓MassDev", "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和,即理论mz与实际mz(按intensity加权计算)的差别程度"),
    MassDevW("↓MassDevW", "[库强度相关打分]按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和,即理论mz与实际mz(按intensity加权计算)的差别程度(程度按照库中intensity的强度进行加成)"),
    NormRt("↓NormRt", "归一化后libRt与groupRt之差,缺点是iRT有波动的时候会导致本打分分数降低"),
    LogSn("↑LogSn", "log(距离ApexRt最近点的stn值之和)"),

//    ManhattScore("ManhattScore", ""),
//    ElutionModelFitScore("ElutionModelFitScore", ""),
//    IntensityTotalScore("IntensityTotalScore", "针对特殊需要的只做Intensity分类得到的总分-Intensity总分"),
    ;
    String name;

    String desc;

    ScoreType(String typeName, String description) {
        this.name = typeName;
        this.desc = description;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static List<String> getAllTypesName() {
        List<String> scoreNameList = new ArrayList<>();
        for (ScoreType type : values()) {
            scoreNameList.add(type.getName());
        }
        return scoreNameList;
    }

    public static List<String> usedScoreTypes() {
        List<String> scoreTypes = getAllTypesName();
        return scoreTypes;
    }
}
