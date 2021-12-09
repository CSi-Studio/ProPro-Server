package net.csibio.propro.domain.bean.score;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.List;
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

    //所有碎片的最终的强度总和
    double intensitySum;

    //算法选定的峰形范围左侧最合适的RT
    double leftRt;
    //算法选定的峰形范围右侧最合适的RT
    double rightRt;
    //定量拟合值
    Double fitIntSum;
    //ms1定量值
    Double ms1Sum;
    //拥有最佳洗脱曲线的碎片
    String bestIon;

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
    Double[] ms1Ints;
    @Transient
    Double[] selfInts;
    @Transient
    HashMap<String, Double> ionIntensity; //各个cutInfo在该peakGroup范围内的intensity总和
    @Transient
    HashMap<String, Double> apexIonsIntensity; //各个cutInfo在apex处的intensity
    @Transient
    Boolean notMine = false;

    public PeakGroup() {
    }

    public PeakGroup(double leftRt, double rightRt) {
        this.leftRt = leftRt;
        this.rightRt = rightRt;
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
        double shapeAvg = (this.get(ScoreType.CorrShape, ScoreType.usedScoreTypes()) + this.get(ScoreType.CorrShapeW, ScoreType.usedScoreTypes())) / 2;
        double libDotProd = this.get(ScoreType.Dotprod, ScoreType.usedScoreTypes());
        double libCorr = this.get(ScoreType.Pearson, ScoreType.usedScoreTypes());
        double ionsCount = this.get(ScoreType.IonsDelta, ScoreType.usedScoreTypes());
        double coelutionWeight = this.get(ScoreType.CorrCoeW, ScoreType.usedScoreTypes());
        double isoForward = this.get(ScoreType.IsoCorr, ScoreType.usedScoreTypes());
        double isoBack = this.get(ScoreType.IsoOverlap, ScoreType.usedScoreTypes());

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
        double shapeAvg = (this.get(ScoreType.CorrShape, ScoreType.usedScoreTypes()) + this.get(ScoreType.CorrShapeW, ScoreType.usedScoreTypes())) / 2;
        double libDotProd = this.get(ScoreType.Dotprod, ScoreType.usedScoreTypes());
        double libCorr = this.get(ScoreType.Pearson, ScoreType.usedScoreTypes());
        double ionsCount = this.get(ScoreType.IonsDelta, ScoreType.usedScoreTypes());
        double coelutionWeight = this.get(ScoreType.CorrCoeW, ScoreType.usedScoreTypes());
        double isoForward = this.get(ScoreType.IsoCorr, ScoreType.usedScoreTypes());
        double isoBack = this.get(ScoreType.IsoOverlap, ScoreType.usedScoreTypes());

        //Shape分和DotProd分数都十分优秀的进入筛选轮
        boolean condition1 = shapeAvg > 0.9 && libDotProd > 0.9 && ionsCount < 0.2 && isoForward > 0.8 && isoBack < 0.05;
        if (condition1) {
            return true;
        }
        return false;
    }

    public double getTotal() {
        return this.get(ScoreType.CorrShapeW, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.Pearson, ScoreType.usedScoreTypes()) +
                this.get(ScoreType.Dotprod, ScoreType.usedScoreTypes()) -
                this.get(ScoreType.IonsDelta, ScoreType.usedScoreTypes()) -
                this.get(ScoreType.IsoOverlap, ScoreType.usedScoreTypes())
                ;
    }

    public void remove(String cutInfo) {
        this.getIonIntensity().remove(cutInfo);
        this.getIonHullInt().remove(cutInfo);
        this.getApexIonsIntensity().remove(cutInfo);
    }

    public void remove(List<String> cutInfos) {
        for (String cutInfo : cutInfos) {
            this.setIntensitySum(this.getIntensitySum() - this.getIonIntensity().get(cutInfo));
            this.getIonIntensity().remove(cutInfo);
            this.getIonHullInt().remove(cutInfo);
            this.getApexIonsIntensity().remove(cutInfo);
        }
    }
}
