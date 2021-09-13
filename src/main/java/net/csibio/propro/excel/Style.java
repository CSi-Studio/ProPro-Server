package net.csibio.propro.excel;

import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;

public class Style {

    public static WriteCellStyle buildHeaderStyle() {
        //设置header字体与大小
        WriteCellStyle headStyle = new WriteCellStyle();
        WriteFont headFont = new WriteFont();
        headFont.setFontName("微软雅黑");
        headFont.setFontHeightInPoints((short) 11);
        headStyle.setWriteFont(headFont);
        return headStyle;
    }

    public static WriteCellStyle buildContentStyle() {
        WriteCellStyle contentStyle = new WriteCellStyle();
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName("微软雅黑");
        contentFont.setFontHeightInPoints((short) 11);
        contentStyle.setWriteFont(contentFont);
        return contentStyle;
    }

    public static CellStyle buildCellStyle(short color) {
        CellStyle cellStyle = new HSSFWorkbook().createCellStyle();
        cellStyle.setFillBackgroundColor(color);
        return cellStyle;
    }
}
