package net.csibio.propro.domain.bean.method;

import lombok.Data;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.options.*;
import org.springframework.beans.BeanUtils;

@Data
public class Method {

    String id;

    String name;

    String description;

    EicOptions eic;

    IrtOptions irt;

    PeakFindingOptions peakFinding;

    ScoreOptions score;

    ClassifierOptions classifier;

    public MethodDO toMethodDO() {
        MethodDO method = new MethodDO();
        BeanUtils.copyProperties(this, method);
        return method;
    }
}
