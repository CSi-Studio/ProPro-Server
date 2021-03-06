package net.csibio.propro.constants.enums;

import net.csibio.propro.annotation.Section;

import java.io.Serializable;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:38
 */
@Section(name = "ResultCode", key = "Code", value = "Message", Version = "1")
public enum ResultCode implements Serializable {

    /**
     * ******
     * 系统错误
     * *******
     */
    ERROR("SYSTEM_ERROR", "系统繁忙,请稍后再试!"),
    EXCEPTION("SYSTEM_EXCEPTION", "系统繁忙,稍后再试!"),
    IO_EXCEPTION("IO_EXCEPTION", "文件读写错误"),
    PARAMS_NOT_ENOUGH("PARAMS_NOT_ENOUGH", "入参不齐"),
    DROP_COLLECTION_ERROR("DROP_COLLECTION_ERROR", "删表失败"),
    CREATE_COLLECTION_ERROR("CREATE_COLLECTION_ERROR", "建表失败"),
    API_IS_FORBIDDEN_TO_USE("API_IS_FORBIDDEN_TO_USE", "本接口禁止使用"),

    /**
     * ******
     * 常见通用错误
     * *******
     */
    OBJECT_CANNOT_BE_NULL("OBJECT_CANNOT_BE_NULL", "对象不能为空"),
    OBJECT_IS_EXISTED("OBJECT_IS_EXISTED", "对象已存在"),
    OBJECT_NOT_EXISTED("OBJECT_NOT_EXISTED", "对象不存在"),
    QUERY_ERROR("QUERY_ERROR", "获取数据失败"),
    INSERT_ERROR("INSERT_ERROR", "插入数据失败"),
    DUPLICATE_KEY_ERROR("DUPLICATE_KEY_ERROR", "插入数据失败,已有同名项存在"),
    SAVE_ERROR("SAVE_ERROR", "保存数据失败"),
    UPDATE_ERROR("UPDATE_ERROR", "更新数据失败"),
    DELETE_ERROR("UPDATE_ERROR", "删除数据失败"),
    PARSE_ERROR("PARSE_ERROR", "解析错误"),
    ID_CANNOT_BE_NULL_OR_ZERO("ID_CANNOT_BE_NULL_OR_ZERO", "ID不能为空或者0"),
    LINE_IS_EMPTY("LINE_IS_EMPTY", "内容为空"),
    EXTRACT_FAILED("EXTRACT_FAILED", "解压缩失败"),
    UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS", "未经授权的非法访问"),
    OPERATING_SUCCESS("OPERATING_SUCCESS", "操作成功"),

    /**
     * ******
     * library
     * *******
     */
    LIBRARY_NAME_CANNOT_BE_EMPTY("LIBRARY_NAME_CANNOT_BE_EMPTY", "标准库名称不能为空"),
    LIBRARY_CANNOT_BE_EMPTY("LIBRARY_CANNOT_BE_EMPTY", "标准库不能为空"),
    DATA_IS_EMPTY("DATA_IS_EMPTY", "数据为空"),
    INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML("INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML", "上传的文件格式必须为TSV或者是TraML"),
    LIBRARY_NOT_EXISTED("LIBRARY_NOT_EXISTED", "指定的库不存在"),
    ANA_LIBRARY_NOT_EXISTED("ANA_LIBRARY_NOT_EXISTED", "指定的目标库不存在"),
    INS_LIBRARY_NOT_EXISTED("INS_LIBRARY_NOT_EXISTED", "指定的内标库不存在"),
    SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3("SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3", "搜索的片段长度至少为4"),
    PEPTIDE_REF_CANNOT_BE_EMPTY("PEPTIDE_REF_CANNOT_BE_EMPTY", "PeptideRef不能为空"),
    PEPTIDE_MZ_CANNOT_BE_NULL("PEPTIDE_MZ_CANNOT_BE_NULL", "mz不能为空"),
    PEPTIDE_SEQUENCE_CANNOT_BE_NULL("PEPTIDE_SEQUENCE_CANNOT_BE_NULL", "肽段Sequence不能为空"),
    PEPTIDE_CHARGE_CANNOT_BE_NULL("PEPTIDE_CHARGE_CANNOT_BE_NULL", "肽段Charge不能为空"),
    PEPTIDE_NOT_EXIST("PEPTIDE_NOT_EXIST", "肽段不存在"),
    NO_DECOY("NO_DECOY", "不使用文件中的Decoy"),

