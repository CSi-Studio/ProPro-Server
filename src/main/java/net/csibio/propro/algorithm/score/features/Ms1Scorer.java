package net.csibio.propro.algorithm.score.features;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.peak.Smoother;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.common.DoublePairs;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.options.PeakFindingOptions;
import net.csibio.propro.utils.ArrayUtil;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("ms1Scorer")
@Slf4j
public class Ms1Scorer {

    @Autowired
    Smoother smoother;

    /**
     * ms1和best ions的pearson相关系数
     *
     * @param peakGroup
     * @return
     */
    public void calcPearsonScore(PeakGroup peakGroup, List<String> scoreTypes) {
        Double[] ms1Ints = peakGroup.getMs1Ints();
        Double[] bestIonInts = peakGroup.getIonHullInt().get(peakGroup.getBestIon());
        if (bestIonInts == null || ms1Ints == null) {
            peakGroup.put(ScoreType.MS1Pearson, -1d, scoreTypes);
            return;
        }

        PeakFindingOptions options = new PeakFindingOptions();
        options.fillParams();
        double[] rts = ArrayUtil.toPrimitive(peakGroup.getIonHullRt());
        DoublePairs pairs = smoother.doSmooth(new DoublePairs(rts, ArrayUtil.toPrimitive(bestIonInts)), options);
        double[] bestIonSmoothEic = pairs.y();
        Double pearson = new PearsonsCorrelation().correlation(bestIonSmoothEic, ArrayUtil.toPrimitive(ms1Ints));
        if (pearson.isNaN()) {
            pearson = -1d;
        }
        peakGroup.put(ScoreType.MS1Pearson, pearson, scoreTypes);
    }

}
