package net.csibio.propro.domain.bean.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Pairs<T, D> implements Serializable {

    T[] x;
    D[] y;

    public Pairs() {
    }

    public Pairs(T[] x, D[] y) {
        this.x = x;
        this.y = y;
    }

    public int length() {
        if (x == null) {
            return 0;
        }
        return x.length;
    }

    public T[] getX() {
        return x;
    }

    public D[] getY() {
        return y;
    }
}
