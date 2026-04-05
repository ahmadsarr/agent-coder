package org.sl.coderia.core.sandbox;

import org.sl.coderia.core.TerminalIO;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.*;

public class ToolSandbox {
    public enum RiskLevel { SAFE, RISKY, BLOCKED }

    public enum Permission { READ_ONLY, FILE_WRITE, SHELL_EXEC, NETWORK }


    private final Set<Permission> grantedPermissions;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ToolSandbox(Set<Permission> granted) {
        this.grantedPermissions = granted;
    }

    public <T> T run(Method toolMethod, Callable<T> action, Object... args) throws Exception {
        ToolPolicy policy = toolMethod.getAnnotation(ToolPolicy.class);
        String     name   = toolMethod.getName();

        for (Permission p : policy.requires()) {
            if (!grantedPermissions.contains(p))
                throw new SecurityException("Tool '" + name + "' requires " + p + " — not granted");
        }

        if (policy.risk() == RiskLevel.BLOCKED)
            throw new SecurityException("Tool '" + name + "' is unconditionally blocked");

        if (policy.risk() == RiskLevel.RISKY && !TerminalIO.getInstance().requestApproval(name,args))
            throw new SecurityException("User denied execution of '" + name + "'");

        Future<T> future = executor.submit(action);

        T result;
        try {
            result = future.get(policy.timeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Tool '" + name + "' timed out after " + policy.timeoutMs() + "ms");
        }

        result = sanitise(result, 8_000);


        return result;
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
