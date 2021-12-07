package net.csibio.propro.domain.bean.data;

import lombok.Data;
import net.csibio.propro.domain.bean.common.DoublePair;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.IonPeak;

import java.util.HashMap;
import java.util.List;

@Data
public class UnSearchPeakGroup {

    PeptideCoord coord;
    //当前PeptideRef对应的rt数组[Float版本]
    float[] floatRtArray;
    //当前PeptideRef对应的rt数组[Double版本]
    Double[] rtArray;
    //当前peptideRef对应的强度值,key为cutInfo
    HashMap<String, Double[]> intensitiesMap;
    //ms1 eic intensity图
    Double[] ms1Intensity;
    //每一个cutInfo对应的峰顶数组
    HashMap<String, RtIntensityPairsDouble> maxPeaks4Ions;
    //每一个cutInfo对应的峰组
    HashMap<String, List<IonPeak>> peaks4Ions;
    //每一个cutInfo对应的noise1000信噪比数组
    HashMap<String, double[]> noise1000Map;

    //IonCount相关数据独立列出
    List<DoublePair> maxPeaks4IonsHigh; //left为rt, right为平滑后的ionsCount
    int[] ionsLow; //原始的ions50,数组长度与rtArray一致且一一对应
    int[] ionsHigh; //原始的ions300,数组长度与rtArray一致且一一对应
    Double[] ionsHighSmooth; //平滑后的IonsCount,数组长度与rtArray一致且一一对应
    double[] noise10004IonCount;
}
