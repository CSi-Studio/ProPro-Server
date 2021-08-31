package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.constants.enums.Classifier;

@Data
public class ClassifierOptions {

    String algorithm = Classifier.lda.name();

    Double fdr = 0.01d;

    //最终结果中是否需要从数据库中移出fdr不符合阈值的结果,以节省数据库空间,默认不移除
    Boolean removeUnmatched = false;
}
