package net.csibio.propro.service.impl;

import net.csibio.propro.algorithm.simulator.*;
import net.csibio.propro.constants.constant.SpModelConstant;
import net.csibio.propro.dao.PeptideDAO;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.SimulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service("SimulateService")
public class SimulateServiceImpl implements SimulateService {
    @Autowired
    PeptideDAO peptideDAO;
    @Autowired
    PeptideService peptideService;

    @Override
    public void predictFragment(String libraryId, String spModel, boolean iso) {
        List<PeptideDO> peptideDOList = peptideDAO.getAllByLibraryId(libraryId);
        if (spModel.equals(SpModelConstant.HCD)) {
            staticValue.parameter = new Parameters1();
        } else {
            staticValue.parameter = new Parameters2();
        }
        for (PeptideDO peptideDO : peptideDOList
        ) {
            //The model only supports peptides with 2 charges
            if (peptideDO.getCharge() != 2) {
                continue;
            }
            String sequence = peptideDO.getSequence();
            Peptide peptide = new Peptide(sequence, 2);
            Simulator simu = new Simulator(peptide);
            float[][] peak_group;
            Set<FragmentInfo> fragments = peptideDO.getFragments();
            if (iso) {
                peak_group = simu.getYisoList();
                for (int i = 0; i <= peak_group.length - 1; i++) {
                    FragmentInfo fragment = new FragmentInfo();
                    fragment.setMz((double) peak_group[i][0]);
                    fragment.setIntensity((double) peak_group[i][1]);
                    fragment.setPredict(true);
                    fragment.setCharge(1);
                    fragment.setCutInfo("y" + (i/3 + 1));
                    fragment.setAnnotations("y" + (i/3 + 1));
                    fragments.add(fragment);
                }
            } else {
                peak_group = simu.getYList();
                for (int i = 0; i <= peak_group.length - 1; i++) {
                    FragmentInfo fragment = new FragmentInfo();
                    fragment.setMz((double) peak_group[i][0]);
                    fragment.setIntensity((double) peak_group[i][1]);
                    fragment.setPredict(true);
                    fragment.setCharge(1);
                    fragment.setCutInfo("y" + (i + 1));
                    fragment.setAnnotations("y" + (i + 1));
                    fragments.add(fragment);
                }
            }

            peptideDO.setFragments(fragments);
            peptideService.update(peptideDO);
        }
    }

    @Override
    public List<FragmentInfo> predictFragment(PeptideDO peptideDO, String spModel, boolean iso, int limit) {
        if (spModel.equals(SpModelConstant.HCD)) {
            staticValue.parameter = new Parameters1();
        } else {
            staticValue.parameter = new Parameters2();
        }
        String sequence = peptideDO.getSequence();
        Peptide peptide = new Peptide(sequence, 2);
        Simulator simu = new Simulator(peptide);
        float[][] peak_group;
        if (iso) {
            peak_group = simu.getYisoList();
        } else {
            peak_group = simu.getYList();
        }
        List<FragmentInfo> fragments = new ArrayList<>();
        for (int i = 0; i <= peak_group.length - 1; i++) {
            FragmentInfo fragment = new FragmentInfo();
            fragment.setMz((double) peak_group[i][0]);
            fragment.setIntensity((double) peak_group[i][1]);
            fragment.setPredict(true);
            fragment.setCharge(1);
            fragment.setCutInfo("y" + (i + 1));
            fragment.setAnnotations("y" + (i + 1));
            fragments.add(fragment);
        }
        fragments.sort(Comparator.comparing(FragmentInfo::getIntensity).reversed());
        if (limit < fragments.size()) {
            return fragments.subList(0, limit);
        } else {
            return fragments;
        }
    }

    @Override
    public Set<FragmentInfo> singlePredictFragment(PeptideDO peptideDO, String spModel, boolean iso) {
        if (spModel.equals(SpModelConstant.HCD)) {
            staticValue.parameter = new Parameters1();
        } else {
            staticValue.parameter = new Parameters2();
        }
        if (peptideDO.getCharge() != 2) {
            return null;
        }

        Peptide peptide = new Peptide(peptideDO.getSequence(), 2);
        Simulator simu = new Simulator(peptide);
        float[][] peak_group;
        if (iso) {
            peak_group = simu.getYisoList();
        } else {
            peak_group = simu.getYList();
        }
        Set<FragmentInfo> fragments = peptideDO.getFragments();
        for (int i = 0; i <= peak_group.length - 1; i++) {
            FragmentInfo fragment = new FragmentInfo();
            fragment.setMz((double) peak_group[i][0]);
            fragment.setIntensity((double) peak_group[i][1]);
            fragment.setPredict(true);
            fragment.setCharge(1);
            fragment.setCutInfo("y" + (i + 1));
            fragment.setAnnotations("y" + (i + 1));
            fragments.add(fragment);
        }
        return fragments;

    }
}
