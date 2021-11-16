package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class RunQuery extends PageQuery {

    String id;

    List<String> ids;

    String name;

    String label;

    String projectName;

    String projectId;

    String type;
}
