package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.constants.enums.BaselineMethod;
import net.csibio.propro.constants.enums.NoiseEstimateMethod;
import net.csibio.propro.constants.enums.PeakFindingMethod;
import net.csibio.propro.constants.enums.SmoothMethod;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Nico Wang
 * Time: 2019-12-03 20:53
 */
@Data
public class PeakFindingOptions {
    /**
     * 选择平滑算法
     *
     * @see net.csibio.propro.constants.enums.SmoothMethod
     */
    String smoothMethod;
    Integer smoothPoints; //平滑点数

    /**
     * 选择选峰算法
     *
     * @see net.csibio.propro.constants.enums.PeakFindingMethod
     */
    String peakFindingMethod;

    /**
     * 选择baseline算法
     *
     * @see net.csibio.propro.constants.enums.BaselineMethod
     */
    String baselineMethod;
    Double baselineRtTolerance; //baselineRt窗口大小

    /**
     * 噪声估计算法
     *
     * @see net.csibio.propro.constants.enums.NoiseEstimateMethod
     */
    //EIC噪声估计算法
    String eicNoiseMethod;
    Double noiseAmplitude; //噪声振幅
    Double noisePercentage; //噪声百分比

    /**
     * 噪声估计算法
     *
     * @see net.csibio.propro.constants.enums.NoiseEstimateMethod
     */
    //Peak噪声估计算法
    String peakNoiseMethod;
    Double stnThreshold; //信噪比阈值

    /**
     * 峰筛选条件
     */
    Double minPeakHeight; //最小峰高
    Double minPeakWidth; //最小峰宽
    Integer minPeakPoints; //最少含有的点数
    Double maxZeroPointRatio; //最大噪声信号点比例  //??0.1,135 0.2,81 0.3,61 0.4,55 1,50
    Double minObviousness; //最小显著程度

    /**
     * 峰边界矫正
     */
    Double firstDerivativeCutoffFactor; //峰边界一阶导数与最大一阶导数的比例系数

    public void fillParams() {
        if (StringUtils.isEmpty(this.smoothMethod)) this.smoothMethod = SmoothMethod.GAUSS.getName();
        if (this.smoothPoints == null) this.smoothPoints = 5;
        if (StringUtils.isEmpty(this.peakFindingMethod)) this.peakFindingMethod = PeakFindingMethod.WAVELET.getName();
        if (StringUtils.isEmpty(this.baselineMethod)) this.baselineMethod = BaselineMethod.TOLERANCE.getName();
        if (this.baselineRtTolerance == null) this.baselineRtTolerance = 0.2d;
        if (StringUtils.isEmpty(this.eicNoiseMethod)) this.eicNoiseMethod = NoiseEstimateMethod.PROPRO_EIC.getName();
        if (this.noiseAmplitude == null) this.noiseAmplitude = 10000d;
        if (this.noisePercentage == null) this.noisePercentage = 20d;
        if (StringUtils.isEmpty(this.peakNoiseMethod))
            this.peakNoiseMethod = NoiseEstimateMethod.SLIDING_WINDOW_PEAK.getName();
        if (this.stnThreshold == null) this.stnThreshold = 1.0d;
        if (this.minPeakHeight == null) this.minPeakHeight = 10000d;
        if (this.minPeakWidth == null) this.minPeakWidth = 0.02d;
        if (this.minPeakPoints == null) this.minPeakPoints = 7;
        if (this.maxZeroPointRatio == null) this.maxZeroPointRatio = 0.5d;
        if (this.minObviousness == null) this.minObviousness = 1.0d;
        if (this.firstDerivativeCutoffFactor == null) this.firstDerivativeCutoffFactor = 0.05d;
    }

    public void updateParams(PeakFindingOptions params) {
        if (StringUtils.isNotEmpty(params.smoothMethod)) this.smoothMethod = params.smoothMethod;
        if (params.smoothPoints != null) this.smoothPoints = params.smoothPoints;
        if (StringUtils.isNotEmpty(params.peakFindingMethod)) this.peakFindingMethod = params.peakFindingMethod;
        if (StringUtils.isNotEmpty(params.baselineMethod)) this.baselineMethod = params.baselineMethod;
        if (params.baselineRtTolerance != null) this.baselineRtTolerance = params.baselineRtTolerance;
        if (StringUtils.isNotEmpty(params.eicNoiseMethod)) this.eicNoiseMethod = params.eicNoiseMethod;
        if (params.noiseAmplitude != null) this.noiseAmplitude = params.noiseAmplitude;
        if (params.noisePercentage != null) this.noisePercentage = params.noisePercentage;
        if (StringUtils.isNotEmpty(params.peakNoiseMethod)) this.peakNoiseMethod = params.peakNoiseMethod;
        if (params.stnThreshold != null) this.stnThreshold = params.stnThreshold;
        if (params.minPeakHeight != null) this.minPeakHeight = params.minPeakHeight;
        if (params.minPeakWidth != null) this.minPeakWidth = params.minPeakWidth;
        if (params.minPeakPoints != null) this.minPeakPoints = params.minPeakPoints;
        if (params.maxZeroPointRatio != null) this.maxZeroPointRatio = params.maxZeroPointRatio;
        if (params.minObviousness != null) this.minObviousness = params.minObviousness;
        if (params.firstDerivativeCutoffFactor != null)
            this.firstDerivativeCutoffFactor = params.firstDerivativeCutoffFactor;
    }
}
