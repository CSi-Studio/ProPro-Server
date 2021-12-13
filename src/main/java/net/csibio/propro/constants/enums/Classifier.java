package net.csibio.propro.constants.enums;

public enum Classifier {
    LDA("LDA"),
    XGBoost("XGBoost");

    String name;

    Classifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
