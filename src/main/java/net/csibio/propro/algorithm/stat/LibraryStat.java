package net.csibio.propro.algorithm.stat;

import net.csibio.propro.domain.bean.common.StrIntPairs;
import net.csibio.propro.domain.bean.peptide.FragmentGroup;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LibraryStat {

    @Autowired
    PeptideService peptideService;

    public long peptideCount(LibraryDO library) {
        return peptideService.count(new PeptideQuery(library.getId()));
    }

    public long proteinCount(LibraryDO library) {
        return library.getProteins().size();
    }

    public long fragmentCount(List<FragmentGroup> peptideList) {
        return peptideList.stream().mapToLong(fragmentGroup -> fragmentGroup.getFragments().size()).sum();
    }

    /**
     * @param peptideList
     * @param slice       一般设置为20
     */
    public StrIntPairs mzDistList(List<FragmentGroup> peptideList, int slice) {
        peptideList = peptideList.stream().sorted(Comparator.comparing(FragmentGroup::getMz)).collect(Collectors.toList());
        int maxMz = peptideList.get(peptideList.size() - 1).getMz().intValue();
        int minMz = peptideList.get(0).getMz().intValue();
        int maxRange = (maxMz / 100 + 1) * 100;
        int minRange = (minMz / 100) * 100;
        int stage = (maxRange - minRange) / slice;
        int add = 0;
        int temp = 0;
        String[] rangeArray = new String[slice];
        int[] countArray = new int[slice];

        for (int i = 0; i < slice; i++) {
            add = minRange + (i + 1) * stage;
            temp = minRange + i * stage;
            int count = 0;
            for (FragmentGroup peptide : peptideList) {
                if (peptide.getMz() > temp && peptide.getMz() <= add) {
                    count = count + 1;
                }
            }
            rangeArray[i] = temp + "-" + add;
            countArray[i] = count;
        }
        return new StrIntPairs(rangeArray, countArray);
    }

    /**
     * @param peptideList
     * @param slice       一般设置为20
     */
    public StrIntPairs rtDistList(List<FragmentGroup> peptideList, int slice) {
        peptideList = peptideList.stream().sorted(Comparator.comparing(FragmentGroup::getRt)).collect(Collectors.toList());
        int maxRt = peptideList.get(peptideList.size() - 1).getRt().intValue();
        int minRt = peptideList.get(0).getRt().intValue();
        int maxRange = (maxRt / 100 + 1) * 100;
        int minRange;
        if (minRt < 0 && minRt > -100) {
            minRange = -100;
        } else {
            minRange = (minRt / 100) * 100;
        }

        int stage = (maxRange - minRange) / slice;
        int add = 0;
        int temp = 0;
        String[] rangeArray = new String[slice];
        int[] countArray = new int[slice];

        for (int i = 0; i < slice; i++) {
            add = minRange + (i + 1) * stage;
            temp = minRange + i * stage;
            int count = 0;
            for (FragmentGroup peptide : peptideList) {
                if (peptide.getRt() > temp && peptide.getRt() <= add) {
                    count = count + 1;
                }
            }
            rangeArray[i] = temp + "-" + add;
            countArray[i] = count;
        }
        return new StrIntPairs(rangeArray, countArray);
    }
}
