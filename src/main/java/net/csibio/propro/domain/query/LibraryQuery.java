package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-04 21:16
 */
@Data
@Accessors(chain = true)
public class LibraryQuery extends PageQuery {

    private static final long serialVersionUID = -3258829839160856625L;

    String id;

    String name;

    /**
     * @see net.csibio.propro.constants.enums.LibraryType
     */
    String type;

    Date createDate;

    Date lastModifiedDate;

    public LibraryQuery() {
    }

    public LibraryQuery(String id) {
        this.id = id;
    }

    public LibraryQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn) {
        super(pageNo, pageSize, direction, sortColumn);
    }
}
