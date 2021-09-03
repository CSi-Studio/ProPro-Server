package net.csibio.propro.domain.bean.score;

import lombok.Data;

/**
 * Time: 2018-08-05 22:42
 *
 * @author Nico Wang Ruimin
 */
@Data
public class PeakGroupScores extends BaseScores {

    /**
     * 检测出的该峰的峰顶的rt时间
     */
    Double rt;

    //rt begin;rt end
    String rtRangeFeature;

    //定量值
    Double intensitySum;

    //HashMap --> String,存储每一个fragment的信号强度,使用String的方式进行存储以提升从数据库中读取时的速度
    String fragIntFeature;

    Boolean thresholdPassed;

    public PeakGroupScores() {
    }

    public PeakGroupScores(int scoreTypesSize) {
        this.scores = new Double[scoreTypesSize];
    }

}
