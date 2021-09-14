package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProjectQuery extends PageQuery {

    String id;

    String group;
    
    String name;

    String alias;

    String type;

    String owner;
}
