package net.csibio.propro.excel.peptide;

import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.List;

@Slf4j
public class PeptideCellHandler implements CellWriteHandler {

    List<PeptideRow> rowList;

    //实验数据从第几列开始展示,从0开始计数
    int startIndex;

    public PeptideCellHandler(List<PeptideRow> rowList, Integer startIndex) {
        this.rowList = rowList;
        this.startIndex = 3;
    }

    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Head head, Integer integer, Integer integer1, Boolean aBoolean) {

    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Cell cell, Head head, Integer integer, Boolean aBoolean) {

    }

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, CellData cellData, Cell cell, Head head, Integer integer, Boolean aBoolean) {
        if (head.getColumnIndex() >= startIndex) {
//            int status = rowList.get(integer).getStatusList().get(head.getColumnIndex() - startIndex);
//            if (status == IdentifyStatus.SUCCESS.getCode()) {
//                cell.getCellStyle().setFillBackgroundColor(HSSFColor.HSSFColorPredefined.GREEN.getIndex());
//            } else if (status == IdentifyStatus.FAILED.getCode()) {
//                cell.getCellStyle().setFillBackgroundColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
//            } else if (status == IdentifyStatus.NO_ENOUGH_FRAGMENTS.getCode() || status == IdentifyStatus.NO_PEAK_GROUP_FIND.getCode()) {
//                cell.getCellStyle().setFillBackgroundColor(HSSFColor.HSSFColorPredefined.YELLOW.getIndex());
//            } else {
//                cell.getCellStyle().setFillBackgroundColor(HSSFColor.HSSFColorPredefined.PINK.getIndex());
//            }
//            cell.setCellValue(0d);
        }
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<CellData> list, Cell cell, Head head, Integer integer, Boolean aBoolean) {

    }
}
