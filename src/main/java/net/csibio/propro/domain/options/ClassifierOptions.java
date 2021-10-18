package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.constants.enums.Classifier;

@Data
public class ClassifierOptions {

    String algorithm = Classifier.lda.name();

    Double fdr = 0.01d;
}
