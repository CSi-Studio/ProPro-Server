package net.csibio.propro.algorithm.peak;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.utils.PeptideUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component("peakIdentifier")
public class PeakIdentifier {

    @Autowired
    PeptideService peptideService;
    @Autowired
    BlockIndexService blockIndexService;

    public List<SelectedPeakGroup> identify(String runId, List<DataScore> dataList, Map<String, SelectedPeakGroup> selectedDataMap, List<WindowRange> ranges, String libraryId, double minTotalScore) {
        List<SelectedPeakGroup> selectedList = new ArrayList<>();
        Map<String, DataScore> dataMap = dataList.stream().filter(data -> !data.getDecoy()).collect(Collectors.toMap(DataScore::getPeptideRef, Function.identity()));
        for (WindowRange range : ranges) {
            TreeMap<Double, List<SelectedPeakGroup>> rtMap = new TreeMap<>();
            BlockIndexDO index = blockIndexService.getOne(runId, range.getMz());
            for (Float rt : index.getRts()) {
                rtMap.put((double) rt, new ArrayList<>());
            }
            Map<String, PeptideDO> peptideMap = peptideService.getAll(new PeptideQuery(libraryId).setMzStart(range.getStart()).setMzEnd(range.getEnd())).stream().collect(Collectors.toMap(PeptideDO::getPeptideRef, Function.identity()));
            for (PeptideDO peptide : peptideMap.values()) {
                SelectedPeakGroup selectedPeakGroup = selectedDataMap.get(peptide.getPeptideRef());
                if (selectedPeakGroup != null && selectedPeakGroup.getFdr() <= 0.01) {
                    if (rtMap.get(selectedPeakGroup.getApexRt()) != null) {
                        rtMap.get(selectedPeakGroup.getApexRt()).add(selectedPeakGroup);
                    } else {
                        ArrayList<SelectedPeakGroup> peakGroupList = new ArrayList<SelectedPeakGroup>();
                        peakGroupList.add(selectedPeakGroup);
                        rtMap.put(selectedPeakGroup.getApexRt(), peakGroupList);
                    }
                }
            }
            List<List<SelectedPeakGroup>> peakGroupListCollection = new ArrayList<>(rtMap.values());
            for (int i = 0; i < peakGroupListCollection.size() - 2; i++) {
                List<SelectedPeakGroup> peakGroupList = new ArrayList<>();
                peakGroupList.addAll(peakGroupListCollection.get(i));
                peakGroupList.addAll(peakGroupListCollection.get(i + 1));
                peakGroupList.addAll(peakGroupListCollection.get(i + 2));
                for (int a = 0; a < peakGroupList.size(); a++) {
                    for (int b = a + 1; b < peakGroupList.size(); b++) {
                        SelectedPeakGroup selectedPeakGroupA = peakGroupList.get(a);
                        SelectedPeakGroup selectedPeakGroupB = peakGroupList.get(b);
                        PeptideDO peptideA = peptideMap.get(selectedPeakGroupA.getPeptideRef());
                        PeptideDO peptideB = peptideMap.get(selectedPeakGroupB.getPeptideRef());
                        int minLength = Math.min(peptideA.getSequence().length(), peptideB.getSequence().length());
                        if (PeptideUtil.similar(peptideA, peptideB, minLength <= 8 ? 5 : 6)) {
                            //如果是两个相邻干扰峰,开始处理
                            DataScore dataA = dataMap.get(peptideA.getPeptideRef());
                            DataScore dataB = dataMap.get(peptideB.getPeptideRef());
                            //如果两组分数相同
                            if (selectedPeakGroupA.getTotalScore().equals(selectedPeakGroupB.getTotalScore())) {
                                log.info("两组的得分完全一样");
                                continue; //如果两组分数完全相同,那么不做处理
                            }
                            //选择得分较小的一组
                            if (selectedPeakGroupA.getTotalScore() < selectedPeakGroupB.getTotalScore()) {
                                for (PeakGroup peakGroup : dataA.getPeakGroupList()) {
                                    if (peakGroup.getApexRt().equals(selectedPeakGroupA.getApexRt())) {
                                        peakGroup.setNotMine(true);
                                    }
                                }
                            } else {
                                for (PeakGroup peakGroup : dataB.getPeakGroupList()) {
                                    if (peakGroup.getApexRt().equals(selectedPeakGroupB.getApexRt())) {
                                        peakGroup.setNotMine(true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return selectedList;
    }
}
