package net.csibio.propro.algorithm.peak;

import net.csibio.propro.constants.constant.SmoothConst;
import net.csibio.propro.constants.enums.SmoothMethod;
import net.csibio.propro.domain.bean.common.DoublePairs;
import net.csibio.propro.domain.options.PeakFindingOptions;
import net.csibio.propro.domain.options.SigmaSpacing;
import org.springframework.stereotype.Component;

@Component("smoother")
public class Smoother {

    public DoublePairs doSmooth(DoublePairs eic, PeakFindingOptions params) {
        DoublePairs smoothEic;
        SmoothMethod smoothMethod = SmoothMethod.getByName(params.getSmoothMethod());
        if (smoothMethod == null) {
            smoothMethod = SmoothMethod.NONE;
        }
        switch (smoothMethod) {
            case LINEAR:
                smoothEic = linearSmoother(eic, params.getSmoothPoints());
                break;
            case GAUSS:
                smoothEic = gaussSmoother(eic, params.getSmoothPoints());
                break;
            case SAVITZKY_GOLAY:
                smoothEic = savitzkyGolaySmoother(eic, params.getSmoothPoints());
                break;
            case PROPRO_GAUSS:
                smoothEic = proproGaussSmoother(eic); //propro平滑暂时不支持平滑点数设置, 内部已调参到最优
                break;
            case NONE:
                smoothEic = eic;
                break;
            default:
                smoothEic = eic;
        }
        return smoothEic;
    }

    //n点Linear平滑, n为从3开始的奇数, 代表参与平滑的总点数
    private DoublePairs linearSmoother(DoublePairs eic, int pointNum) {
        if (SmoothConst.LINEAR.containsKey(pointNum)) {
            double[] weights = SmoothConst.LINEAR.get(pointNum);
            double[] smoothInts = convolve(eic.y(), weights);
            DoublePairs smoothEic = new DoublePairs(eic.x(), smoothInts);
            return smoothEic;
        } else {
            return eic;
        }
    }

    //n点Gauss平滑, n为从3开始的奇数, 代表参与平滑的总点数
    private DoublePairs gaussSmoother(DoublePairs eic, int pointNum) {
        if (SmoothConst.GAUSS.containsKey(pointNum)) {
            double[] weights = SmoothConst.GAUSS.get(pointNum);
            double[] smoothInts = convolve(eic.y(), weights);

            DoublePairs smoothEic = new DoublePairs(eic.x(), smoothInts);
            return smoothEic;
        } else {
            return eic;
        }
    }

    //n点S-G平滑, n为从5开始的奇数, 代表参与平滑的总点数
    private DoublePairs savitzkyGolaySmoother(DoublePairs eic, int pointNum) {
        if (SmoothConst.SAVITZKY_GOLAY.containsKey(pointNum)) {
            double[] weights = SmoothConst.SAVITZKY_GOLAY.get(pointNum);
            double[] smoothInts = convolve(eic.y(), weights);
            DoublePairs smoothEic = new DoublePairs(eic.x(), smoothInts);
            return smoothEic;
        } else {
            return eic;
        }
    }

    /**
     * @param pairs
     * @Description: 对色谱图的高斯平滑
     * @see DoublePairs
     **/
    public DoublePairs proproGaussSmoother(DoublePairs pairs) {
        double[] rtArray = pairs.x();
        double[] intensityList = pairs.y();
        SigmaSpacing sigmaSpacing = SigmaSpacing.create();
        double spacing = sigmaSpacing.getSpacing();
        //coeffs: 以0为中心，sigma为标准差的正态分布参数
        double[] coeffs = sigmaSpacing.getCoeffs();
        int middle = sigmaSpacing.getRightNum();
        double middleSpacing = sigmaSpacing.getRightNumSpacing();

        int rtLength = rtArray.length;
        double startPosition, endPosition;
        double minRt = rtArray[0];
        double maxRt = rtArray[rtLength - 1];

        //begin integrate
        double[] smoothIntensityList = new double[rtLength];

        Double distanceInGaussian;
        int leftPosition;
        int rightPosition;
        double residualPercent;
        double coeffRight;
        double coeffLeft;
        double norm = 0;

        for (int i = 0; i < rtLength; i++) {
            double v = 0;
            norm = 0;
            //startPosition
            if ((rtArray[i] - middleSpacing) > minRt) {
                startPosition = rtArray[i] - middleSpacing;
            } else {
                startPosition = minRt;
            }

            //endPostion
            if ((rtArray[i] + middleSpacing) < maxRt) {
                endPosition = rtArray[i] + middleSpacing;
            } else {
                endPosition = maxRt;
            }

            //help index
            int j = i;

            // left side of i
            while (j > 0 && rtArray[j - 1] > startPosition) {
                distanceInGaussian = rtArray[i] - rtArray[j];
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }

                distanceInGaussian = rtArray[i] - rtArray[j - 1];
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing - distanceInGaussian)) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[j - 1] - rtArray[j]) * (coeffRight + coeffLeft) / 2.0;


                v += Math.abs(rtArray[j - 1] - rtArray[j]) * (intensityList[j - 1] * coeffLeft + intensityList[j] * coeffRight) / 2.0;

                j--;

            }

            j = i;
            // right side of i
            while (j < rtLength - 1 && rtArray[j + 1] < endPosition) {
                distanceInGaussian = rtArray[j] - rtArray[i];
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                distanceInGaussian = rtArray[j + 1] - rtArray[i];
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;

                residualPercent = (Math.abs(leftPosition * spacing - distanceInGaussian)) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[j + 1] - rtArray[j]) * (coeffLeft + coeffRight) / 2.0;
                v += Math.abs(rtArray[j + 1] - rtArray[j]) * (intensityList[j] * coeffLeft + intensityList[j + 1] * coeffRight) / 2.0;
                j++;

            }

            if (v > 0) {
                smoothIntensityList[i] = v / norm;
            } else {
                smoothIntensityList[i] = 0d;
            }
        }

        return new DoublePairs(rtArray, smoothIntensityList);
    }

    private double[] convolve(double[] intensities, double[] weights) {
        double[] convolved = new double[intensities.length];

        for (int i = 0; i < intensities.length; i++) {
            //mid
            double sum = intensities[i] * weights[0];
            //left
            for (int j = 1; j < weights.length; j++) {
                if (i - j < 0) {
                    break;
                }
                sum += weights[j] * intensities[i - j];
            }
            //right
            for (int j = 1; j < weights.length; j++) {
                if (i + j >= intensities.length) {
                    break;
                }
                sum += weights[j] * intensities[i + j];
            }
            convolved[i] = sum;
        }

        return convolved;
    }
}
