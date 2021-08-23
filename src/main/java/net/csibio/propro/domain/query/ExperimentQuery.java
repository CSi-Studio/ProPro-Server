package net.csibio.propro.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ExperimentQuery extends PageQuery {

    String id;

    List<String> ids;

    String name;

    String projectName;

    String projectId;

    String type;
}
