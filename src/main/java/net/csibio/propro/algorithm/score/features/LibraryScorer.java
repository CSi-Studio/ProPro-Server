package net.csibio.propro.algorithm.score.features;

import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.options.DeveloperParams;
import net.csibio.propro.utils.MathUtil;
import net.csibio.propro.utils.ScoreUtil;
import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * scores.library_corr
 * scores.library_norm_manhattan
 * scores.var_intensity_score
 * <p>
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 */
@Component("libraryScorer")
public class LibraryScorer {
    /**
     * scores.library_corr //对run和library intensity算Pearson相关系数
     * scores.library_norm_manhattan // 对run intensity 算平均占比差距
     * scores.norm_rt_score //normalizedRunRt与groupRt之差
     *
     * @param peakGroup       get runIntensity: from features extracted
     * @param normedLibIntMap get libraryIntensity: from transitions
     * @param scoreTypes      library_corr, library_norm_manhattan
     */
    public void calculateLibraryScores(PeakGroup peakGroup, HashMap<String, Double> normedLibIntMap, List<String> scoreTypes) {
        List<Double> runIntensity = new ArrayList<>(peakGroup.getIonIntensity().values());
        assert runIntensity.size() == normedLibIntMap.size();

        List<Double> normedLibInt = new ArrayList<>(normedLibIntMap.values());
        List<Double> normedRunInt = ScoreUtil.normalizeSumDouble(runIntensity, peakGroup.getIntensitySum());
        //library_norm_manhattan
        //占比差距平均
        if (scoreTypes.contains(ScoreType.LibraryRsmd.getName())) {
            double sum = 0.0d;
            for (int i = 0; i < normedLibInt.size(); i++) {
                sum += Math.abs(normedLibInt.get(i) - normedRunInt.get(i));
            }
            peakGroup.put(ScoreType.LibraryRsmd.getName(), sum / normedLibInt.size(), scoreTypes);
        }

        double runSum = 0.0d, librarySum = 0.0d, run2Sum = 0.0d, library2Sum = 0.0d, dotprod = 0.0d;
        for (int i = 0; i < normedLibInt.size(); i++) {
            dotprod += runIntensity.get(i) * normedLibInt.get(i); //corr
            runSum += runIntensity.get(i); //sum of run
            librarySum += normedLibInt.get(i); //sum of library
            run2Sum += runIntensity.get(i) * runIntensity.get(i);// run ^2
            library2Sum += normedLibInt.get(i) * normedLibInt.get(i); // library ^2
        }
        //library_corr pearson 相关系数
        //需要的前置变量：dotprod, sum, 2sum
        if (scoreTypes.contains(ScoreType.LibraryCorr.getName())) {
            if (DeveloperParams.USE_NEW_LIBRARY_SHIFT_SCORE) {
                peakGroup.put(ScoreType.LibraryCorr.getName(), calculateLibraryShiftScore(normedLibInt, normedRunInt), scoreTypes);
            } else {
                double runDeno = run2Sum - runSum * runSum / normedLibInt.size();
                double libDeno = library2Sum - librarySum * librarySum / normedLibInt.size();
                if (runDeno <= Constants.MIN_DOUBLE || libDeno <= Constants.MIN_DOUBLE) {
                    peakGroup.put(ScoreType.LibraryCorr.getName(), 0d, scoreTypes);
                } else {
                    double pearsonR = dotprod - runSum * librarySum / normedLibInt.size();
                    pearsonR /= FastMath.sqrt(runDeno * libDeno);
                    peakGroup.put(ScoreType.LibraryCorr.getName(), pearsonR, scoreTypes);
                }
            }
        }

        double[] runSqrt = new double[runIntensity.size()];
        double[] libSqrt = new double[normedLibInt.size()];
        for (int i = 0; i < runSqrt.length; i++) {
            runSqrt[i] = FastMath.sqrt(runIntensity.get(i));
            libSqrt[i] = FastMath.sqrt(normedLibInt.get(i));
        }

        //dotprodScoring
        //需要的前置变量：runSum, librarySum, runSqrt, libSqrt
        if (scoreTypes.contains(ScoreType.LibraryDotprod.getName())) {
            double runVecNorm = FastMath.sqrt(runSum);
            double libVecNorm = FastMath.sqrt(librarySum);

            double[] runSqrtVecNormed = normalize(runSqrt, runVecNorm);
            double[] libSqrtVecNormed = normalize(libSqrt, libVecNorm);

            double sumOfMult = 0d;
            for (int i = 0; i < runSqrt.length; i++) {
                sumOfMult += runSqrtVecNormed[i] * libSqrtVecNormed[i];
            }
            peakGroup.put(ScoreType.LibraryDotprod.getName(), sumOfMult, scoreTypes);
        }

        //manhattan
        //需要的前置变量：runSqrt, libSqrt
        if (scoreTypes.contains(ScoreType.LibraryManhattan.getName())) {
            double runIntTotal = MathUtil.sum(runSqrt);
            double libIntTotal = MathUtil.sum(libSqrt);
            double[] runSqrtNormed = normalize(runSqrt, runIntTotal);
            double[] libSqrtNormed = normalize(libSqrt, libIntTotal);
            double sumOfDivide = 0;
            for (int i = 0; i < runSqrt.length; i++) {
                sumOfDivide += FastMath.abs(runSqrtNormed[i] - libSqrtNormed[i]);
            }
            peakGroup.put(ScoreType.LibraryManhattan.getName(), sumOfDivide, scoreTypes);
        }

        //spectral angle
        if (scoreTypes.contains(ScoreType.LibrarySangle.getName())) {
            double spectralAngle = FastMath.acos(dotprod / (FastMath.sqrt(run2Sum) * FastMath.sqrt(library2Sum)));
            peakGroup.put(ScoreType.LibrarySangle.getName(), spectralAngle, scoreTypes);
        }

        //root mean square
        if (scoreTypes.contains(ScoreType.LibraryRootmeansquare.getName())) {
            double rms = 0;
            for (int i = 0; i < normedLibInt.size(); i++) {
                rms += (normedLibInt.get(i) - normedRunInt.get(i)) * (normedLibInt.get(i) - normedRunInt.get(i));
            }
            rms = Math.sqrt(rms / normedLibInt.size());
            peakGroup.put(ScoreType.LibraryRootmeansquare.getName(), rms, scoreTypes);
        }
    }

