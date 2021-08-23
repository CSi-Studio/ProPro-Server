package net.csibio.propro.constants.enums;

import net.csibio.propro.annotation.Section;

import java.io.Serializable;
@Section(name = "ProjectType",key="Name",value = "Code",Version = "1")
public enum ProjectType implements Serializable {

    DIA("DIA", "DIA"),
    PRM("PRM", "PRM"),

    ;

    private String code;
    private String name;

    ProjectType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
