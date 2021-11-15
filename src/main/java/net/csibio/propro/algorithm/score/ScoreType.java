package net.csibio.propro.algorithm.score;

import net.csibio.propro.annotation.Section;

import java.util.ArrayList;
import java.util.List;

@Section(name = "ScoreType", key = "TypeName", value = "PyProphetName", Version = "1")
public enum ScoreType {

    WeightedTotalScore("WeightedTotalScore", "",
            "根据权重算出的加权总分-加权总分的平均分",
            true),
    InitScore("InitScore", "main_VarXxSwathPrelimScore",
            "初始打分",
            true),
    IonsCountDeltaScore("IonsCountDeltaScore", "var_IonsCountDeltaScore",
            "该次运算中所有峰中最高离子数总和 - 对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和",
            false),
    IsotopeCorrelationScore("IsotopeCorrelationScore", "var_IsotopeCorrelationScore",
            "碎片的同位素存在的可能性",
            true),
    IsotopeOverlapScore("IsotopeOverlapScore", "var_IsotopeOverlapScore",
            "碎片是其他肽段同位素的可能性,feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和",
            false),
    XcorrShape("XcorrShape", "var_XcorrShape",
            "互相关序列最大值的平均值",
            true),
    XcorrShapeWeighted("XcorrShapeWeighted", "var_XcorrShapeWeighted",
            "带权重的互相关序列最大值的平均值",
            true),
    LibraryCorr("LibraryCorr", "var_LibraryCorr",
            "对experiment和library intensity算Pearson相关系,当有干扰碎片的时候得分会很差",
            true),
    LibraryDotprod("LibraryDotprod", "var_LibraryDotprod",
            "",
            true),
    LibraryRsmd("LibraryRsmd", "var_LibraryRsmd",
            "对归一化后 库强度与实验强度的差平均值Avg(|LibInt-ExpInt|)",
            false),
    LibraryManhattan("LibraryManhattan", "var_LibraryManhattan",
            "",
            false),
    LibrarySangle("LibrarySangle", "var_LibrarySangle",
            "",
            false),
    LibraryRootmeansquare("LibraryRootmeansquare", "var_LibraryRootmeansquare",
            "",
            false),
    XcorrCoelution("XcorrCoelution", "var_XcorrCoelution",
            "互相关偏移的mean + std",
            false),
    XcorrCoelutionWeighted("XcorrCoelutionWeighted", "var_XcorrCoelutionWeighted",
            "带权重的相关偏移sum",
            false),
    MassdevScore("MassdevScore", "var_MassdevScore",
            "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和,即理论mz与实际mz(按intensity加权计算)的差别程度",
            false),
    MassdevScoreWeighted("MassdevScoreWeighted", "var_MassdevScoreWeighted",
            "[库强度相关打分]按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和,即理论mz与实际mz(按intensity加权计算)的差别程度(程度按照库中intensity的强度进行加成)",
            false),
    IntensityScore("IntensityScore", "var_IntensityScore",
            "同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和",
            true),
    NormRtScore("NormRtScore", "var_NormRtScore",
            "归一化后libRt与groupRt之差,缺点是iRT有波动的时候会导致本打分分数降低",
            false),
    LogSnScore("LogSnScore", "var_LogSnScore",
            "log(距离ApexRt最近点的stn值之和)",
            true),

//    ManhattScore("ManhattScore", "var_ManhattScore",
//            "",
//            null, false),
//    ElutionModelFitScore("ElutionModelFitScore", "var_ElutionModelFitScore",
//            "",
//            null, false),
//    IntensityTotalScore("IntensityTotalScore", "",
//            "针对特殊需要的只做Intensity分类得到的总分-Intensity总分",
//            null, false),
    ;
    String name;

    String pyProphetName;

    String desc;

    Boolean biggerIsBetter;


    ScoreType(String typeName, String pyProphetName, String description, Boolean biggerIsBetter) {
        this.name = typeName;
        this.pyProphetName = pyProphetName;
        this.biggerIsBetter = biggerIsBetter;
        this.desc = description;
    }

    public static Boolean getBiggerIsBetter(String name) {
        for (ScoreType type : values()) {
            if (type.getName().equals(name)) {
                return type.getBiggerIsBetter();
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getPyProphetName() {
        return pyProphetName;
    }

    public Boolean getBiggerIsBetter() {
        return biggerIsBetter;
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
        scoreTypes4Irt.add(ScoreType.WeightedTotalScore.getName());
        scoreTypes4Irt.add(ScoreType.InitScore.getName());
        scoreTypes4Irt.add(ScoreType.IonsCountDeltaScore.getName());
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
