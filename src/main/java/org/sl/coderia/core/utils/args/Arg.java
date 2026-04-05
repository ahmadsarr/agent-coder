package org.sl.coderia.core.utils.args;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention( java.lang.annotation.RetentionPolicy.RUNTIME)
@Target( java.lang.annotation.ElementType.FIELD)
public @interface Arg {
    String  name();
    String description();
    String defaultValue() default "";
}
