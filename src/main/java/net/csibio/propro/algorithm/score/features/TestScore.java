package net.csibio.propro.algorithm.score.features;

import net.csibio.propro.utils.MathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang
 * Time: 2019-03-01 20:08
 */
public class TestScore {
    private double delta = 0.1d;
    private double deltaSlope = 0.01d;
    private double slopeStep = 0.2d;
    private double slopeThreshold = 0.001d;

    public double getIntensityScore(List<Double> libIntList, List<Double> runIntList) {
        double slope = getSlope(libIntList, runIntList);
        double score = 0;
        for (int i = 0; i < libIntList.size(); i++) {
            score += (1d - shiftedSigmoid(Math.abs(runIntList.get(i) - libIntList.get(i) * slope) / (libIntList.get(i) * slope)));
        }
        score = score / libIntList.size();
        return score;
    }

    public double getSlope(List<Double> libIntList, List<Double> runIntList) {
        double slope = MathUtil.sum(runIntList) / MathUtil.sum(libIntList);
        double lastSlope = Double.MAX_VALUE;
        while (Math.abs(slope - lastSlope) >= slopeThreshold) {
            lastSlope = slope;
            slope = updateSlope(libIntList, runIntList, slope);
        }
        return slope;
    }

    private double getLoss(List<Double> libIntList, List<Double> runIntList, double slope) {
        List<Double> normalizedDiffs = new ArrayList<>();
        for (int index = 0; index < libIntList.size(); index++) {
            double diff = libIntList.get(index) * slope - runIntList.get(index);
            double normalizedDiff = Math.abs(diff) / (libIntList.get(index) * slope);
            normalizedDiffs.add(normalizedDiff);
        }
        return huberLoss(normalizedDiffs, delta);
    }


    /**
     * Huber Loss 是一个用于回归问题的带参损失函数, 优点是能增强平方误差损失函数(MSE, mean square error)对离群点的鲁棒性。
     *
     * @param diffs 归一化后的diff List
     * @param delta 线性误差与平方误差的阈值
     * @return
     */
    private double huberLoss(List<Double> diffs, double delta) {
        double loss = 0;
        for (double diff : diffs) {
            if (diff <= delta) {
                loss += 0.5 * diff * diff;
            } else {
                loss += delta * diff - 0.5 * delta * delta;
            }
        }
        return loss;
    }

    private double shiftedSigmoid(double value) {
//        =2/(1+EXP(-2*A4))-1
        return 2d / (1 + Math.exp(-2d * value)) - 1;
    }

    private double getGradient(List<Double> libIntList, List<Double> runIntList, double slope) {
        double loss = getLoss(libIntList, runIntList, slope - deltaSlope);
        double deltaLoss = getLoss(libIntList, runIntList, slope + deltaSlope) - loss;
        return deltaLoss / deltaSlope / 2d;
    }

    private double updateSlope(List<Double> libIntList, List<Double> runIntList, double slope) {
        double gradient = getGradient(libIntList, runIntList, slope);
//        slope -= gradient * slopeStep;
        slope -= Math.random() * gradient * slopeStep;
        return slope;
    }
}