    /**
     * ******
     * Clinic诊所
     * *******
     */
    INS_ANA_METHOD_ID_CANNOT_BE_EMPTY_WHEN_USING_CLINIC("INS_ANA_METHOD_ID_CANNOT_BE_EMPTY_WHEN_USING_CLINIC", "使用诊所功能时项目的内标库,标准库和方法包必须设置"),

    /**
     * ******
     * run
     * *******
     */
    RUN_ID_CANNOT_BE_EMPTY("RUN_ID_CANNOT_BE_EMPTY", "实验ID不能为空"),

    SOME_RUN_HAVE_NO_DEFAULT_OVERVIEW("SOME_RUN_HAVE_NO_DEFAULT_OVERVIEW", "部分实验没有默认的实验概览"),

    RUN_NAME_CANNOT_BE_EMPTY("RUN_NAME_CANNOT_BE_EMPTY", "实验名称不能为空"),

    FILE_LOCATION_CANNOT_BE_EMPTY("FILE_LOCATION_CANNOT_BE_EMPTY", "文件路径不能为空"),

    FILE_NOT_EXISTED("FILE_NOT_EXISTED", "文件不存在"),

    FILE_ALREADY_EXISTED("FILE_ALREADY_EXISTED", "文件已存在"),

    FILE_NOT_SET("FILE_NOT_SET", "文件未设定"),

    FILE_FORMAT_NOT_SUPPORTED("FILE_FORMAT_NOT_SUPPORTED", "文件格式不支持"),

    RUN_INSERT_ERROR("RUN_INSERT_ERROR", "实验数据插入失败"),

    RUN_MZXML_FILE_MUST_BE_CONVERTED_TO_AIRD_FORMAT_FILE_FIRST("RUN_MZXML_FILE_MUST_BE_CONVERTED_TO_AIRD_FORMAT_FILE_FIRST", "原始实验文件必须转换为Aird格式文件才可以进入下一步"),

    SWATH_INFORMATION_NOT_EXISTED("SWATH_INFORMATION_NOT_EXISTED", "Swath的分块信息不存在,请确保Aird压缩过程正确"),

    IRT_FIRST("IRT_FIRST", "请先对原始实验运行Irt计算"),

    IRT_EXCEPTION("IRT_EXCEPTION", "iRT失败"),

    PRM_FILE_IS_EMPTY("PRM_FILE_IS_EMPTY", "PRM文件为空"),

    PRM_FILE_FORMAT_NOT_SUPPORTED("PRM_FILE_FORMAT_NOT_SUPPORTED", "PRM文件格式不支持"),

    FASTA_FILE_FORMAT_NOT_SUPPORTED("FASTA_FILE_FORMAT_NOT_SUPPORTED", "FASTA文件格式不支持"),

    NO_AIRD_COMPRESSION_FOR_DIA_SWATH("NO_AIRD_COMPRESSION_FOR_DIA_SWATH", "DIA-Swath Run不需要进行Aird文件压缩,请使用Propro客户端进行压缩"),

    RUN_TYPE_MUST_DEFINE("RUN_TYPE_MUST_DEFINE", "Run类型必须被定义"),

    NO_NEW_RUNS("NO_NEW_RUNS", "没有扫描到新Run"),

    /**
     * ******
     * BlockIndex
     * *******
     */
    SWATH_INDEX_LIST_MUST_BE_QUERY_WITH_RUN_ID("SWATH_INDEX_LIST_MUST_BE_QUERY_WITH_RUN_ID", "SwathIndex的列表页面必须至少按照RunId维度进行查询"),

    RUN_NOT_EXISTED("RUN_NOT_EXISTED", "实验数据不存在"),

    OVERVIEWS_MUST_USE_THE_SAME_INS_LIBRARY("OVERVIEWS_MUST_USE_THE_SAME_INS_LIBRARY", "分析概览必须使用相同的内标库"),
    OVERVIEWS_MUST_USE_THE_SAME_ANA_LIBRARY("OVERVIEWS_MUST_USE_THE_SAME_ANA_LIBRARY", "分析概览必须使用相同的标准库"),

    RUN_IDS_CANNOT_BE_EMPTY("RUN_IDS_CANNOT_BE_EMPTY", "RUN ID列表不能为空"),

    SOME_RUN_NOT_EXISTED("SOME_RUN_NOT_EXISTED", "部分实验数据不存在"),

