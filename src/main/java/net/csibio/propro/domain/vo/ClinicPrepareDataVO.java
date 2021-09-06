package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.common.IdNameAlias;
import net.csibio.propro.domain.bean.overview.OverviewV1;
import net.csibio.propro.domain.db.ProjectDO;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ClinicPrepareDataVO {

    ProjectDO project;
    IdName insLib;
    IdName anaLib;
    IdName method;
    //    Set<String> insProteins;
    Set<String> anaProteins;
    List<IdNameAlias> expList;
    Map<String, List<OverviewV1>> overviewMap;
}