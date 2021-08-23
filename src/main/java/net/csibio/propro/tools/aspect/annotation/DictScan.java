package net.csibio.propro.tools.aspect.annotation;

import lombok.Data;
import net.csibio.propro.config.SectionRegister;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

//@Retention(RetentionPolicy.RUNTIME)
//@Documented
//@Target(ElementType.TYPE)
@Data
@Import({SectionRegister.class})
public class DictScan {
    String[] basePackages;
}
