package net.csibio.propro.constants.enums;

/**
 * Created by Nico Wang
 * Time: 2020-03-12 09:36
 */
public enum BaselineMethod {
    TOLERANCE("TOLERANCE"),
    NONE("NONE");


    String name;

    BaselineMethod(String name) {
        this.name = name;
    }

    public static BaselineMethod getByName(String name) {
        for (BaselineMethod method : values()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    public String getName() {
        return this.name;
    }
}
