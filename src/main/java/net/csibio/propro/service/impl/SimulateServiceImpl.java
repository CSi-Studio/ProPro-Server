package net.csibio.propro.service.impl;

import net.csibio.propro.algorithm.simulator.Parameters1;
import net.csibio.propro.algorithm.simulator.Parameters2;
import net.csibio.propro.algorithm.simulator.Peptide;
import net.csibio.propro.algorithm.simulator.Simulator;
import net.csibio.propro.constants.constant.CutInfoConst;
import net.csibio.propro.constants.constant.SpModelConstant;
import net.csibio.propro.dao.PeptideDAO;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.SimulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("SimulateService")
public class SimulateServiceImpl implements SimulateService {

    @Autowired
    PeptideDAO peptideDAO;
    @Autowired
    PeptideService peptideService;

    @Override
    public void predictFragment(LibraryDO library, String spModel, boolean iso) {
        List<PeptideDO> peptideDOList = peptideDAO.getAllByLibraryId(library.getId());
        for (PeptideDO peptideDO : peptideDOList) {
            //The model only supports peptides with 2 charges
            if (peptideDO.getCharge() != 2) {
                continue;
            }
            peptideDO.getFragments().addAll(predictFragment(peptideDO.getSequence(), spModel, iso));
            peptideService.update(peptideDO);
        }
    }


    @Override
    public List<FragmentInfo> predictFragment(String sequence, String spModel, boolean iso, int limit) {
        Set<FragmentInfo> fragmentsSet = predictFragment(sequence, spModel, iso);
        List<FragmentInfo> fragments = new ArrayList<>(fragmentsSet);
        fragments.sort(Comparator.comparing(FragmentInfo::getIntensity).reversed());
        if (limit < fragments.size()) {
            return fragments.subList(0, limit);
        } else {
            return fragments;
        }
    }

    @Override
    public Set<FragmentInfo> predictFragment(String sequence, String spModel, boolean iso) {
        Peptide peptide = new Peptide(sequence, 2);
        Simulator simu = new Simulator(peptide, spModel.equals(SpModelConstant.HCD) ? new Parameters1() : new Parameters2());
        float[][] peakGroup = iso ? simu.getYisoList() : simu.getYList();
        Set<FragmentInfo> fragments = new HashSet<>();
        for (int i = 0; i < peakGroup.length; i++) {
            FragmentInfo fragment = new FragmentInfo();
            fragment.setMz((double) peakGroup[i][0]);
            fragment.setIntensity((double) peakGroup[i][1]);
            fragment.setPredict(true);
            fragment.setCharge(1);

            int cuts = iso ? 3 : 1;
            fragment.setCutInfo(CutInfoConst.Y_ION + (i / cuts + 1));
            fragment.setAnnotations(CutInfoConst.Y_ION + (i / cuts + 1));
            fragments.add(fragment);
        }
        return fragments;
    }

    @Override
    public Set<FragmentInfo> singlePredictFragment(PeptideDO peptideDO, String spModel, boolean iso) {
        if (peptideDO.getCharge() != 2) {
            return null;
        }
        Set<FragmentInfo> fragments = predictFragment(peptideDO.getSequence(), spModel, iso);
        return fragments;
    }


}

