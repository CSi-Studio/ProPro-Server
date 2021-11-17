package net.csibio.propro.domain.bean.score;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PeakGroup extends BaseScores {

    //低信号量总碎片数目
    int ionsLow;
    //高信号量总碎片数目
    int ionsHigh;

    //算法得出的顶峰的Rt(不一定在谱图里面存在这个值)
    Double apexRt;

    //最接近apexRt的光谱rt值
    Double selectedRt;

    //最终的强度总和
    double intensitySum;

    //算法选定的峰形范围左侧最合适的RT
    double leftRt;
    //算法选定的峰形范围右侧最合适的RT
    double rightRt;

    //最大强度碎片cutInfo
    String maxIon;
    //最大强度碎片的强度
    Double maxIonIntensity;

    //中间计算变量,不需要存入数据库
    @Transient
    double tic;  //所有离子在所有RT上的Intensity总和
    @Transient
    double signalToNoiseSum;
    @Transient
    Double[] ionHullRt;     //在算法选定的峰形范围内的Rt和Intensity对
    @Transient
    HashMap<String, Double[]> ionHullInt;
    @Transient
    HashMap<String, Double> ionIntensity;
    @Transient
    Boolean notMine = false;

    public PeakGroup() {
    }

    public PeakGroup(int scoreSize) {
        this.scores = new Double[scoreSize];
    }

    public void initScore(int scoreSize) {
        this.scores = new Double[scoreSize];
    }

    /**
     * 必须要满足的条件
     *
     * @return
     */
    public boolean base() {
        if (ionIntensity != null) {
            AtomicBoolean allHit = new AtomicBoolean(true);
            ionIntensity.values().forEach(value -> {
                if (value == 0) {
                    allHit.set(false);
                }
            });
            if (!allHit.get()) {
                return false;
            }
        }
        return true;
    }

    public boolean getFine() {
        if (!base()) {
            return false;
        }
        double shapeAvg = (this.get(ScoreType.XcorrShape, ScoreType.usedScoreTypes()) + this.get(ScoreType.XcorrShapeWeighted, ScoreType.usedScoreTypes())) / 2;
        double libDotProd = this.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes());
        double libCorr = this.get(ScoreType.LibraryCorr, ScoreType.usedScoreTypes());
        double ionsCount = this.get(ScoreType.IonsCountDeltaScore, ScoreType.usedScoreTypes());
        double coelutionWeight = this.get(ScoreType.XcorrCoelutionWeighted, ScoreType.usedScoreTypes());
        double isoForward = this.get(ScoreType.IsotopeCorrelationScore, ScoreType.usedScoreTypes());
        double isoBack = this.get(ScoreType.IsotopeOverlapScore, ScoreType.usedScoreTypes());

        //Shape分和DotProd分数都十分优秀的进入筛选轮
        boolean condition1 = shapeAvg > 0.85 && libDotProd > 0.9 && ionsCount < 0.2 && isoForward > 0.8 && isoBack < 0.05;
        if (condition1) {
            return true;
        }

        return false;
    }

    public boolean ionsDisturb() {
        if (!base()) {
            return false;
        }
        double shapeAvg = (this.get(ScoreType.XcorrShape, ScoreType.usedScoreTypes()) + this.get(ScoreType.XcorrShapeWeighted, ScoreType.usedScoreTypes())) / 2;
        double libDotProd = this.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes());
        double libCorr = this.get(ScoreType.LibraryCorr, ScoreType.usedScoreTypes());
        double ionsCount = this.get(ScoreType.IonsCountDeltaScore, ScoreType.usedScoreTypes());
        double coelutionWeight = this.get(ScoreType.XcorrCoelutionWeighted, ScoreType.usedScoreTypes());
        double isoForward = this.get(ScoreType.IsotopeCorrelationScore, ScoreType.usedScoreTypes());
        double isoBack = this.get(ScoreType.IsotopeOverlapScore, ScoreType.usedScoreTypes());

        //Shape分和DotProd分数都十分优秀的进入筛选轮
        boolean condition1 = shapeAvg > 0.9 && libDotProd > 0.9 && ionsCount < 0.2 && isoForward > 0.8 && isoBack < 0.05;
        if (condition1) {
            return true;
        }

        return false;
    }

    public double getTotal() {
        return this.get(ScoreType.XcorrShape, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.XcorrShapeWeighted, ScoreType.usedScoreTypes()) +
//                this.get(ScoreType.LibraryCorr, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.LibraryDotprod, ScoreType.usedScoreTypes()) -
                this.get(ScoreType.IonsCountDeltaScore, ScoreType.usedScoreTypes()) -
//                this.get(ScoreType.XcorrCoelutionWeighted, ScoreType.usedScoreTypes()) -
                this.get(ScoreType.IsotopeOverlapScore, ScoreType.usedScoreTypes())
                ;
    }
}
