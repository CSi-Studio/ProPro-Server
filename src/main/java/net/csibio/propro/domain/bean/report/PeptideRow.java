package net.csibio.propro.domain.bean.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PeptideRow<T> {

    String proteins;

    String peptide;

    //每一个overview对应的结果
    List<T> dataList = new ArrayList<>();
}
