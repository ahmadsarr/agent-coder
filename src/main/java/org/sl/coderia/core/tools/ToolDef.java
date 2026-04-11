package org.sl.coderia.core.tools;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * Describes a tool implementation by pairing its target instance with the
 * reflective method used to invoke it.
 */
@Data
public class ToolDef {
    private final Object instance;
    private final Method method;

}
