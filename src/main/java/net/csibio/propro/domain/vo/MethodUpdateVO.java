package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.constants.enums.*;

import java.util.List;

@Data
public class MethodUpdateVO {

    // 用于更新操作时使用的id
    String id;

    //*********************************基本参数****************************************************//
    String name; //放发包名称
    String description; //参数备注

    //*********************************EIC参数****************************************************//
    Double mzWindow = 15d; //MZ窗口,为a时表示的是±a,单位是ppm
    Double rtWindow = 300d; //RT窗口,为300时表示的是 ±300
    Double extraRtWindow = 200d; //当在rtWindow窗口内没有搜索到峰的时候,会扩展RT窗口进一步搜索,为300时表示的是 ±300
    Float ionsLow = 50f; //计算IonsCountLow时的最小强度值
    Float ionsHigh = 300f; //计算IonsCountHigh时的最小强度值
    Integer maxIons = 6;

    //*********************************Irt参数(需要包含基本参数)****************************************************//
    Double minShapeScoreForIrt = 0.95d; //当useAnaLibForIrt为true时生效,表示用于做irt时检测到的峰的shape分数的最小值
    Integer pickedNumbers = 500; //当useAnaLibForIrt为true时生效,从数据库中随机取出的点的数目,越少速度越快,但是容易出现没有命中的情况,当出现没有命中的情况是,最终的采样点数会少于设定的collectNumbers数目,为null的时候表示全部取出不限制数目
    int wantedNumber = 50; //当useAnaLibForIrt为true时生效, 使用标准库进行查询时的采样点数目,默认为50个点位,不能为空

    //*********************************选峰参数****************************************************//
    String smoothMethod = SmoothMethod.PROPRO_GAUSS.getName();
    Integer smoothPoints = 5; //平滑点数
    String peakFindingMethod = PeakFindingMethod.IONS_COUNT.getName();
    String baselineMethod = BaselineMethod.TOLERANCE.getName();
    Double baselineRtWindow = 0.2d; //baselineRt窗口大小
    String eicNoiseMethod = NoiseEstimateMethod.PROPRO_EIC.getName();
    Double noiseAmplitude = 10000d; //噪声振幅
    Double noisePercentage = 20d; //噪声百分比
    String peakNoiseMethod = NoiseEstimateMethod.SLIDING_WINDOW_PEAK.getName();
    Double stnThreshold = 1d; //信噪比阈值
    Double minPeakHeight = 10000d; //最小峰高
    Double minPeakWidth = 0.02d; //最小峰宽
    Integer minPeakPoints = 5; //最少含有的点数
    Double maxZeroPointRatio = 0.5d; //最大噪声信号点比例  //??0.1,135 0.2,81 0.3,61 0.4,55 1,50
    Double minObviousness = 1.0d; //最小显著程度
    Double firstDerivativeCutoffFactor = 0.05d; //峰边界一阶导数与最大一阶导数的比例系数

    //*********************************打分参数****************************************************//
    List<String> scoreTypes = ScoreType.getAllTypesName(); //打分类型,详情见net.csibio.propro.algorithm.score.ScoreType

    //*********************************回归参数****************************************************//
    String classifier = Classifier.LDA.name(); //回归用的算法分类器 @see net.csibio.propro.constants.enums.Classifier
    Double fdr = 0.01d; //筛选的FDR值,默认值为0.01
}
