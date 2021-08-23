package net.csibio.propro.annotation;

import net.csibio.propro.config.SectionRegister;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Import({SectionRegister.class})
public @interface SectionScan {
    String[] basePackages() default {};
}
