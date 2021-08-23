package net.csibio.propro.algorithm.decoy.repeatCount;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RepeatCount {

    @Autowired
    PeptideService peptideService;

    /**
     * 统计重复率
     *
     * @param libraryId
     * @return
     */
    public String repeatCount(String libraryId) {
        log.info("开始从数据库抓取数据");
        List<StatPeptide> peptideList = (List<StatPeptide>) peptideService.getAll(new PeptideQuery(libraryId), StatPeptide.class);
        log.info("Total Peptide:" + peptideList.size());

        peptideList.parallelStream().forEach(StatPeptide::trans);
        peptideList = peptideList.stream().sorted(Comparator.comparing(StatPeptide::getRt)).collect(Collectors.toList());

        // allByLibraryId = allByLibraryId.stream().sorted(Comparator.comparing(PeptideDO::getRt)).collect(Collectors.toList());
        double range = peptideList.get(peptideList.size() - 1).getRt() - peptideList.get(0).getRt();
        double swath5 = range * 0.05;
        double swath10 = range * 0.1;
        AtomicInteger decoyForReal5 = new AtomicInteger();
        AtomicInteger decoyForReal10 = new AtomicInteger();

        AtomicLong run = new AtomicLong();
        List<StatPeptide> finalPeptideList = peptideList;
        peptideList.parallelStream().forEach(peptide -> {
            run.getAndIncrement();
            if (run.get() % 1000 == 0) {
                log.info(run + " Peptides Finished");
            }
            int index = finalPeptideList.indexOf(peptide);
            Set<Float> decoyMzList = peptide.getDecoyMzList();
            Double rt = peptide.getRt();
            for (int j = index; j < finalPeptideList.size(); j++) {
                StatPeptide current = finalPeptideList.get(j);
                Set<Float> mzList = current.getMzList();
                if (current.getRt() > rt + swath10) {
                    break;
                }
                if (current.getRt() <= rt + swath5) {
                    for (Float decoyMz : decoyMzList) {
                        if (mzList.contains(decoyMz)) {
                            decoyForReal5.set(decoyForReal5.get() + 1);
                        }
                    }
                } else if (current.getRt() > rt + swath5) {
                    for (Float decoyMz : decoyMzList) {
                        if (mzList.contains(decoyMz)) {
                            decoyForReal10.set(decoyForReal10.get() + 1);
                        }
                    }
                }
            }
        });
        String s = "decoyForReal5=" + decoyForReal5 + ",decoyForReal10=" + decoyForReal10;
        log.info(s);
        return s;

    }

    @Data
    public class StatPeptide {
        Set<FragmentInfo> fragments;
        Set<FragmentInfo> decoyFragments;
        Set<Float> mzList = new HashSet<>();
        Set<Float> decoyMzList = new HashSet<>();
        Double rt;

        public void trans() {
            fragments.forEach(value -> {
                mzList.add(value.getMz().floatValue() * 1000 / 1000);
            });
            decoyFragments.forEach(value -> {
                decoyMzList.add(value.getMz().floatValue() * 1000 / 1000);
            });
            fragments = null;
            decoyFragments = null;
        }
    }
}
