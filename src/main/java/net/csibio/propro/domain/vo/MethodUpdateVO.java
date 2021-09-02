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
    Double mzWindow = 0.015d; //MZ窗口,为a时表示的是±a
    Boolean adaptiveMzWindow = false; //是否使用自适应mz窗口,自适应mz算
    Double rtWindow = 300d; //RT窗口,为300时表示的是 ±300

    //*********************************Irt参数(需要包含基本参数)****************************************************//
    Boolean useAnaLibForIrt = false;  //是否使用标准库来做Irt校准
    String anaLibForIrt; //当useAnaLibForIrt为true时生效,表示用于做irt的标准库的id
    Double minShapeScoreForIrt = 0.95d; //当useAnaLibForIrt为true时生效,表示用于做irt时检测到的峰的shape分数的最小值
    Integer pickedNumbers = 500; //当useAnaLibForIrt为true时生效,从数据库中随机取出的点的数目,越少速度越快,但是容易出现没有命中的情况,当出现没有命中的情况是,最终的采样点数会少于设定的collectNumbers数目,为null的时候表示全部取出不限制数目
    int wantedNumber = 50; //当useAnaLibForIrt为true时生效, 使用标准库进行查询时的采样点数目,默认为50个点位,不能为空

    //*********************************选峰参数****************************************************//
    String smoothMethod = SmoothMethod.PROPRO_GAUSS.getName();
    Integer smoothPoints = 5; //平滑点数
    String peakFindingMethod = PeakFindingMethod.WAVELET.getName();
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

    //*********************************快筛参数****************************************************//
    Double minShapeScore = 0.6d; // shape的筛选阈值,一般建议在0.6左右
    Double minShapeWeightScore = 0.8d; //shape的筛选阈值,一般建议在0.8左右

    //*********************************打分参数****************************************************//
    List<String> scoreTypes = ScoreType.getAllTypesName(); //打分类型,详情见net.csibio.propro.algorithm.score.ScoreType
    boolean diaScores = true; //是否使用DIA打分,如果使用DIA打分的话,需要提前读取Aird文件中的谱图信息以提升系统运算速度

    //*********************************回归参数****************************************************//
    String classifier = Classifier.lda.name(); //回归用的算法分类器 @see net.csibio.propro.constants.enums.Classifier
    Double fdr = 0.01d; //筛选的FDR值,默认值为0.01
    boolean removeUnmatched = false; //是否删除fdr不符合阈值的结果,默认不删除
}
