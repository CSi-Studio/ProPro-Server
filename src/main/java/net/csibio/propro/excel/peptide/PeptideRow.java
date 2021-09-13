package net.csibio.propro.excel.peptide;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PeptideRow {

    List<String> proteins;

    String peptide;

    //每一个exp-overview对应的定量
    List<Double> sumList = new ArrayList<>();

    //每一个exp-overveiw对应的鉴定状态
    List<Integer> statusList = new ArrayList<>();
}
