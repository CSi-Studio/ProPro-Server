package net.csibio.propro.domain.options;

import lombok.Data;

@Data
public class EicOptions {

    Double mzWindow = 0.015d; //MZ窗口,为a时表示的是±a
    Boolean adaptiveMzWindow = false; //是否使用自适应mz窗口,自适应mz算
    Double rtWindow = 300d; //RT窗口,为600时表示的是 ±300
}
