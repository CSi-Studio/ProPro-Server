package net.csibio.propro.domain.bean.peptide;

import lombok.Data;

@Data
public class ProteinPeptide {

    String proteinName;

    String peptideRef;

    Boolean isUnique;
}
