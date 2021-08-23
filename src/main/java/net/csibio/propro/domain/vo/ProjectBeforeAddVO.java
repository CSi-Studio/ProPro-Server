package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameType;

import java.util.List;

@Data
public class ProjectBeforeAddVO {

    List<String> unloads;

    List<IdNameType> anaLibs;

    List<IdNameType> insLibs;

    List<IdName> methods;
}
