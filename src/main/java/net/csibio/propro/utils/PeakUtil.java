package net.csibio.propro.utils;

import net.csibio.propro.domain.bean.math.BisectionLowHigh;

public class PeakUtil {

    public static int findNearestIndex(Double[] x, double value) {
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(x, value);
        if (x[bisectionLowHigh.high()] - value > value - x[bisectionLowHigh.low()]) {
            return bisectionLowHigh.low();
        } else {
            return bisectionLowHigh.high();
        }
    }

    public static int findNearestIndex(float[] x, float value) {
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(x, value);
        if (x[bisectionLowHigh.high()] - value > value - x[bisectionLowHigh.low()]) {
            return bisectionLowHigh.low();
        } else {
            return bisectionLowHigh.high();
        }
    }
}
