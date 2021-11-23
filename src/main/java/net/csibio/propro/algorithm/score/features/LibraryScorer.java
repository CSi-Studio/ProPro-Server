package net.csibio.propro.algorithm.score.features;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.options.DeveloperParams;
import net.csibio.propro.utils.ArrayUtil;
import net.csibio.propro.utils.MathUtil;
import net.csibio.propro.utils.ScoreUtil;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * scores.library_corr
 * scores.library_norm_manhattan
 * scores.var_intensity_score
 * <p>
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 */
@Slf4j
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
        for (String cutInfo : normedLibIntMap.keySet()) {
            peakGroup.getApexIonsIntensity().putIfAbsent(cutInfo, 0d);
        }
        Double[] runIntensity = new Double[peakGroup.getIonIntensity().size()];
        Double[] apexRunIntensity = new Double[peakGroup.getApexIonsIntensity().size()];
        Double[] normedLibInt = new Double[peakGroup.getIonIntensity().size()];
        peakGroup.getIonIntensity().values().toArray(runIntensity);
        peakGroup.getApexIonsIntensity().values().toArray(apexRunIntensity);
        normedLibIntMap.values().toArray(normedLibInt);

        Double[] normedRunInt = ScoreUtil.normalizeSumDoubleArray(runIntensity, peakGroup.getIntensitySum());
        Double[] normedApexRunInt = ScoreUtil.normalizeSumDoubleArray(apexRunIntensity);

        //library_norm_manhattan
        if (scoreTypes.contains(ScoreType.LibraryRsmd.getName())) {
            double sum = 0.0d;
            for (int i = 0; i < normedLibInt.length; i++) {
                sum += Math.abs(normedLibInt[i] - normedRunInt[i]);
            }
            peakGroup.put(ScoreType.LibraryRsmd.getName(), sum / normedLibInt.length, scoreTypes);
        }

        double runSum = 0.0d, librarySum = 0.0d, run2Sum = 0.0d, library2Sum = 0.0d, dotprod = 0.0d;
        for (int i = 0; i < normedLibInt.length; i++) {
            dotprod += runIntensity[i] * normedLibInt[i]; //corr
            runSum += runIntensity[i]; //sum of run
            librarySum += normedLibInt[i]; //sum of library
            run2Sum += runIntensity[i] * runIntensity[i];// run ^2
            library2Sum += normedLibInt[i] * normedLibInt[i]; // library ^2
        }
        //library_corr pearson 相关系数
        //需要的前置变量：dotprod, sum, 2sum
        if (scoreTypes.contains(ScoreType.LibraryCorr.getName())) {
            double pearsonSum = 0d;
            if (DeveloperParams.USE_NEW_LIBRARY_SHIFT_SCORE) {
                peakGroup.put(ScoreType.LibraryCorr.getName(), calculateLibraryShiftScore(normedLibInt, normedRunInt), scoreTypes);
            } else {
                double runDeno = run2Sum - runSum * runSum / normedLibInt.length;
                double libDeno = library2Sum - librarySum * librarySum / normedLibInt.length;
                if (runDeno <= Constants.MIN_DOUBLE || libDeno <= Constants.MIN_DOUBLE) {
                    peakGroup.put(ScoreType.LibraryCorr.getName(), 0d, scoreTypes);
                } else {
                    pearsonSum = dotprod - runSum * librarySum / normedLibInt.length;
                    pearsonSum /= FastMath.sqrt(runDeno * libDeno);

                    //Apex处的pearson系数
                    PearsonsCorrelation pearson = new PearsonsCorrelation();
                    double pearsonApex = pearson.correlation(ArrayUtil.toPrimitive(normedLibInt), ArrayUtil.toPrimitive(normedApexRunInt));
                    if (Double.isNaN(pearsonApex)) {
                        pearsonApex = 0d;
                    }
                    peakGroup.put(ScoreType.LibraryCorr.getName(), Math.max(pearsonSum, pearsonApex), scoreTypes);
                    peakGroup.put(ScoreType.LibraryCorr.getName(), pearsonApex, scoreTypes);
                }
            }

            //另外一种计算函数
            PearsonsCorrelation pearson = new PearsonsCorrelation();
            double pearsonResult = pearson.correlation(ArrayUtil.toPrimitive(normedLibInt), ArrayUtil.toPrimitive(normedApexRunInt));
            if (Double.isNaN(pearsonResult)) {
                pearsonResult = 0d;
            }
//            peakGroup.put(ScoreType.LibraryApexCorr.getName(), pearsonResult, scoreTypes);
        }

        double[] runSqrt = new double[runIntensity.length];
        double[] libSqrt = new double[normedLibInt.length];
        for (int i = 0; i < runSqrt.length; i++) {
            runSqrt[i] = FastMath.sqrt(runIntensity[i]);
            libSqrt[i] = FastMath.sqrt(normedLibInt[i]);
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
            for (int i = 0; i < normedLibInt.length; i++) {
                rms += (normedLibInt[i] - normedRunInt[i]) * (normedLibInt[i] - normedRunInt[i]);
            }
            rms = Math.sqrt(rms / normedLibInt.length);
            peakGroup.put(ScoreType.LibraryRootmeansquare.getName(), rms, scoreTypes);
        }
    }

    public void calculateNormRtScore(PeakGroup peakGroup, SlopeIntercept slopeIntercept, double libRt, List<String> scoreTypes) {
        double runRt = peakGroup.getApexRt();
        double normRunRt = ScoreUtil.trafoApplier(slopeIntercept, runRt);
        peakGroup.put(ScoreType.NormRtScore.getName(), Math.abs(normRunRt - libRt), scoreTypes);
    }

    public void calculateSpearmanScore(PeakGroup peakGroup, PeptideCoord coord, List<String> scoreTypes) {
        List<Map.Entry<String, Double>> entryList = peakGroup.getIonIntensity().entrySet().stream().sorted(Comparator.comparing(Map.Entry<String, Double>::getValue).reversed()).toList();
        int length = coord.getFragments().size();

        HashMap<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < entryList.size(); i++) {
            orderMap.put(entryList.get(i).getKey(), i);
        }
        double diffSum = 0;
        for (int i = 0; i < coord.getFragments().size(); i++) {
            String cutInfo = coord.getFragments().get(i).getCutInfo();
            int index = -1;
            if (orderMap.get(cutInfo) == null) {
                index = coord.getFragments().size();
            } else {
                index = orderMap.get(cutInfo);
            }
            diffSum += (index - i) * (index - i);
        }
        double score = 1 - diffSum * 6 / (length * length * length - length);
//        peakGroup.put(ScoreType.LibrarySpearman.getName(), score, scoreTypes);
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

    private double calculateLibraryShiftScore(Double[] libIntensities, Double[] runIntensities) {
        double maxRatio = 0d, minRatio = Double.MAX_VALUE;
        for (int i = 0; i < libIntensities.length; i++) {
            double ratio = runIntensities[i] / libIntensities[i];
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
