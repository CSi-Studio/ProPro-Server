package net.csibio.propro.algorithm.score;

import java.util.ArrayList;
import java.util.List;

public enum ScoreType {

    TotalScore("↑TotalScore", "根据权重算出的加权总分-加权总分的平均分"),
    InitScore("↑InitScore", "初始打分"),
    IonsDelta("↓IonsDeltaScore", "该次运算中所有峰中最高离子数总和 - 对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和"),
    IsoCorr("↑IsoCorr", "碎片的同位素存在的可能性"),
    IsoOverlap("↓IsoOverlap", "碎片是其他肽段同位素的可能性,feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和"),
    XcorrShape("↑XcorrShape", "互相关序列最大值的平均值"),
    XcorrShapeWeighted("↑XcorrShapeWeighted", "带权重的互相关序列最大值的平均值"),
    LibraryCorr("↑LibraryCorr", "对run和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差"),
    LibraryDotprod("↑LibraryDotprod", ""),
    LibraryRsmd("↓LibraryRsmd", "对归一化后 库强度与实验强度的差平均值Avg(|LibInt-RunInt|)"),
    LibraryManhattan("↓LibraryManhattan", ""),
    LibrarySangle("↓LibrarySangle", ""),
    LibraryRootmeansquare("↓LibraryRootmeansquare", ""),
    XcorrCoelution("↓XcorrCoelution", "互相关偏移的mean + std"),
    XcorrCoelutionWeighted("↓XcorrCoelutionWeighted", "带权重的相关偏移sum"),
    MassdevScore("↓MassdevScore", "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和,即理论mz与实际mz(按intensity加权计算)的差别程度"),
    MassdevScoreWeighted("↓MassdevScoreWeighted", "[库强度相关打分]按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和,即理论mz与实际mz(按intensity加权计算)的差别程度(程度按照库中intensity的强度进行加成)"),
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

    public static List<String> scoreTypes4Irt() {
        List<String> scoreTypes4Irt = new ArrayList<>();
        scoreTypes4Irt.add(ScoreType.TotalScore.getName());
        scoreTypes4Irt.add(ScoreType.InitScore.getName());
        scoreTypes4Irt.add(ScoreType.IonsDelta.getName());
        scoreTypes4Irt.add(ScoreType.XcorrShape.getName());
        scoreTypes4Irt.add(ScoreType.XcorrShapeWeighted.getName());
        scoreTypes4Irt.add(ScoreType.XcorrCoelution.getName());
        scoreTypes4Irt.add(ScoreType.XcorrCoelutionWeighted.getName());
        scoreTypes4Irt.add(ScoreType.LibraryDotprod.getName());
        return scoreTypes4Irt;
    }


    public static List<String> usedScoreTypes() {
        List<String> scoreTypes = getAllTypesName();
        return scoreTypes;
    }
}