    BLOCK_INDEX_NOT_EXISTED("BLOCK_INDEX_NOT_EXISTED", "对应的索引数据不存在"),

    SPECTRUM_NOT_EXISTED("SPECTRUM_NOT_EXISTED", "对应的原始谱图不存在"),

    PARSE_MS1_SPECTRUM_FAILED("PARSE_MS1_SPECTRUM_FAILED", "MS1谱图解析错误"),

    /**
     * ******
     * Dict
     * *******
     */
    DICT_NAME_CANNOT_BE_NULL("DICT_NAME_CANNOT_BE_NULL", "字典名称不能为空"),

    /**
     * ******
     * Method
     * *******
     */
    METHOD_NAME_CANNOT_BE_NULL("METHOD_NAME_CANNOT_BE_NULL", "参数包名称不能为空"),

    METHOD_NOT_EXISTED("METHOD_NOT_EXISTED", "参数包不存在"),

    /**
     * ******
     * Overview
     * *******
     */
    ANALYZE_FAILED("ANALYZE_FAILED", "分析失败"),
    ANALYZE_CREATE_FAILED("ANALYZE_CREATE_FAILED", "创建Overview失败"),

    ANALYSE_DATA_NOT_EXISTED("ANALYSE_DATA_NOT_EXISTED", "XIC数据不存在"),

    ANALYSE_DATA_ID_CANNOT_BE_EMPTY("ANALYSE_DATA_ID_CANNOT_BE_EMPTY", "XIC数据ID不能为空"),

    ANALYSE_DATA_ARE_ALL_ZERO("ANALYSE_DATA_ARE_ALL_ZERO", "XIC数据全部为零"),

    OVERVIEW_NOT_EXISTED("OVERVIEW_NOT_EXISTED", "分析数据概览不存在"),

    OVERVIEW_ID_CAN_NOT_BE_EMPTY("OVERVIEW_ID_CAN_NOT_BE_EMPTY", "分析数据概览ID不能为空"),

    OVERVIEW_NAME_CAN_NOT_BE_EMPTY("OVERVIEW_NAME_CAN_NOT_BE_EMPTY", "分析数据概览名称不能为空"),

    ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH("ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH", "本接口只支持校准库的数据聚合查询"),

    DATA_ID_CANNOT_BE_EMPTY("DATA_ID_CANNOT_BE_EMPTY", "DataId不能为空"),

    FRAGMENT_CANNOT_BE_EMPTY_ALL("FRAGMENT_CANNOT_BE_EMPTY_ALL", "肽段碎片不能全部为空"),

    FRAGMENT_LENGTH_IS_TOO_LONG("FRAGMENT_LENGTH_IS_TOO_LONG", "设定的肽段碎片长度大于本身长度"),

    POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE("POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE", "索引地址搜索块的数目与mzMap+1的结果不一致"),

    SOME_RUNS_HAVE_NO_DEFAULT_OVERVIEW("SOME_RUNS_HAVE_NO_DEFAULT_OVERVIEW", "部分Run没有默认的概览结果,请先进行分析或者设置默认值"),

    /**
     * ******
     * Scores
     * *******
     */
    ALL_SCORE_DATA_ARE_DECOY("ALL_SCORE_DATA_ARE_DECOY", "所有的数据都是伪肽段数据,无法打分"),

    ALL_SCORE_DATA_ARE_REAL("ALL_SCORE_DATA_ARE_REAL", "所有的数据都是真实肽段数据,未包含伪肽段,无法打分"),

    SCORES_NOT_EXISTED("SCORES_NOT_EXISTED", "打分数据不存在"),

    SCORE_DISTRIBUTION_NOT_GENERATED_YET("SCORE_DISTRIBUTION_NOT_GENERATED_YET", "子分数分布情况还未生成"),

    /**
     * ******
     * RT Normalizer
     * *******
     */
    NOT_ENOUGH_IRT_PEPTIDES("NOT_ENOUGH_IRT_PEPTIDES", "There are less than 2 iRT peptides, not enough for an RT correction"),

    /**
     * ******
     * Analyse
     * *******
     */
    COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO("COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO", "需要需要两个不同的数据分析ID才可以开始比对"),

    /**
     * ******
     * Predict
     * *******
     */
    PREDICT_MODEL_ONLY_FOR_TWO_CHARGE_PEPTIDE("PREDICT_MODEL_ONLY_FOR_TWO_CHARGE_PEPTIDE", "只能预测带2个电荷的肽段的碎片"),

