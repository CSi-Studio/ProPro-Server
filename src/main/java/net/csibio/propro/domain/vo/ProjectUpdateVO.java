package net.csibio.propro.domain.vo;

import lombok.Data;

import java.util.Set;

@Data
public class ProjectUpdateVO {

    String id;
    String name;
    String type;
    String alias;
    String owner;
    String description;
    String anaLibId;
    String insLibId;
    String methodId;
    Set<String> tags;
}
