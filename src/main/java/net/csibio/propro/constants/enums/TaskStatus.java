package net.csibio.propro.constants.enums;

import net.csibio.propro.annotation.Section;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
@Section(name = "TaskStatus",key ="Name",value = "Desc",Version = "1")
public enum TaskStatus {

    UNKNOWN("UNKNOWN"),

    WAITING("WAITING"),

    RUNNING("RUNNING"),

    SUCCESS("SUCCESS"),

    FAILED("FAILED"),


    ;

    String name;
    String desc;


    TaskStatus(String name) {
        this.name = name;
        this.desc = name;
    }

    public static TaskStatus getByName(String name) {
        for (TaskStatus status : values()) {
            if (status.getName().equals(name)) {
                return status;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
    public String getDesc() {
        return name;
    }
}