    UNSUPPORTED_FRAGMENTATION_MODEL("UNSUPPORTED_FRAGMENTATION_MODEL", "不支持该肽段碎裂模型"),

    /**
     * ******
     * TASK
     * *******
     */
    TASK_TEMPLATE_NOT_EXISTED("TASK_TEMPLATE_NOT_EXISTED", "任务模板不存在"),

    TASK_TEMPLATE_CAN_NOT_BE_NULL("TASK_TEMPLATE_CAN_NOT_BE_NULL", "任务模板不能为空"),

    TASK_NAME_CAN_NOT_BE_NULL("TASK_NAME_CAN_NOT_BE_NULL", "任务名不能为空"),

    TASK_NOT_EXISTED("TASK_NOT_EXISTED", "任务不存在"),

    /**
     * ******
     * Aird Related
     * *******
     */
    CREATE_FILE_FAILED("CREATE_FILE_FAILED", "创建文件失败"),

    FILE_CHUNK_UPLOAD_FAILED("FILE_CHUNK_UPLOAD_FAILED", "文件分片上传失败"),

    FILE_CHUNKS_MERGE_FAILED("FILE_CHUNKS_MERGE_FAILED", "文件分片合并失败"),

    FILE_CHUNK_ALREADY_EXISTED("FILE_CHUNK_ALREADY_EXISTED", "文件分片已存在"),

    FILE_CHUNK_NOT_EXISTED("FILE_CHUNK_NOT_EXISTED", "文件分片不存在"),

    /**
     * ******
     * Project
     * ******
     */
    PROJECT_ID_CANNOT_BE_EMPTY("PROJECT_ID_CANNOT_BE_EMPTY", "项目ID不能为空"),

    PROJECT_NAME_CANNOT_BE_EMPTY("PROJECT_NAME_CANNOT_BE_EMPTY", "项目名称不能为空"),

    PROJECT_NOT_EXISTED("PROJECT_NOT_EXISTED", "项目不能为空"),

    NO_RUN_UNDER_PROJECT("NO_RUN_UNDER_PROJECT", "项目下没有实验数据"),

    /**
     * ******
     * Protein
     * ******
     */
    PROTEIN_IDENTIFIER_CANNOT_BE_NULL("PROTEIN_IDENTIFIER_CANNOT_BE_NULL", "蛋白识别号不能为空"),

    OVERVIEWS_MUST_USE_THE_SAME_ANA_LIBRARY_ID("OVERVIEWS_MUST_USE_THE_SAME_ANA_LIBRARY_ID", "概览使用的标准库id必须是同一个"),


    /**
     * ******
     * User
     * *******
     */
    USER_ALREADY_EXISTED("USER_ALREADY_EXISTED", "该用户名已被注册"),

    OLD_PASSWORD_ERROR("OLD_PASSWORD_ERROR", "原密码错误"),

    NEW_PASSWORD_NOT_EQUALS_WITH_REPEAT_PASSWORD("NEW_PASSWORD_NOT_EQUALS_WITH_REPEAT_PASSWORD", "两次密码不一致"),

    USER_NOT_EXISTED("USER_NOT_EXISTED", "该用户不存在"),

    USER_NOT_LOGIN("USER_NOT_LOGIN", "用户未登录"),

    USERNAME_CANNOT_BE_USED("USERNAME_CANNOT_BE_USED", "该用户名不能被使用"),

    UNKNOWN_ACCOUNT("UNKNOWN_ACCOUNT", "未知的账户"),

    USERNAME_OR_PASSWORD_ERROR("USERNAME_OR_PASSWORD_ERROR", "未知的账户"),

    ACCOUNT_IS_LOCKED("ACCOUNT_IS_LOCKED", "账户已被锁定"),

    TRY_TOO_MANY_TIMES("TRY_TOO_MANY_TIMES", "尝试次数过多"),

    /**
     * ******
     * SQL-COMMON
     * *******
     */
    CONNECTION_URL_CANNOT_BE_EMPTY("CONNECTION_URL_CANNOT_BE_EMPTY", "数据库链接URL不能为空"),

    CONNECTION_FAILED("CONNECTION_FAILED", "数据库连接失败"),

    CONNECTION_SUCCESS("CONNECTION_SUCCESS", "数据库连接成功"),
    ;


    private String code = "";
    private String message = "";

    private static final long serialVersionUID = -799302222165012777L;

    /**
     * @param code
     * @param message
     */
    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message + "(" + code + ")";
    }

}
