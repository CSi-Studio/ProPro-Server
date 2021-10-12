package net.csibio.propro.algorithm.lfqbench.bean;

import lombok.Data;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

@Data
public class BenchStat<T> {


    long identifyNumA; //A组鉴定数(唯一肽段)
    long identifyProteinNumA; //A组鉴定数(唯一肽段)
    double missingRatioA; //A组缺失率,缺失值/总数
    int hit1A; //单次命中
    int hit2A; //两次命中
    int hit3A; //三次命中

    long identifyNumB; //B组鉴定数(唯一肽段)
    long identifyProteinNumB; //B组鉴定数(唯一肽段)
    double missingRatioB;  //B组缺失率,缺失值/总数
    int hit1B; //单次命中
    int hit2B; //两次命中
    int hit3B; //三次命中

    double humanCV;  //标准偏差
    double humanAvg; //均值
    double humanSD;  //标准差
    List<Double> humanPercentile = new ArrayList<>(); //1-99 percentile

    double yeastCV;
    double yeastAvg;
    double yeastSD;
    List<Double> yeastPercentile = new ArrayList<>();

    double ecoliCV;
    double ecoliAvg;
    double ecoliSD;
    List<Double> ecoliPercentile = new ArrayList<>();

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
        for (int i = 1; i <= 99; i++) {
            humanPercentile.add(human.getPercentile(i));
        }
    }

    public void setYeastStat(DescriptiveStatistics yeast) {
        this.yeastAvg = yeast.getMean();
        this.yeastSD = yeast.getStandardDeviation();
        this.yeastCV = yeastSD / yeastAvg;
        for (int i = 1; i <= 99; i++) {
            yeastPercentile.add(yeast.getPercentile(i));
        }
    }

    public void setEcoliStat(DescriptiveStatistics ecoli) {
        this.ecoliAvg = ecoli.getMean();
        this.ecoliSD = ecoli.getStandardDeviation();
        this.ecoliCV = ecoliSD / ecoliAvg;
        for (int i = 1; i <= 99; i++) {
            ecoliPercentile.add(ecoli.getPercentile(i));
        }
    }
}
