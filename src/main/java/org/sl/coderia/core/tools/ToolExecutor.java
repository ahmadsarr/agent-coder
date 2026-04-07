package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


@NoArgsConstructor
public class ToolExecutor {
    Map<String, Function<String, ToolExecutionResultMessage>> registry = new java.util.concurrent.ConcurrentHashMap<>();

    public ToolExecutionResultMessage execute(ToolExecutionRequest req) {
        Function<String, ToolExecutionResultMessage> fn = registry.get(req.name());
        if (fn == null)
            return error(req.name(), "Unknown tool: " + req.name());
        try {
            return fn.apply(req.arguments());
        } catch (Exception e) {
            return error(req.name(), e.getMessage());
        }
    }

    public ToolExecutor register(String name, Function<String, ToolExecutionResultMessage> callable) {
        registry.put(name, callable);
        return this;
    }

    public static ToolExecutor withTools(Tools tools) {
        ToolExecutor ex = new ToolExecutor();
        return ex
                .register("readFile",    args ->
                        ok("readFile",    tools.readFile(ex.arg(args, "arg0"))))

                .register("writeFile",   args ->
                        ok("writeFile",   tools.writeFile(
                                ex.arg(args, "path"),
                                ex.arg(args, "content"))))

                .register("execCommand", args ->
                        ok("execCommand", tools.execCommand(ex.arg(args, "arg0"))))

                .register("memory",      args ->
                        ok("memory",      tools.memory(
                                ex.arg(args, "arg0"),
                                ex.arg(args, "arg1"))))

                .register("print",       args ->
                        ok("print",       tools.print(ex.arg(args, "arg0"))));
    }

    public static ToolExecutionResultMessage error(String toolName, String reason) {
        return ToolExecutionResultMessage.builder()
                .id(toolName)
                .toolName(toolName)
                .isError(true)
                .text(reason)
                .build();
    }
    public static ToolExecutionResultMessage ok(String name, String text) {
        return ToolExecutionResultMessage.builder()
                .id(name)
                .toolName(name).isError(false).text(text).build();
    }


     String arg(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        return m.find() ? m.group(1).replace("\\n", "\n").replace("\\\"", "\"") : "";
    }


}
