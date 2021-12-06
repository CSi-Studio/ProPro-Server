package net.csibio.propro.domain.options;

import lombok.Data;

@Data
public class EicOptions {

    Double mzWindow = 15d; //MZ窗口,为a时表示的是±a,单位是ppm(百万分之一)
    Double rtWindow = 300d; //RT窗口,为600时表示的是 ±300
    Float ionsLow = 50f; //计算IonsCountLow时的最小强度值
    Float ionsHigh = 300f; //计算IonsCountHigh时的最小强度值
    Integer maxIons = 6;
}
