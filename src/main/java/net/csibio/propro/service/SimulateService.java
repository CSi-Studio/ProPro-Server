package net.csibio.propro.service;

import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.PeptideDO;

import java.util.List;

public interface SimulateService {
    void predictFragment(String libraryId, String spModel, boolean iso);

    List<FragmentInfo> predictFragment(String peptideId, String spModel, boolean iso, int limit);

    List<FragmentInfo> singlePredictFragment(PeptideDO peptideDO, String spModel, boolean iso);
}
