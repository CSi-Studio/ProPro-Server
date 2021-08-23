package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;

@Data
@Accessors(chain = true)
public class DictQuery extends PageQuery {

    String id;

    String Name;
    String Version;

    public DictQuery() {
    }

    public DictQuery(String id) {
        this.id = id;
    }


    public DictQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn) {
        super(pageNo, pageSize, direction, sortColumn);
    }

}