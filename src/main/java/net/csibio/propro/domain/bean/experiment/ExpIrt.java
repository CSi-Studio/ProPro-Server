package net.csibio.propro.domain.bean.experiment;

import lombok.Data;
import net.csibio.propro.domain.bean.irt.IrtResult;

@Data
public class ExpIrt {

    /**
     * 实验的ID
     */
    String id;

    /**
     * 实验名称
     */
    String name;

    /**
     * 实验的irt结果
     */
    IrtResult irt;

}
