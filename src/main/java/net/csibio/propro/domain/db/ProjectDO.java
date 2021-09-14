package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Document(collection = "project")
public class ProjectDO extends BaseDO {

    @Id
    String id;

    /**
     * 项目名称,唯一值
     */
    @Indexed(unique = true)
    String name;

    /**
     * 项目别名
     */
    @Indexed
    String alias;

    /**
     * 采集类型
     *
     * @see net.csibio.propro.constants.enums.ProjectType
     * DIA, PRM
     */
    @Indexed
    String type;

    @Indexed
    String group;
    
    /**
     * 项目负责人名称
     */
    String owner;

    /**
     * 使用的方法集Id
     */
    String methodId;

    /**
     * 该批次使用的内标库Id
     */
    String insLibId;

    /**
     * 该批次使用的标准库Id
     */
    String anaLibId;

    /**
     * 项目标签
     */
    Set<String> tags;

    /**
     * 关于本项目的统计数据
     */
    Map<String, Object> statistic;

    /**
     * 项目描述
     */
    String description;

    /**
     * KV值
     */
    HashMap featureMap;

    Date createDate;

    Date lastModifiedDate;
}
