package net.csibio.propro.constants.enums;

import net.csibio.propro.annotation.Section;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 09:42
 */
@Section(name = "TaskTemplate", key = "Name", value = "TemplateName", Version = "1")
public enum TaskTemplate {

    /**
     * default template
     */
    DEFAULT("DEFAULT"),

    /**
     * upload runs
     */
    SCAN_AND_UPDATE_RUNS("SCAN_AND_UPDATE_RUNS"),

    /**
     * upload library file(including standard library and irt library)
     */
    UPLOAD_LIBRARY_FILE("UPLOAD_LIBRARY_FILE"),

    /**
     * extract for mzxml with standard library
     */
    EXTRACTOR("EXTRACTOR"),

    IRT_EXTRACTOR("IRT_EXTRACTOR"),

    RESELECT("RESELECT"),

    EXTRACT_PEAKPICK_SCORE("EXTRACT_PEAKPICK_SCORE"),

    IRT_EXTRACT_PEAKPICK_SCORE("IRT_EXTRACT_PEAKPICK_SCORE"),

    /**
     * compute irt for slope and intercept
     */
    IRT("IRT"),

    /**
     * compute sub scores
     */
    SCORE("SCORE"),

    /**
     * the whole workflow for swath including(irt -> extractor -> sub scores -> final scoreForAll)
     */
    SWATH_WORKFLOW("SWATH_WORKFLOW"),

    /**
     * compress the mzxml and sort the mzxml scan for swath
     */
    COMPRESSOR_AND_SORT("COMPRESSOR_AND_SORT"),

    ;

    String name;

    TaskTemplate(String templateName) {
        this.name = templateName;
    }

    public static TaskTemplate getByName(String name) {
        for (TaskTemplate template : values()) {
            if (template.getName().equals(name)) {
                return template;
            }
        }
        return null;
    }


    public String getName() {
        return name;
    }

    public String getTemplateName() {
        return name;
    }
}
