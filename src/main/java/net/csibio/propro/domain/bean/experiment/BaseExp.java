package net.csibio.propro.domain.bean.experiment;

import lombok.Data;
import net.csibio.aird.bean.Compressor;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.constants.constant.SuffixConst;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.utils.RepositoryUtil;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

@Data
public class BaseExp {

    /**
     * 实验的ID
     */
    String id;

    /**
     * 实验类型
     */
    String type;

    /**
     * 实验名称
     */
    String name;

    /**
     * 标注,用于数据分类学习
     */
    String label;

    /**
     * 标签,用于多维度的分类学习
     */
    List<String> tags;

    /**
     * 实验别名
     */
    String alias;

    /**
     * 项目名称
     */
    String projectName;

    //原始文件的大小,单位byte
    Long vendorFileSize;

    //Aird文件大小,单位byte
    Long airdSize;

    //Aird索引文件的大小,单位byte
    Long airdIndexSize;

    //[核心字段]数组压缩策略
    List<Compressor> compressors;

    //核心字段, Swath窗口列表
    List<WindowRange> windowRanges;

    /**
     * 实验的irt结果
     */
    IrtResult irt;

    public String getAirdPath() {
        return FilenameUtils.concat(RepositoryUtil.getProjectRepo(projectName), name) + SuffixConst.AIRD;
    }

    public String getAirdIndexPath() {
        return FilenameUtils.concat(RepositoryUtil.getProjectRepo(projectName), name) + SuffixConst.JSON;
    }

}
