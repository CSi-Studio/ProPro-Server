package net.csibio.propro.domain.vo;

import lombok.Data;

@Data
public class ExpVO {

    String id;

    String projectId;

    String name;

    String alias;

    //DIA_SWATH, PRM, SCANNING_SWATH @see ExpType
    String type;

    //Aird文件大小,单位byte
    Long airdSize;

    //Aird索引文件的大小,单位byte
    Long airdIndexSize;

    //原始文件的大小,单位byte
    Long vendorFileSize;
}
