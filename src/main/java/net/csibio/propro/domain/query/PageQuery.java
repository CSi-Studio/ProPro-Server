package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;
import net.csibio.propro.constants.constant.SymbolConst;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * @author fengzhi
 * Date 2017-01-03
 */
@Data
@Accessors(chain = true)
public class PageQuery implements Serializable {

    private static final long serialVersionUID = -8745138167696978267L;

    protected long current = 1;
    protected int pageSize = 20;
    protected long totalNum = 0;

    protected Sort.Direction orderBy = null;
    protected String sortColumn = null;
    protected String sorter;
    //是否使用estimateCount, 默认为false,即使用正常的count方法
    protected Boolean estimateCount = false;

    protected PageQuery() {
    }

    public PageQuery(int pageNo, int pageSize) {
        this.current = pageNo;
        this.pageSize = pageSize;
    }

    public PageQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn) {
        this.current = pageNo;
        this.pageSize = pageSize;
        this.sortColumn = sortColumn;
        this.orderBy = direction;
    }

    public void setPageNo(final int pageNo) {
        this.current = pageNo;

        if (pageNo < 1) {
            this.current= 1;
        }
    }

    public Long getFirst() {
        return (getCurrent() > 0 && getPageSize() > 0) ? ((getCurrent() - 1) * getPageSize()) : 0;
    }

    public Long getLast() {
        return (getFirst() + getPageSize() - 1);
    }

    public long getTotalPage() {
        if (this.pageSize > 0 && this.totalNum > 0) {
            return (this.totalNum % this.pageSize == 0 ? (this.totalNum / this.pageSize) : (this.totalNum / this.pageSize + 1));
        } else {
            return 0;
        }
    }

    public void parseSorter(String sorter) {
        String[] sorters = sorter.split("/");
        if (sorters.length != 2) {
            return;
        }
        if (sorters[1].equals("ascend")) {
            this.setOrderBy(Sort.Direction.ASC);
        } else {
            this.setOrderBy(Sort.Direction.DESC);
        }

        if (!sorters[0].contains(SymbolConst.COMMA)) {
            this.sortColumn = sorters[0];
        } else {
            this.sortColumn = sorters[0].replace(SymbolConst.COMMA, SymbolConst.DOT);
        }
    }
}
