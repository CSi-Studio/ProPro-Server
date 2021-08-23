package net.csibio.propro.domain.options;

import lombok.Data;

@Data
public class BuildLibOptions {

    /**
     * 格式: K,R
     */
    String cuts;

    /**
     * 新库名
     */
    String libraryName;

    /**
     * 最短肽段长度
     */
    int minLength;

    /**
     * 最长肽段长度
     */
    int maxLength;
}
