package net.csibio.propro.domain.bean.data;

import java.util.HashMap;

public record PeptideSpectrum(Double[] rtArray, HashMap<String, Double[]> intensitiesMap) {
}
