package net.csibio.propro.algorithm.stat;


public class StatConst {

    //蛋白质数
    public static String Protein_Count = "Protein_Count";
    //肽段数
    public static String Peptide_Count = "Peptide_Count";
    //肽段碎片数
    public static String Fragment_Count = "Fragment_Count";

    //肽段在mz上的分布区间,间隔为5%,即总共采样20处
    public static String Peptide_Dist_On_Mz_5 = "Peptide_Dist_On_Mz_5";
    //肽段在rt上的分布区间,间隔为5%,即总共采样20处
    public static String Peptide_Dist_On_RT_5 = "Peptide_Dist_On_RT_5";

    //伪肽段在指定窗口内的最小重复度,指定窗口为5%
    public static String DECOY_REPEAT_RATIO_5 = "DECOY_REPEAT_RATIO_5";
    public static String DECOY_REPEAT_RATIO_10 = "DECOY_REPEAT_RATIO_10";

    public static String TOTAL_PEPTIDE_COUNT = "TOTAL_PEPTIDE_COUNT";
    public static String TOTAL_PEAK_COUNT = "TOTAL_PEAK_COUNT";
    public static String STATUS_SUCCESS_PEPTIDE_COUNT = "STATUS_SUCCESS_PEPTIDE_COUNT";

    public static String MATCHED_UNIQUE_PEPTIDE_COUNT = "MATCHED_UNIQUE_PEPTIDE_COUNT";
    public static String MATCHED_TOTAL_PEPTIDE_COUNT = "MATCHED_TOTAL_PEPTIDE_COUNT";
    public static String MATCHED_UNIQUE_PROTEIN_COUNT = "MATCHED_UNIQUE_PROTEIN_COUNT";
    public static String MATCHED_TOTAL_PROTEIN_COUNT = "MATCHED_TOTAL_PROTEIN_COUNT";

    public static String TARGET_DIST = "TARGET_DIST";
    public static String DECOY_DIST = "DECOY_DIST";
}
