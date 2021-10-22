package net.csibio.propro.algorithm.score;

import com.google.common.collect.Lists;
import net.csibio.propro.annotation.Section;
import net.csibio.propro.constants.constant.Constants;

import java.util.ArrayList;
import java.util.List;

@Section(name = "ScoreType", key = "TypeName", value = "PyProphetName", Version = "1")
public enum ScoreType {

    WeightedTotalScore("WeightedTotalScore", "",
            "根据权重算出的加权总分-加权总分的平均分",
            null, false),
    InitScore("InitScore", "main_VarXxSwathPrelimScore",
            "Swath主打分",
            true, true),
    NormRtScore("NormRtScore", "var_NormRtScore",
            "归一化后libRt与groupRt之差,缺点是iRT有波动的时候会导致本打分分数降低",
            false, true),
    LogSnScore("LogSnScore", "var_LogSnScore",
            "log(距离ApexRt最近点的stn值之和)",
            true, true),
    IonsCountDeltaScore("IonsCountDeltaScore", "var_IonsCountDeltaScore",
            "该次运算中所有峰中最高离子数总和 - 对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和",
            false, true),
    IonsCountWeightScore("IonsCountWeightScore", "var_IonsCountWeightScore",
            "对应的光谱图中可以检测到的符合该碎裂模式下的离子数量之和与该peptide碎片长度的比例",
            true, true),
    IntensityScore("IntensityScore", "var_IntensityScore",
            "同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和",
            true, false),
    IsotopeCorrelationScore("IsotopeCorrelationScore", "var_IsotopeCorrelationScore",
            "",
            true, true),
    IsotopeOverlapScore("IsotopeOverlapScore", "var_IsotopeOverlapScore",
            "feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和",
            true, true),
    XcorrCoelution("XcorrCoelution", "var_XcorrCoelution",
            "互相关偏移的mean + std",
            false, true),
    XcorrCoelutionWeighted("XcorrCoelutionWeighted", "var_XcorrCoelutionWeighted",
            "带权重的相关偏移sum",
            false, true),
    XcorrShape("XcorrShape", "var_XcorrShape",
            "互相关序列最大值的平均值",
            true, true),
    XcorrShapeWeighted("XcorrShapeWeighted", "var_XcorrShapeWeighted",
            "带权重的互相关序列最大值的平均值",
            true, true),
    LibraryCorr("LibraryCorr", "var_LibraryCorr",
            "对experiment和library intensity算Pearson相关系",
            true, true),
    LibraryRsmd("LibraryRsmd", "var_LibraryRsmd",
            "对归一化后 库强度与实验强度的差平均值Avg(|LibInt-ExpInt|)",
            false, true),
    LibraryDotprod("LibraryDotprod", "var_LibraryDotprod",
            "",
            true, true),
    LibraryManhattan("LibraryManhattan", "var_LibraryManhattan",
            "",
            false, true),
//    LibrarySangle("LibrarySangle", "var_LibrarySangle",
//            "",
//            false, true),

    MassdevScore("MassdevScore", "var_MassdevScore",
            "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和,即理论mz与实际mz(按intensity加权计算)的差别程度",
            false, true),
    MassdevScoreWeighted("MassdevScoreWeighted", "var_MassdevScoreWeighted",
            "[库强度相关打分]按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和,即理论mz与实际mz(按intensity加权计算)的差别程度(程度按照库中intensity的强度进行加成)",
            false, true),


//    LibraryRootmeansquare("LibraryRootmeansquare", "var_LibraryRootmeansquare",
//            "",
//            false, false),
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

    boolean isUsed;

    ScoreType(String typeName, String pyProphetName, String description, Boolean biggerIsBetter, Boolean isUsed) {
        this.name = typeName;
        this.pyProphetName = pyProphetName;
        this.biggerIsBetter = biggerIsBetter;
        this.isUsed = isUsed;
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

    public boolean getIsUsed() {
        return isUsed;
    }

    public Boolean getBiggerIsBetter() {
        return biggerIsBetter;
    }

    public String getDesc() {
        return desc;
    }

    public static List<ScoreType> getUsedTypes() {
        List<ScoreType> types = new ArrayList<>();
        for (ScoreType type : values()) {
            if (type.isUsed) {
                types.add(type);
            }
        }
        return types;
    }

    public static List<String> getUsedTypesName() {
        List<String> types = new ArrayList<>();
        for (ScoreType type : values()) {
            if (type.isUsed) {
                types.add(type.getName());
            }
        }
        return types;
    }

    public static List<ScoreType> getShownTypes() {
        List<ScoreType> types = Lists.newArrayList(values());
        types.remove(ScoreType.WeightedTotalScore);
        types.remove(ScoreType.InitScore);
        return types;
    }

    public static List<ScoreType> getUnusedTypes() {
        List<ScoreType> types = new ArrayList<>();
        for (ScoreType type : values()) {
            if (!type.isUsed) {
                types.add(type);
            }
        }
        return types;
    }

    public static List<String> getAllTypesName() {
        List<String> scoreNameList = new ArrayList<>();
        for (ScoreType type : values()) {
            scoreNameList.add(type.getName());
        }
        return scoreNameList;
    }

    public static String getPyProphetScoresColumns(String spliter) {
        StringBuilder columns = new StringBuilder();
        List<ScoreType> scoreTypes = ScoreType.getUsedTypes();
        for (int i = 0; i < scoreTypes.size(); i++) {
            if (i != scoreTypes.size() - 1) {
                columns.append(scoreTypes.get(i).getPyProphetName()).append(spliter);
            } else {
                columns.append(scoreTypes.get(i).getPyProphetName()).append(Constants.CHANGE_LINE);
            }
        }
        return columns.toString();
    }

}
