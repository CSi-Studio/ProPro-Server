package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.domain.bean.common.IdName;

import java.util.List;

@Data
public class PrepareAnalyzeVO {
    String projectName;

    List<IdName> insLibs;
    List<IdName> anaLibs;
    List<IdName> methods;

    //项目默认的内标库,目标库和方法包ID
    String insLibId;
    String anaLibId;
    String methodId;
}
