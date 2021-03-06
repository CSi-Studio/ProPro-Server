package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.constants.enums.Classifier;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:05
 */

@Data
public class LearningParams {

    //    Classifier classifier = Classifier.lda;
    String classifier = Classifier.LDA.getName();

    double fdr = Constants.DEFAULT_FDR;

    double trainTestRatio = 1;

    int xevalNumIter = 20;

//    double ssInitialFdr = 0.15;

    double ssIterationFdr = 0.05; //0.1 3310; 0.08 3300; 0.05 3276

    double xgbInitialFdr = 0.01;

    double xgbIterationFdr = 0.008;

    String ssMainScore = "var_xcorr_shape";

    boolean parametric = false;

    boolean pFdr = false;

    Double[] pi0Lambda = {0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45};
//    Double[] pi0Lambda = {0.01, 0.02, 0.03,0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1};

    String pi0Method = "bootstrap";

    boolean pi0SmoothLogPi0 = false;

    int numCutOffs = 51;

    int useSortOrders = 1;

    Double[] qvalues = {0.0, 0.01, 0.02, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5};

    public static boolean isDebug = false;

    //用于打分的子分数模板快照,会和AnalyseDataDO中的每一个FeatureScore中的scores对象做一一映射
    List<String> scoreTypes;

    double progressiveRate = 0.8d;

    String type;
}