    public void calculateNormRtScore(PeakGroup peakGroup, SlopeIntercept slopeIntercept, double libRt, List<String> scoreTypes) {
        //varNormRtScore
        double runRt = peakGroup.getApexRt();
        double normRunRt = ScoreUtil.trafoApplier(slopeIntercept, runRt);
        peakGroup.put(ScoreType.NormRtScore.getName(), Math.abs(normRunRt - libRt), scoreTypes);
    }

    /**
     * 当出现干扰碎片的时候,本打分会有很大的副作用
     * scores.var_intensity_score
     * <p>
     * sum of intensitySum:
     * totalXic
     */
    public void calculateIntensityScore(PeakGroup peakGroup, List<String> scoreTypes) {
        double intensitySum = peakGroup.getIntensitySum();
        double totalXic = peakGroup.getTic();
        peakGroup.put(ScoreType.IntensityScore.getName(), intensitySum / totalXic, scoreTypes);
    }

    private double[] normalize(double[] array, double value) {
        if (value > 0) {
            for (int i = 0; i < array.length; i++) {
                array[i] /= value;
            }
        }
        return array;
    }

    private double calculateLibraryShiftScore(List<Double> libIntensities, List<Double> runIntensities) {
        double maxRatio = 0d, minRatio = Double.MAX_VALUE;
        for (int i = 0; i < libIntensities.size(); i++) {
            double ratio = runIntensities.get(i) / libIntensities.get(i);
            if (ratio > maxRatio) {
                maxRatio = ratio;
            }
            if (ratio < minRatio) {
                minRatio = ratio;
            }
        }
        return (maxRatio - minRatio) / maxRatio;
    }

}
