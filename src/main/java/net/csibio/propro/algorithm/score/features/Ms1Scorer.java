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
        Double[] selfInts = peakGroup.getSelfInts();
        Double[] bestIonInts = peakGroup.getIonHullInt().get(peakGroup.getBestIon());
        if (bestIonInts == null || ms1Ints == null) {
            peakGroup.put(ScoreType.MS1, -1d, scoreTypes);
            return;
        }

        double[] dBestIonInts = ArrayUtil.toPrimitive(bestIonInts);
        double[] dMs1Ints = ArrayUtil.toPrimitive(ms1Ints);
        double[] dSelfInts = ArrayUtil.toPrimitive(selfInts);
        PeakFindingOptions options = new PeakFindingOptions();
        options.fillParams();
        double[] rts = ArrayUtil.toPrimitive(peakGroup.getIonHullRt());
        DoublePairs pairs = smoother.doSmooth(new DoublePairs(rts, dBestIonInts), options);
        double[] bestIonSmoothEic = pairs.y();
        Double ms1Pearson = new PearsonsCorrelation().correlation(bestIonSmoothEic, dMs1Ints);
        if (ms1Pearson.isNaN()) {
            ms1Pearson = -1d;
        }
        peakGroup.put(ScoreType.MS1, ms1Pearson, scoreTypes);

        Double selfPearson = new PearsonsCorrelation().correlation(bestIonSmoothEic, dSelfInts);
        if (selfPearson.isNaN()) {
            selfPearson = -1d;
        }
        peakGroup.put(ScoreType.SELF, selfPearson, scoreTypes);

        Double selfMs1Pearson = new PearsonsCorrelation().correlation(dMs1Ints, dSelfInts);
        if (selfMs1Pearson.isNaN()) {
            selfMs1Pearson = -1d;
        }
        peakGroup.put(ScoreType.MS1_SELF, selfMs1Pearson, scoreTypes);
    }

}
