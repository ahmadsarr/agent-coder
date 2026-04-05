package org.sl.coderia.core.sandbox;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ToolPolicy {
    ToolSandbox.Permission[] requires() default {};

    ToolSandbox.RiskLevel risk() default ToolSandbox.RiskLevel.SAFE;

    long timeoutMs() default 5_000;

}