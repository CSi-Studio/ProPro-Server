package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.domain.BaseDO;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "protein")
public class ProteinDO extends BaseDO {

    @Indexed
    String id;

    /**
     * 蛋白质标识符,第一位是来源数据库,第二位是Access号,第三位是名称
     * 如果需要通过UniProt进行查询,可以使用第二位Access进行查询
     * 例如: sp|B6J853|MIAB_COXB1
     */
    @Indexed(unique = true)
    String identifier;

    @Indexed
    Boolean reviewed;

    /**
     * 创建标记,默认为录入时的fasta文件名+当日的时间戳
     */
    @Indexed
    String createTag;

    /**
     * 蛋白质名称
     * 例如: tRNA-2-methylthio-N(6)-dimethylallyladenosine synthase
     */
    List<String> names;

    @Indexed
    String organism;

    @Indexed
    String gene;

    String sequence;

    public String getUniProtLink() {
        if (identifier != null) {
            String[] identifiers = identifier.split(SymbolConst.BAR, -1);
            if (identifiers.length == 3) {
                return "https://www.uniprot.org/uniprot/" + identifiers[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getAlphaFoldLink() {
        if (identifier != null) {
            String[] identifiers = identifier.split(SymbolConst.BAR, -1);
            if (identifiers.length == 3) {
                return "https://www.alphafold.ebi.ac.uk/entry/" + identifiers[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
