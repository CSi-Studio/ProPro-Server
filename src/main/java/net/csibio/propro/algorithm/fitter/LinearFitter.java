package net.csibio.propro.algorithm.fitter;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.bean.common.Pair;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("linearFitter")
public class LinearFitter {

    /**
     * 最小二乘法线性拟合RTPairs
     */
    public SlopeIntercept leastSquare(List<Pair> rtPairs) {
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (Pair rtPair : rtPairs) {
            obs.add(rtPair.right(), rtPair.left());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope(coeff[1]);
        slopeIntercept.setIntercept(coeff[0]);
        return slopeIntercept;
    }

    public SlopeIntercept huberFit(List<Pair> rtPairs, double delta) throws Exception {
        double tolerance = 0.001d;
        SlopeIntercept lastSlopeIntercept = getInitSlopeIntercept(rtPairs);
        SlopeIntercept slopeIntercept = updateHuberSlopeIntercept(rtPairs, lastSlopeIntercept, delta);
        int count = 1;
        while (count < 10000 && Math.abs(getHuberSlopeGradient(rtPairs, slopeIntercept, delta)) > tolerance
                || Math.abs(getHuberInterceptGradient(rtPairs, slopeIntercept, delta)) > tolerance) {
            slopeIntercept = updateHuberSlopeIntercept(rtPairs, slopeIntercept, delta);
            count++;
        }
        log.info("----------------------- Huber " + count + " epochs -----------------------");
        return slopeIntercept;
    }

    public SlopeIntercept proproFit(List<Pair> rtPairs, double delta) throws Exception {
        double tolerance = 0.001d;
        SlopeIntercept lastSlopeIntercept = getInitSlopeIntercept(rtPairs);
        SlopeIntercept slopeIntercept = updateProproSlopeIntercept(rtPairs, lastSlopeIntercept, delta);
        int count = 1;
        while (count < 10000 && Math.abs(getProproSlopeGradient(rtPairs, slopeIntercept, delta)) > tolerance
                || Math.abs(getProproInterceptGradient(rtPairs, slopeIntercept, delta)) > tolerance) {
            slopeIntercept = updateProproSlopeIntercept(rtPairs, slopeIntercept, delta);
            count++;
        }
        log.info("----------------------- Propro " + count + " epochs -----------------------");
        return slopeIntercept;
    }

    private double getHuberLoss(List<Pair> rtPairs, double slope, double intercept, double delta) {
        double loss = 0d;
        for (Pair rtPair : rtPairs) {
            double tempDiff = Math.abs(rtPair.right() * slope + intercept - rtPair.left());
            if (tempDiff <= delta) {
                loss += 0.5 * tempDiff * tempDiff;
            } else {
                loss += delta * tempDiff - 0.5 * delta * delta;
            }
        }
        return loss;
    }

    private double getProproLoss(List<Pair> rtPairs, double slope, double intercept, double delta) {
        double loss = 0d;
        for (Pair rtPair : rtPairs) {
            double tempDiff = Math.abs(rtPair.right() * slope + intercept - rtPair.left());
            if (tempDiff <= delta) {
                loss += 0.5 * tempDiff * tempDiff;
            } else {
                loss += (Math.log(tempDiff) - Math.log(delta) + 0.5d) * delta * delta;
            }
        }
        return loss;
    }

    private double getHuberSlopeGradient(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double deltaSlope = 0.00000001d;
        double loss = getHuberLoss(rtPairs, slopeIntercept.getSlope() - deltaSlope, slopeIntercept.getIntercept(), delta);
        double deltaLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope() + deltaSlope, slopeIntercept.getIntercept(), delta) - loss;
        return deltaLoss / deltaSlope / 2d;
    }

    private double getProproSlopeGradient(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double deltaSlope = 0.00000001d;
        double loss = getProproLoss(rtPairs, slopeIntercept.getSlope() - deltaSlope, slopeIntercept.getIntercept(), delta);
        double deltaLoss = getProproLoss(rtPairs, slopeIntercept.getSlope() + deltaSlope, slopeIntercept.getIntercept(), delta) - loss;
        return deltaLoss / deltaSlope / 2d;
    }

