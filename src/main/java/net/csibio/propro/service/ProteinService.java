package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.ProteinDO;
import net.csibio.propro.domain.query.ProteinQuery;

import java.io.InputStream;
import java.util.List;

public interface ProteinService extends BaseService<ProteinDO, ProteinQuery> {

    List<ProteinDO> importFromLocalFasta(InputStream inputStream, String fileName, boolean review);

    Result<List<ProteinDO>> importFromFasta(InputStream inputStream, String fileName, boolean review, int min, int max);

    void proteinToPeptide(String libraryId, List<ProteinDO> list, int min, int max, String spModel, Boolean isotope);

}
