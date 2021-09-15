package net.csibio.propro.algorithm.lfqbench.bean;

import lombok.Data;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

@Data
public class BenchStat<T> {

    //A组鉴定数(唯一肽段)
    long identifyNumA;
    //A组缺失率,缺失值/总数
    long missingRatioA;

    //B组鉴定数(唯一肽段)
    long identifyNumB;
    //B组缺失率,缺失值/总数
    long missingRatioB;

    double humanCV;  //标准偏差
    double humanAvg; //均值
    double humanSD;  //标准差

    double yeastCV;
    double yeastAvg;
    double yeastSD;

    double ecoliCV;
    double ecoliAvg;
    double ecoliSD;

    List<T> human;
    List<T> yeast;
    List<T> ecoli;

    public BenchStat(List<T> human, List<T> yeast, List<T> ecoli) {
        this.human = human;
        this.yeast = yeast;
        this.ecoli = ecoli;
    }

    public void setHumanStat(DescriptiveStatistics human) {
        this.humanAvg = human.getMean();
        this.humanSD = human.getStandardDeviation();
        this.humanCV = humanSD / humanAvg;
    }

    public void setYeastStat(DescriptiveStatistics yeast) {
        this.yeastAvg = yeast.getMean();
        this.yeastSD = yeast.getStandardDeviation();
        this.yeastCV = yeastSD / yeastAvg;
    }

    public void setEcoliStat(DescriptiveStatistics ecoli) {
        this.ecoliAvg = ecoli.getMean();
        this.ecoliSD = ecoli.getStandardDeviation();
        this.ecoliCV = ecoliSD / ecoliAvg;
    }
}
