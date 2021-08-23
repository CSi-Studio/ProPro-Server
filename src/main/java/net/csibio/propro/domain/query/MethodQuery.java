package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MethodQuery extends PageQuery {

    String id;

    String name;
}
