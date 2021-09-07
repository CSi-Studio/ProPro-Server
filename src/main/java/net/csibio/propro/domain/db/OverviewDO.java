package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * 批处理分析结果概览,每一次分析可以包含多个实验文件
 */
@Data
@Document(collection = "overview")
public class OverviewDO extends BaseDO {

    @Id
    String id;

    @Indexed
    String projectId;

    /**
     * 分析的实验对象
     */
    @Indexed
    String expId;

    Boolean defaultOne = false;
    /**
     * 分析名称
     */
    String name;

    /**
     * 分析的实验对象名称
     */
    String expName;

    /**
     * 项目标签
     */
    Set<String> tags;

    /**
     * 内标库ID
     */
    String insLibId;

    /**
     * 标准库ID
     */
    String anaLibId;

    /**
     * 实验类型
     */
    String type;

    /**
     * 分析参数快照
     */
    AnalyzeParams params;

    /**
     * 分析实验的创建时间
     */
    Date createDate;

    /**
     * 分析实验的最后一次修改时间
     */
    Date lastModifiedDate;

    /**
     * 最终计算所得的子分数的权重,LDA算法才有
     */
    HashMap<String, Double> weights = new HashMap<>();

    /**
     * 关于本次分析的统计数据
     */
    HashMap<String, Object> statistic = new HashMap<>();
    
    /**
     * 备忘录
     */
    String note;

    /**
     * KV值
     */
    HashMap featureMap;
}
