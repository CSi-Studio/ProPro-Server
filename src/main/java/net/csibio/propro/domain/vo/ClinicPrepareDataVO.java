package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameAlias;
import net.csibio.propro.domain.bean.method.Method;
import net.csibio.propro.domain.bean.overview.Overview4Clinic;
import net.csibio.propro.domain.db.ProjectDO;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ClinicPrepareDataVO {

    ProjectDO project;
    IdName insLib;
    IdName anaLib;
    Long peptideCount;
    Long proteinCount;
    Method method;
    Set<String> proteins;
    List<IdNameAlias> runList;
    Map<String, List<Overview4Clinic>> overviewMap; //key为runId
}
