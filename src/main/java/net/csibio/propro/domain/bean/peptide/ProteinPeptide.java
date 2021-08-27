package net.csibio.propro.domain.bean.peptide;

import lombok.Data;

@Data
public class ProteinPeptide {

    String proteinIdentifier;

    String peptideRef;

    Boolean isUnique;
}
