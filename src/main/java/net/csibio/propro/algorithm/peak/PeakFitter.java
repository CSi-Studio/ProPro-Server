package net.csibio.propro.algorithm.peak;

import com.google.common.util.concurrent.AtomicDouble;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("peakFitter")
public class PeakFitter {

    public void fit(PeakGroup peakGroup, PeptideCoord coord, boolean removeIons) {

        FragmentInfo bestLibIon = coord.getFragments().get(0);
        List<FragmentInfo> top3Fragments = coord.getFragments().subList(0, 3);
        Double bestIonIntensity = peakGroup.getIonIntensity().get(bestLibIon.getCutInfo());
        if (bestIonIntensity == null) {
            return;
        }

        Map<String, Double> libIntMap = top3Fragments.stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, FragmentInfo::getIntensity));
        double bestLibIonsIntensity = libIntMap.get(bestLibIon.getCutInfo());
        AtomicDouble fitSum = new AtomicDouble(0d);

        for (Map.Entry<String, Double> entry : libIntMap.entrySet()) {
            if (entry.getKey().equals(bestLibIon.getCutInfo())) {
                fitSum.getAndAdd(bestIonIntensity);
            } else {
                double libRatio = libIntMap.get(entry.getKey()) / bestLibIonsIntensity;
                Double entryIntensity = peakGroup.getIonIntensity().get(entry.getKey());
                if (entryIntensity == null) {
                    entryIntensity = 0d;
                }
                if (entryIntensity > bestIonIntensity) {
                    fitSum.getAndAdd(bestIonIntensity * libRatio);
                } else {
                    fitSum.getAndAdd(entryIntensity);
                }
            }
        }

//        for (String disturbIon : disturbIons) {
//            double libRatio = libIntMap.get(disturbIon) / bestLibIonsIntensity;
//            fitSum = fitSum + peakGroup.getIonIntensity().get(bestIon.getCutInfo()) * libRatio - peakGroup.getIonIntensity().get(disturbIon);
//        }
        peakGroup.setFitIntSum(fitSum.get());
    }
}
