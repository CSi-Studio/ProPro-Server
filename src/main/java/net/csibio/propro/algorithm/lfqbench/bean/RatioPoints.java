package net.csibio.propro.algorithm.lfqbench.bean;

import lombok.Data;

import java.util.List;

@Data
public class RatioPoints<T> {

    List<T> human;
    List<T> yeast;
    List<T> ecoli;

    public RatioPoints(List<T> human, List<T> yeast, List<T> ecoli) {
        this.human = human;
        this.yeast = yeast;
        this.ecoli = ecoli;
    }
}
