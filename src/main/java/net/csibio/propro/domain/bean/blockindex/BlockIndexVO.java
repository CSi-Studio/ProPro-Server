package net.csibio.propro.domain.bean.blockindex;

import lombok.Data;
import net.csibio.aird.bean.WindowRange;

@Data
public class BlockIndexVO {

    String id;

    String expId;

    /**
     * 1: ms1 swath block, 2: ms2 swath block
     */
    Integer level;
    /**
     * 在文件中的开始位置
     */
    Long startPtr;
    /**
     * 在文件中的结束位置
     */
    Long endPtr;
    /**
     * SWATH块对应的WindowRange
     */
    WindowRange range;

    /**
     * 用于存储KV键值对
     */
    String features;
}
