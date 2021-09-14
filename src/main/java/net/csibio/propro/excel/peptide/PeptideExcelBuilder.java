package net.csibio.propro.excel.peptide;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.google.common.collect.Lists;
import net.csibio.propro.constants.constant.PrefixConst;
import net.csibio.propro.excel.ExcelBuilder;
import net.csibio.propro.excel.Style;
import net.csibio.propro.utils.RepositoryUtil;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PeptideExcelBuilder extends ExcelBuilder<String, PeptideRow> {

    public PeptideExcelBuilder(String projectName, List<String> headerInfos, List<PeptideRow> rowList) {
        buildOutputPath(projectName);
        buildHeader(headerInfos);
        buildRowList(rowList);
        this.cellWriteHandler = new PeptideCellHandler(rowList, 3);
    }

    @Override
    public ExcelBuilder buildOutputPath(String name) {
        String outputPath = RepositoryUtil.getExport(name);
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //output = outputPath + PrefixConst.REPORT + name + ".xlsx";
        output = FilenameUtils.concat(outputPath, PrefixConst.REPORT + name + ".xlsx");
        File targetExcel = new File(output);
        //如果文件已经存在,那么换一个名字,加上时间戳
        if (targetExcel.exists()) {
            output = FilenameUtils.concat(outputPath, PrefixConst.REPORT + name + "_" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + ".xlsx");
        }

        return this;
    }

    @Override
    public ExcelBuilder buildHeader(List<String> headerInfos) {
        headers.add(Lists.newArrayList("Protein"));
        headers.add(Lists.newArrayList("Peptide"));
        headers.add(Lists.newArrayList("Unique"));
        headerInfos.forEach(info -> {
            headers.add(Lists.newArrayList(info));
        });
        return this;
    }

    @Override
    public ExcelBuilder buildRowList(List<PeptideRow> rowList) {
        for (int i = 0; i < rowList.size(); i++) {
            PeptideRow peptideRow = rowList.get(i);
            List<Object> row = new ArrayList<>();
            row.add(String.join(",", peptideRow.getProteins()));
            row.add(peptideRow.getPeptide());
            row.add(peptideRow.getProteins().size() == 1);
            for (int j = 0; j < peptideRow.sumList.size(); j++) {
                row.add(peptideRow.sumList.get(j));
            }
            contents.add(row);
        }
        return this;
    }

    @Override
    public void export() {
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(output).build();
            WriteCellStyle headStyle = Style.buildHeaderStyle(); //设置header字体与大小
            WriteCellStyle contentStyle = Style.buildContentStyle(); //设置content字体与大小
            HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(headStyle, contentStyle);
            WriteSheet sheet = EasyExcel.writerSheet().registerWriteHandler(horizontalCellStyleStrategy).registerWriteHandler(cellWriteHandler).head(headers).build();
            excelWriter.write(contents, sheet);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }
}
