package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ToolExecutor {
    private final ToolSandbox sandbox;
    private final ToolRegistry registry;

    public ToolExecutor(ToolSandbox sandbox, ToolRegistry registry) {
        this.sandbox = sandbox;
        this.registry = registry;
    }

    public ToolExecutionResultMessage execute(ToolExecutionRequest req) {
        ToolDef fn = registry.get(req.name());
        if (fn == null) {
            return error(req.name(), "Unknown tool: " + req.name());
        }
        try {
            List<String> args = extractArgs(req.arguments(), fn.getMethod());
            Object result = this.sandbox.run(fn.getInstance(), fn.getMethod(), args.toArray(String[]::new));
            return ok(req.name(), result);
        } catch (Exception e) {
            e.printStackTrace();
            return error(req.name(), e.getMessage());
        }
    }

    public List<ToolSpecification> getSpec() {
        return this.registry.getToolMethods().stream()
                .map(ToolSpecifications::toolSpecificationFrom)
                .toList();
    }


    private List<String> extractArgs(String json, Method method) {
        List<String> args = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        JSONObject obj = parseJson(json);
        for (int i = 0; i < method.getParameterCount(); i++) {
            String paramName = parameters[i].getName(); // vrai nom si -parameters activé
            String raw = (String)obj.get(paramName);
            args.add(raw);
        }
        return args;
    }
    private JSONObject parseJson(String json) {
        try {
            return (JSONObject) new JSONParser().parse(json);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JSON from AI: " + json);
        }
    }

    public static ToolExecutionResultMessage error(String toolName, String reason) {
        return ToolExecutionResultMessage.builder()
                .id(toolName)
                .toolName(toolName)
                .isError(true)
                .text(reason)
                .build();
    }

    public static ToolExecutionResultMessage ok(String name, Object text) {
        return ToolExecutionResultMessage.builder()
                .id(name)
                .toolName(name)
                .isError(false)
                .text(String.valueOf(text))
                .build();
    }

    String arg(String json, String key) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(json);
            return (String) obj.get(key);
        } catch (ParseException e) {
            throw new RuntimeException("AI Response " + json + " < is not a valid JSON object");
        }
    }
}
