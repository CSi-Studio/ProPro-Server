package net.csibio.propro.domain.bean.score;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Time: 2018-08-05 22:42
 *
 * @author Nico Wang Ruimin
 */
@Data
public class PeakGroupScore extends BaseScores {

    /**
     * 检测出的该峰的峰顶的rt时间
     */
    Double rt;

    //最接近rt的光谱rt值
    double nearestRt;

    //rt begin;rt end
    String rtRangeFeature;

    //定量值
    Double intensitySum;

    //HashMap --> String,存储每一个fragment的信号强度,使用String的方式进行存储以提升从数据库中读取时的速度
    String fragIntFeature;

    //原BYSeries分数,ApexRt处所有的可能的离子碎片数目
    Integer ions50;
    //强度最高的碎片cutInfo
    String maxIon;
    //强度最高的碎片的强度
    Double maxIonIntensity;

    //中间计算变量,不需要存入数据库
    @Transient
    Boolean thresholdPassed;
    @Transient
    HashMap<String, Double> ionIntensity;
    @Transient
    Boolean mark = false;
    @Transient
    Boolean changed = false;

    public PeakGroupScore() {
    }

    public PeakGroupScore(int scoreTypesSize) {
        this.scores = new Double[scoreTypesSize];
    }

    public boolean fine() {
        AtomicBoolean allHit = new AtomicBoolean(true);
        ionIntensity.values().forEach(value -> {
            if (value == 0) {
                allHit.set(false);
            }
        });
        boolean condition1 = (this.get(ScoreType.XcorrShape, ScoreType.usedScoreTypes()) + this.get(ScoreType.XcorrShapeWeighted, ScoreType.usedScoreTypes())) / 2 > 0.8;
        boolean condition2 = this.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes()) > 0.9;
        boolean condition3 = this.get(ScoreType.IsotopeCorrelationScore, ScoreType.usedScoreTypes()) > 0.6;
        return allHit.get() && condition1 && condition2 && condition3;
    }

    public double total() {
        return this.get(ScoreType.XcorrShape, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.XcorrShapeWeighted, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.IsotopeCorrelationScore, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes()) -
                this.get(ScoreType.IonsCountDeltaScore, ScoreType.usedScoreTypes())
                ;
    }

}
