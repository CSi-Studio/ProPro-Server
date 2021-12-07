package net.csibio.propro.constants.enums;

public enum Classifier {
    lda("LDA"),
    xgboost("XGBoost");

    String name;

    Classifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
