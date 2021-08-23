package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.domain.BaseDO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Data
@Document(collection = "library")
public class LibraryDO extends BaseDO {

    private static final long serialVersionUID = -3258829839160856625L;

    @Id
    String id;

    @Indexed(unique = true)
    String name;

    String filePath;

    String fastaName;

    /**
     * @see net.csibio.propro.constants.enums.LibraryType
     */
    String type;

    String description;

    /**
     * 伪肽段的生成算法
     */
    String generator;

    /**
     * 关于本库的统计数据
     */
    Map<String, Object> statistic;

    /**
     * 建样组织
     */
    @Indexed
    Set<String> organism = new HashSet<>();

    Date createDate;

    Date lastModifiedDate;

    /**
     * KV值
     */
    HashMap featureMap;

    public LibraryDO() {
    }

    public LibraryDO(String libraryName, String libraryType) {
        this.name = libraryName;
        this.type = libraryType;
    }

    public LibraryDO(String libraryName, String libraryType, String filePath) {
        this.name = libraryName;
        this.type = libraryType;
        this.filePath = filePath;
    }

    public void setOrganismStr(String organismStr) {
        this.setOrganism(organismStr != null ? new HashSet<>(Arrays.asList(organismStr.split(SymbolConst.COMMA))) : null);
    }
}
