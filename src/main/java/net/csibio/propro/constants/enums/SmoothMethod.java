package net.csibio.propro.constants.enums;

public enum SmoothMethod {

    LINEAR("LINEAR"),
    GAUSS("GAUSS"),
    SAVITZKY_GOLAY("SAVITZKY_GOLAY"),
    PROPRO_GAUSS("PROPRO_GAUSS"),
    NONE("NONE");

    String name;

    SmoothMethod(String name) {
        this.name = name;
    }

    public static SmoothMethod getByName(String name) {
        for (SmoothMethod method : values()) {
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
