package net.csibio.propro.domain.bean.run;

import lombok.Data;
import net.csibio.propro.domain.bean.irt.IrtResult;

@Data
public class RunIrt {

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