    private double getHuberInterceptGradient(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double deltaIntercept = 0.00000001d;
        double loss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() - deltaIntercept, delta);
        double deltaLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() + deltaIntercept, delta) - loss;
        return deltaLoss / deltaIntercept / 2d;
    }

    private double getProproInterceptGradient(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double deltaIntercept = 0.00000001d;
        double loss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() - deltaIntercept, delta);
        double deltaLoss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() + deltaIntercept, delta) - loss;
        return deltaLoss / deltaIntercept / 2d;
    }

    private SlopeIntercept updateHuberSlopeIntercept(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double slopeStep = 0.000001d, interceptStep = 0.1d;
        double sigma = 1d;
        double oriLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept(), delta);
        double slopeGradient = getHuberSlopeGradient(rtPairs, slopeIntercept, delta);
        double interceptGradient = getHuberInterceptGradient(rtPairs, slopeIntercept, delta);
        double intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
        double slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
        double updatedLoss = getHuberLoss(rtPairs, slope, intercept, delta);
        while (updatedLoss > oriLoss) {
            sigma = sigma / 2d;
            slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
            intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
            updatedLoss = getHuberLoss(rtPairs, slope, intercept, delta);
        }
        return new SlopeIntercept(slope, intercept);
    }

    private SlopeIntercept updateProproSlopeIntercept(List<Pair> rtPairs, SlopeIntercept slopeIntercept, double delta) {
        double slopeStep = 0.00000001d, interceptStep = 0.1d;
        double sigma = 1d;
        double oriLoss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept(), delta);
        double slopeGradient = getProproSlopeGradient(rtPairs, slopeIntercept, delta);
        double interceptGradient = getProproInterceptGradient(rtPairs, slopeIntercept, delta);
        double intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
        double slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
        double updatedLoss = getProproLoss(rtPairs, slope, intercept, delta);
        while (updatedLoss > oriLoss) {
            sigma = sigma / 2d;
            slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
            intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
            updatedLoss = getProproLoss(rtPairs, slope, intercept, delta);
        }
        return new SlopeIntercept(slope, intercept);
    }

    public SlopeIntercept buildInitSlopeIntercept(List<Pair> rtPairs, List<Pair> diffList) {
        if (rtPairs.size() == 0) {
            log.error("rtPairs is empty!");
            return new SlopeIntercept(0, 0);
        }
        if (rtPairs.size() != diffList.size()) {
            log.error("数据内容有问题!rtPairs和diffList必须等长");
            return new SlopeIntercept(0, 0);
        }

        //按照差值从大到小排序
        List<Pair> topDiffList = diffList.stream().sorted(Comparator.comparing(Pair::right).reversed()).toList();
        Set<Double> selectRts = new HashSet<>();
        if (topDiffList.size() > 2) {
            selectRts = topDiffList.subList(0, topDiffList.size() / 2).stream().map(Pair::left).collect(Collectors.toSet());
        } else {
            selectRts = topDiffList.stream().map(Pair::left).collect(Collectors.toSet());
        }

        List<Pair> selectedRtPairs = new ArrayList<>();
        for (int i = 0; i < rtPairs.size(); i++) {
            if (selectRts.contains(rtPairs.get(i).left())) {
                selectedRtPairs.add(rtPairs.get(i));
            }
        }

        return getInitSlopeIntercept(selectedRtPairs);
    }

    public SlopeIntercept getInitSlopeIntercept(List<Pair> rtPairs) {
        if (rtPairs.size() == 0) {
            log.error("rtPairs is empty!");
            return new SlopeIntercept(0, 0);
        }
        rtPairs = rtPairs.stream().sorted(Comparator.comparing(Pair::left)).toList();
        double minLibRT = rtPairs.get(0).left();
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        int maxIndex = 0, minIndex = 0;
        for (int i = 0; i < rtPairs.size(); i++) {
            double product = (rtPairs.get(i).left() - minLibRT + 1) * rtPairs.get(i).right();
            if (product > max) {
                max = product;
                maxIndex = i;
            }
            if (product < min) {
                min = product;
                minIndex = i;
            }
        }

        double slope = (rtPairs.get(maxIndex).left() - rtPairs.get(minIndex).left())
                / (rtPairs.get(maxIndex).right() - rtPairs.get(minIndex).right());
        double intercept = rtPairs.get(maxIndex).left() - rtPairs.get(maxIndex).right() * slope;
        return new SlopeIntercept(slope, intercept);
    }
}
