package net.csibio.propro.domain.db;

import lombok.Data;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.method.Method;
import net.csibio.propro.domain.options.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document("method")
public class MethodDO extends BaseDO {

    @Id
    String id;

    @Indexed(unique = true)
    String name;

    String description;

    EicOptions eic;

    IrtOptions irt;

    PeakFindingOptions peakFinding;

    QuickFilterOptions quickFilter;

    ScoreOptions score;

    ClassifierOptions classifier;

    Date createDate;

    Date lastModifiedDate;

    public MethodDO() {
    }

    public MethodDO init() {
        this.eic = new EicOptions();
        this.irt = new IrtOptions();
        this.peakFinding = new PeakFindingOptions();
        this.quickFilter = new QuickFilterOptions();
        this.score = new ScoreOptions();
        this.classifier = new ClassifierOptions();
        return this;
    }

    public Method toMethod() {
        Method method = new Method();
        BeanUtils.copyProperties(this, method);
        return method;
    }
}
