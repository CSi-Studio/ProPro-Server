package net.csibio.propro.constants.enums;

/**
 * Created by Nico Wang
 * Time: 2020-03-10 16:11
 */
public enum PeakFindingMethod {
    IONS_SHAPE("IONS_SHAPE"),
    IONS_COUNT("IONS_COUNT"),
    MZMINE("MZMINE"),
    WAVELET("WAVELET"),
    LOCAL_MINIMUM("LOCAL_MINIMUM"),
    SAVITZKY_GOLAY("SAVITZKY_GOLAY");
    
    String name;

    PeakFindingMethod(String name) {
        this.name = name;
    }

    public static PeakFindingMethod getByName(String name) {
        for (PeakFindingMethod method : values()) {
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
