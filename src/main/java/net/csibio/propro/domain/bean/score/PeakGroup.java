package net.csibio.propro.domain.bean.score;

import lombok.Data;

import java.util.HashMap;

@Data
public class PeakGroup {

    //离子片段的数目
    int ionCount;

    //最高峰顶处(NearestRt处)的光谱图中含有的b,y离子(根据轰击方式确定)的总数
    int ions50;

    //算法得出的顶峰的Rt(不一定在谱图里面存在这个值)
    double apexRt;

    //最接近apexRt的光谱rt值
    float nearestRt;

    //peak group中每一个碎片的最大强度
    HashMap<String, Double> ionIntensity;

    //最终的强度总和
    double peakGroupInt;

    //算法选定的峰形范围左侧最合适的RT
    double bestLeftRt;

    //算法选定的峰形范围右侧最合适的RT
    double bestRightRt;

    //所有离子在所有RT上的Intensity总和
    double totalXic;

    //在算法选定的峰形范围内的Rt和Intensity对
    Double[] ionHullRt;

    HashMap<Double, PeakGroup> childPeakGroup;

    HashMap<String, Double[]> ionHullInt;

    double signalToNoiseSum;

    //最大强度碎片cutInfo
    String maxIon;
    //最大强度碎片的强度
    Double maxIonIntensity;
}
