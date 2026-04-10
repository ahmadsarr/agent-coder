package org.sl.coderia.core.tools;

import ai.djl.util.JsonUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sl.coderia.core.utils.TerminalIO;

import java.util.Map;
import java.util.function.Function;


@NoArgsConstructor
public class ToolExecutor {
    Map<String, Function<String, ToolExecutionResultMessage>> registry = new java.util.concurrent.ConcurrentHashMap<>();

    public ToolExecutionResultMessage execute(ToolExecutionRequest req) {
        TerminalIO io = TerminalIO.getInstance();

        Function<String, ToolExecutionResultMessage> fn = registry.get(req.name());
        if (fn == null) {
            io.printLine("[TOOL-EXEC] reject unknownTool=" + req.name() + " available=" + registry.keySet());
            return error(req.name(), "Unknown tool: " + req.name());
        }
        try {
            ToolExecutionResultMessage result = fn.apply(req.arguments());
            return result;
        } catch (Exception e) {
            io.printLine("[TOOL-EXEC] reject tool=" + req.name() + " error=" + e.getMessage());
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
                .register("readFile", args ->
                        ok("readFile", tools.readFile(ex.arg(args, "arg0"))))
                .register("readFileRaw", args ->
                        ok("readFileRaw", tools.readFileRaw(ex.arg(args, "arg0"))))
                .register("editFile", args ->
                        ok("editFile", tools.editFile(
                                ex.arg(args, "arg0"),
                                ex.arg(args, "arg1"),
                                ex.arg(args, "arg2"))))

                .register("writeFile", args ->
                        ok("writeFile", tools.writeFile(
                                ex.arg(args, "arg0"),
                                ex.arg(args, "arg1"))))

                .register("execCommand", args ->
                        ok("execCommand", tools.execCommand(ex.arg(args, "arg0"))))

                .register("memory", args ->
                        ok("memory", tools.memory(
                                ex.arg(args, "arg0"),
                                ex.arg(args, "arg1"))))
                .register("print", args ->
                        ok("print", tools.print(ex.arg(args, "arg0"))))
                .register("askHuman", args ->
                        ok("askHuman", tools.askHuman(ex.arg(args, "arg0"))));
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

        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(json);
            return (String) obj.get(key);
        } catch (ParseException e) {
            throw new RuntimeException("AI Response" + json + "< is not a valid JSON object");
        }
    }

    private String preview(String value, int maxLen) {
        if (value == null) {
            return "<null>";
        }
        String compact = value.replace("\n", "\\n").replace("\r", "\\r").trim();
        if (compact.length() <= maxLen) {
            return compact;
        }
        return compact.substring(0, maxLen) + "...";
    }


}
