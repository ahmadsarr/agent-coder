package org.sl.coderia.core.tools;


import java.lang.reflect.Method;
import java.util.Set;

public class ToolSandbox {
    public enum RiskLevel { SAFE, RISKY, BLOCKED }

    public enum Permission { READ_ONLY, FILE_WRITE, SHELL_EXEC, NETWORK }


    private final Set<Permission> grantedPermissions;

    public ToolSandbox(Set<Permission> granted) {
        this.grantedPermissions = granted;
    }

    public Object run(Object instance,Method toolMethod,String... args) throws Exception {
        ToolPolicy policy = toolMethod.getAnnotation(ToolPolicy.class);
        String     name   = toolMethod.getName();

        for (Permission p : policy.requires()) {
            if (!grantedPermissions.contains(p))
                throw new SecurityException("Tool '" + name + "' requires " + p + " — not granted");
        }

        if (policy.risk() == RiskLevel.BLOCKED)
            throw new SecurityException("Tool '" + name + "' is unconditionally blocked");

        if (policy.risk() == RiskLevel.RISKY)
            throw new SecurityException("User denied execution of '" + name + "'");

       return invoke(instance,toolMethod,args);
    }
    private Object invoke(Object instance, Method method, String... args) throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] converted = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            converted[i] = coerce(args[i], paramTypes[i]);
        }
        return method.invoke(instance, converted);
    }

    private Object coerce(String value, Class<?> target) {
        if (value == null) return null;
        if (target.isInstance(value)) return value;
        String s = value.toString();
        if (target == int.class || target == Integer.class)    return Integer.parseInt(s);
        if (target == long.class || target == Long.class)      return Long.parseLong(s);
        if (target == double.class || target == Double.class)  return Double.parseDouble(s);
        if (target == boolean.class || target == Boolean.class) return Boolean.parseBoolean(s);
        return s; // fallback String
    }




    @SuppressWarnings("unchecked")
    private <T> T sanitise(T result, int maxChars) {
        if (result instanceof String s) {
            String out = s.length() > maxChars
                    ? s.substring(0, maxChars) + "\n…[truncated]"
                    : s;
            out = out.replaceAll("(?i)(api[_-]?key|token|password|secret)[^\\n]{0,80}", "[REDACTED]");
            return (T) out;
        }
        return result;
    }

}
