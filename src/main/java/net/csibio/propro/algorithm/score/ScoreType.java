package net.csibio.propro.algorithm.score;

import java.util.ArrayList;
import java.util.List;

public enum ScoreType {

    //    TotalScore("↑TotalScore", "根据权重算出的加权总分-加权总分的平均分"),
    InitScore("↑InitScore", "初始打分"),
    IonsDelta("↓IonsDeltaScore", "该次运算中所有峰中最高离子数总和 - 对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和"),
    IsoCorr("↑IsoCorr", "碎片的同位素存在的可能性"),
    IsoOverlap("↓IsoOverlap", "碎片是其他肽段同位素的可能性,feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和"),
    XcorrShape("↑XcorrShape", "互相关序列最大值的平均值"),
    XcorrShapeW("↑XcorrShapeW", "带权重的互相关序列最大值的平均值"),
    LibCorr("↑LibCorr", "对run和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差"),
    LibApexCorr("↑LibApexCorr", "对run和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差"),
    LibShift("↓LibShift", ""),
    LibDotprod("↑LibDotprod", ""),
    LibRsmd("↓LibRsmd", "对归一化后 库强度与实验强度的差平均值Avg(|LibInt-RunInt|)"),
    LibSpearman("↑LibSpearman", "实验中碎片顺序与库中顺序的差别"),
    LibManhattan("↓LibManhattan", ""),
    LibSangle("↓LibSangle", ""),
    LibMeanSquare("↓LibMeanSquare", ""),
    CorrCoelution("↓CorrCoelution", "互相关偏移的mean + std"),
    CorrCoelutionW("↓CorrCoelutionW", "带权重的相关偏移sum"),
    MassdevScore("↓MassdevScore", "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和,即理论mz与实际mz(按intensity加权计算)的差别程度"),
    MassdevScoreW("↓MassdevScoreW", "[库强度相关打分]按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和,即理论mz与实际mz(按intensity加权计算)的差别程度(程度按照库中intensity的强度进行加成)"),
    IntensityScore("↑IntensityScore", "同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和"),
    NormRtScore("↓NormRtScore", "归一化后libRt与groupRt之差,缺点是iRT有波动的时候会导致本打分分数降低"),
    LogSnScore("↑LogSnScore", "log(距离ApexRt最近点的stn值之和)"),
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
