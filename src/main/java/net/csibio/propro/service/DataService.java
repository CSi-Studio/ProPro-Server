package net.csibio.propro.service;

import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.DataQuery;
import net.csibio.propro.domain.vo.ExpDataVO;

public interface DataService extends BaseMultiService<DataDO, DataQuery> {

    ExpDataVO fetchEicByPeptideRef(ExperimentDO exp, PeptideCoord coord, AnalyzeParams params);
}
