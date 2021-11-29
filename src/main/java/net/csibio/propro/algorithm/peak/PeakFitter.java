package net.csibio.propro.algorithm.peak;

import com.google.common.util.concurrent.AtomicDouble;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component("peakFitter")
public class PeakFitter {

    public void fit(PeakGroup peakGroup, PeptideCoord coord) {

        FragmentInfo bestLibIon = coord.getFragments().get(0);
        //TODO 陆妙善 如果最强碎片被离子干扰了,就会带来大幅度的定量偏高
        Double bestIonIntensity = peakGroup.getIonIntensity().get(bestLibIon.getCutInfo());
        if (bestIonIntensity == null) {
            return;
        }

        Map<String, Double> libIntMap = coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, FragmentInfo::getIntensity));
        double bestLibIonsIntensity = libIntMap.get(bestLibIon.getCutInfo());
        AtomicDouble fitSum = new AtomicDouble(0d);

        for (Map.Entry<String, Double> entry : libIntMap.entrySet()) {
            if (entry.getKey().equals(bestLibIon.getCutInfo())) {
                fitSum.getAndAdd(bestIonIntensity);
            } else {
                double libRatio = libIntMap.get(entry.getKey()) / bestLibIonsIntensity;
                fitSum.getAndAdd(bestIonIntensity * libRatio); //直接按照库中比例进行拟合
            }
        }

        peakGroup.setFitIntSum(fitSum.get());
    }
}
