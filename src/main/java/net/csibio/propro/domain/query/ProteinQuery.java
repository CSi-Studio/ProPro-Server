package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProteinQuery extends PageQuery {

    String id;

    /**
     * 蛋白质标识符,第一位是来源数据库,第二位是Access号,第三位是名称
     * 如果需要通过UniProt进行查询,可以使用第二位Access进行查询
     * 例如: sp|B6J853|MIAB_COXB1
     */
    String identifier;

    Boolean reviewed;

    String organism;

    String gene;

    Boolean estimateCount = true;
}
