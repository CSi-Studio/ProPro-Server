package net.csibio.propro.utils;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.score.IntegrateWindowMzIntensity;
import net.csibio.propro.domain.bean.score.SlopeIntercept;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScoreUtil {

    /**
     * invert y = kx + b to x = 1/k y + -b/k;
     *
     * @param slopeIntercept k & b input
     * @return 1/k -b/k
     */
    public static SlopeIntercept trafoInverter(SlopeIntercept slopeIntercept) {
        double slope = slopeIntercept.getSlope();
        double intercept = slopeIntercept.getIntercept();
        SlopeIntercept slopeInterceptInvert = new SlopeIntercept();

        if (slope == 0d) {
            slope = 0.000001d;
        }
        slopeInterceptInvert.setSlope(1 / slope);
        slopeInterceptInvert.setIntercept(-intercept / slope);

        return slopeInterceptInvert;
    }

    /**
     * apply kx + b
     *
     * @param slopeIntercept k & b (may be inverted)
     * @param value          x
     * @return y
     */
    public static double trafoApplier(SlopeIntercept slopeIntercept, Double value) {
        if (slopeIntercept.getSlope() == 0) {
            return value;
        } else {
            return value * slopeIntercept.getSlope() + slopeIntercept.getIntercept();
        }
    }

    /**
     * 1) get sum of list
     * 2) divide elements in list by sum
     *
     * @param intensityList input intensity list
     * @return output normalized intensity list
     */
    public static Double[] normalizeSumDoubleArray(Double[] intensityList, double sum) {
        Double[] normalizedIntensity = new Double[intensityList.length];
        for (int i = 0; i < intensityList.length; i++) {
            normalizedIntensity[i] = (intensityList[i] / sum);
        }
        return normalizedIntensity;
    }

    public static Double[] normalizeSumDoubleArray(Double[] intensityList) {

        double sum = 0d;
        for (int i = 0; i < intensityList.length; i++) {
            sum += intensityList[i];
        }
        Double[] normalizedIntensity = new Double[intensityList.length];
        for (int i = 0; i < intensityList.length; i++) {
            normalizedIntensity[i] = (intensityList[i] / sum);
        }
        return normalizedIntensity;
    }

    public static List<Double> normalizeSumDouble(List<Double> intensityList, double sum) {
        List<Double> normalizedIntensity = new ArrayList<>();
        for (int i = 0; i < intensityList.size(); i++) {
            normalizedIntensity.add(intensityList.get(i) / sum);
        }
        return normalizedIntensity;
    }

    /**
     * 1) get left and right index corresponding to spectrum
     * 2) get interval intensity sum to intensity
     * 3) get interval average mz by intensity(as weight)
     *
     * @param spectrumMzArray  spectrum
     * @param spectrumIntArray spectrum
     * @param left             left mz
     * @param right            right mz
     * @return float mz,intensity boolean signalFound
     */
    public static IntegrateWindowMzIntensity integrateWindow(float[] spectrumMzArray, float[] spectrumIntArray, float left, float right) {
        IntegrateWindowMzIntensity mzIntensity = new IntegrateWindowMzIntensity();

        double mz = 0d, intensity = 0d;
        int leftIndex = ConvolutionUtil.findIndex(spectrumMzArray, left, true);
        int rightIndex = ConvolutionUtil.findIndex(spectrumMzArray, right, false);

        if (leftIndex == -1 || rightIndex == -1) {
            return new IntegrateWindowMzIntensity(false);
        }

        for (int index = leftIndex; index <= rightIndex; index++) {
            intensity += spectrumIntArray[index];
            mz += spectrumMzArray[index] * spectrumIntArray[index];
        }

        if (intensity > 0f) {
            mz /= intensity;
            mzIntensity.setSignalFound(true);
        } else {
            mz = -1;
            intensity = 0;
            mzIntensity.setSignalFound(false);
        }
        mzIntensity.setMz(mz);
        mzIntensity.setIntensity(intensity);

        return mzIntensity;
    }

    public static List<String> getScoreTypes(HttpServletRequest request) {
        List<String> scoreTypes = new ArrayList<>();
//        scoreTypes.add(ScoreType.InitScore.getName());
//        scoreTypes.add(ScoreType.TotalScore.getName());
        for (ScoreType type : ScoreType.values()) {
            String typeParam = request.getParameter(type.getName());
            if (typeParam != null && typeParam.equals("on")) {
                scoreTypes.add(type.getName());
            }
        }
        return scoreTypes;
    }
}
