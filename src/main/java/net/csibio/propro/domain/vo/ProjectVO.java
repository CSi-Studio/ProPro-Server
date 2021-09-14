package net.csibio.propro.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Data
public class ProjectVO {

    String id;

    /**
     * 项目名称,唯一值
     */
    String name;

    /**
     * 分组信息
     */
    String group;
    
    /**
     * 项目别名
     */
    String alias;

    /**
     * 采集类型
     *
     * @see net.csibio.propro.constants.enums.ProjectType
     * DIA, PRM
     */
    String type;

    /**
     * 项目负责人名称
     */
    String owner;

    /**
     * 使用的方法集Id
     */
    String methodId;

    /**
     *
     */
    String methodName;

    /**
     * 该批次使用的内标库Id
     */
    String insLibId;

    String insLibName;

    /**
     * 该批次使用的标准库Id
     */
    String anaLibId;

    String anaLibName;

    /**
     * 项目标签
     */
    Set<String> tags;

    /**
     * 关于本项目的统计数据
     */
    Map<String, Object> statistic;

    /**
     * 项目包含的实验数目
     */
    Long expCount;

    /**
     * 项目包含的鉴定数目
     */
    Long overviewCount;

    /**
     * 项目描述
     */
    String description;

    Date createDate;

    Date lastModifiedDate;
}
