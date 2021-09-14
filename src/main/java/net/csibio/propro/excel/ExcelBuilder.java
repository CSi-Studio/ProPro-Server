package net.csibio.propro.excel;

import com.alibaba.excel.write.handler.CellWriteHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class ExcelBuilder<H, R> {

    public String output;
    public List<List<String>> headers = new ArrayList<>();
    public List<List<Object>> contents = new ArrayList<>();
    public CellWriteHandler cellWriteHandler;
    
    public abstract ExcelBuilder buildOutputPath(String name);

    public abstract ExcelBuilder buildHeader(List<H> headers);

    public abstract ExcelBuilder buildRowList(List<R> rowList);

    public abstract void export();
}
