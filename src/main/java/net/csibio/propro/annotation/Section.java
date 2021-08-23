package net.csibio.propro.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface Section {
    String name();
    String key() default "" ;
    String value() default "";
    String Version() ;
}
