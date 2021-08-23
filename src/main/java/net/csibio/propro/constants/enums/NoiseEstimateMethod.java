package net.csibio.propro.constants.enums;

/**
 * Created by Nico Wang
 * Time: 2020-03-10 15:54
 */
public enum NoiseEstimateMethod {
    SLIDING_WINDOW_PEAK("SLIDING_WINDOW_PEAK", "Sliding window peak noise estimator"),
    WAVELET_COEFF_PEAK("WAVELET_COEFF_PEAK", "Wavelet peak noise estimator, only suitable for wavelet peak picking algorithm"),
    PROPRO_EIC("PROPRO_EIC", "Propro eic noise estimator, generate dynamic noise array"),
    AMPLITUDE_EIC("AMPLITUDE_EIC", "Estimate noise by given noise amplitude."),
    PERCENTAGE_EIC("PERCENTAGE_EIC", "determine noise threshold by percentile of all intensities in eic.");

    String name;
    String description;

    NoiseEstimateMethod(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static NoiseEstimateMethod getByName(String name) {
        for (NoiseEstimateMethod method : values()) {
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
