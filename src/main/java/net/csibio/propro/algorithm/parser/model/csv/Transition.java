package net.csibio.propro.algorithm.parser.model.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class Transition {

    @CsvBindByName(column = "precursormz")
    Double precursorMz;

    @CsvBindByName(column = "productmz")
    Double productMz;

    @CsvBindByName(column = "tr_recalibrated")
    Double normalizedRetentionTime;

    @CsvBindByName(column = "transition_name")
    String transitionName;

    @CsvBindByName(column = "transition_group_id")
    String transitionGroupId;

    @CsvBindByName(column = "uniprotid")
    String uniprotId;

    @CsvBindByName(column = "decoy")
    Integer decoy;

    @CsvBindByName(column = "libraryintensity")
    Double productIonIntensity;

    @CsvBindByName(column = "peptidesequence")
    String peptideSequence;

    @CsvBindByName(column = "proteinname")
    String proteinName;

    @CsvBindByName(column = "annotation")
    String annotation;

    @CsvBindByName(column = "fullunimodpeptidename")
    String fullUniModPeptideName;

    @CsvBindByName(column = "precursorcharge")
    Integer precursorCharge;

    @CsvBindByName(column = "fragmenttype")
    String fragmentType;

    @CsvBindByName(column = "fragmentcharge")
    String fragmentCharge;

    @CsvBindByName(column = "fragmentseriesnumber")
    String fragmentSeriesNumber;

    @CsvBindByName(column = "fragmentlosstype")
    String fragmentLossType;

    @CsvBindByName(column = "detecting_transition")
    String detecting;
    @CsvBindByName(column = "identifying_transition")
    String identifying;
    @CsvBindByName(column = "quantifying_transition")
    String quantifying;
}
