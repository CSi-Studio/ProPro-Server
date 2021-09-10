package net.csibio.propro.domain.bean.peptide;

import java.util.List;

public record ProteinPeptide(List<String> proteins, String peptideRef, Boolean isUnique) {
}
