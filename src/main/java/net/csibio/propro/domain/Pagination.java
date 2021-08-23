package net.csibio.propro.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "Page")
public class Pagination implements Serializable {

    private static final long serialVersionUID = 1738821890566027418L;

    /**
     * 总计全部数目
     */
    long total;

    /**
     * 每页显示个数
     */
    int pageSize = 10;

    /**
     * 当前访问页面号
     */
    long current;

    public Pagination() {
    }

    public Pagination(long total, int pageSize, long current) {
        this.total = total;
        this.pageSize = pageSize;
        this.current = current;
    }
}
