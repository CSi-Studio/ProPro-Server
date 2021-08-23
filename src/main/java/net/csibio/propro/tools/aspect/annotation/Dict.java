package net.csibio.propro.tools.aspect.annotation;

import net.csibio.propro.domain.vo.DictItem;

import java.lang.annotation.*;
import java.util.List;
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dict {
    String value() default "";

}
