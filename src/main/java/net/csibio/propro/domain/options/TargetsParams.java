package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.domain.bean.score.SlopeIntercept;

@Data
public class TargetsParams {

    //斜率截距
    SlopeIntercept si;

    //RT卷积窗口
    Double rtWindow;

//    //仅用于PRM实验类型时使用
//    Float[] rtRange;

    //实验类型
//    String type;

    //是否包含伪肽段,null代表全部都获取
    Boolean noDecoy;

    //需要构建的肽段列表限制数目,如果为null则表明不限制,当使用标准库进行预测内标抓取时需要限制的肽段数目
    Integer limit;

}
