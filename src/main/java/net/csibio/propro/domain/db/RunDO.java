package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.aird.bean.*;
import net.csibio.propro.constants.constant.SuffixConst;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.utils.RepositoryUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "run")
public class RunDO extends BaseDO {

    private static final long serialVersionUID = -3258829839160856625L;

    @Id
    String id;

    @Indexed
    String projectId;

    @Indexed
    String projectName;

    //分组,用于单维度分类学习
    String group;

    //标签,用于多维度的分类学习
    List<String> tags;

    //必填,实验名称,与Aird文件同名
    String name;

    //别名,默认为空
    String alias;

    //DIA_SWATH, PRM, SCANNING_SWATH @see RunType
    String type;

    //CID, HCD, ETD,详情见FragMode类
    String fragMode;

    //Aird文件大小,单位byte
    Long airdSize;

    //Aird索引文件的大小,单位byte
    Long airdIndexSize;

    //原始文件的大小,单位byte
    Long vendorFileSize;

    //实验的描述
    String description;

    //仪器设备信息
    List<Instrument> instruments;

    //处理的软件信息
    List<Software> softwares;

    //处理前的文件信息
    List<ParentFile> parentFiles;

    //[核心字段]数组压缩策略
    List<Compressor> compressors;

    //核心字段, Swath窗口列表
    List<WindowRange> windowRanges;

    //实验的创建日期
    Date createDate;

    //最后修改日期
    Date lastModifiedDate;

    //irt校验结果
    IrtResult irt;

    //转byte时的编码顺序,由于C#默认采用LITTLE_ENDIAN,Aird文件由Propro-Client(C#端)转换而来,因此也采用LITTLE_ENDIAN的编码
    String features;

    public Compressor fetchCompressor(String target) {
        for (Compressor c : compressors) {
            if (c.getTarget().equals(target)) {
                return c;
            }
        }
        return null;
    }

    public String getAirdPath() {
        return FilenameUtils.concat(RepositoryUtil.getProjectRepo(projectName), name) + SuffixConst.AIRD;
    }

    public String getAirdIndexPath() {
        return FilenameUtils.concat(RepositoryUtil.getProjectRepo(projectName), name) + SuffixConst.JSON;
    }
}
