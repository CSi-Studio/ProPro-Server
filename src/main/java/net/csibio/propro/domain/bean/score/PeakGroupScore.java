package net.csibio.propro.domain.bean.score;

import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;

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
    Integer totalIons;
    //强度最高的碎片cutInfo
    String maxIon;
    //强度最高的碎片的强度
    Double maxIonIntensity;
    
    @Transient
    Boolean thresholdPassed;

    @Transient
    HashMap<String, Double> ionIntensity;

    @Transient
    Boolean mark = false;

    public PeakGroupScore() {
    }

    public PeakGroupScore(int scoreTypesSize) {
        this.scores = new Double[scoreTypesSize];
    }

}
