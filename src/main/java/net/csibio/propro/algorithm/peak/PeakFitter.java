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

//        String bestIon = coord.getFragments().get(0).getCutInfo();

        String bestIon = peakGroup.getBestIon();
        if (bestIon == null) {
            bestIon = coord.getFragments().get(0).getCutInfo();
        }
        Double bestIonIntensity = peakGroup.getIonIntensity().get(bestIon);
        if (bestIonIntensity == null || bestIonIntensity.isNaN()) {
            return;
        }

        Map<String, Double> libIntMap = coord.getFragments().stream().collect(Collectors.toMap(FragmentInfo::getCutInfo, FragmentInfo::getIntensity));
        double bestLibIonsIntensity = libIntMap.get(bestIon);
        AtomicDouble fitSum = new AtomicDouble(0d);

        for (Map.Entry<String, Double> entry : libIntMap.entrySet()) {
            if (entry.getKey().equals(bestIon)) {
                fitSum.getAndAdd(bestIonIntensity);
            } else {
                double libRatio = libIntMap.get(entry.getKey()) / bestLibIonsIntensity;
                fitSum.getAndAdd(bestIonIntensity * libRatio); //直接按照库中比例进行拟合
            }
        }

        peakGroup.setFitIntSum(fitSum.get());
    }
}
