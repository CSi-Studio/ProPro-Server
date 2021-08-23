package net.csibio.propro.constants.enums;

import net.csibio.propro.annotation.Section;

import java.io.Serializable;
@Section(name = "constants",key="Name",value="Desc",Version = "3")
public enum LibraryType implements Serializable {

    INS("INS", "Internal Standard"),
    ANA("ANA", "Analytes to analyze"),

    ;

    private String desc;
    private String name;

    LibraryType(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
