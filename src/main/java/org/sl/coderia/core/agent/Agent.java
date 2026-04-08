package org.sl.coderia.core.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.sl.coderia.core.tools.ToolExecutor;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.tools.Tools;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;
import org.sl.coderia.core.utils.args.CommandLineArgs;
import org.sl.coderia.core.utils.args.ArgParser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;



public class Agent {



    public void run(String[] args) throws Exception {
        TerminalIO terminal = TerminalIO.getInstance();
        ArgParser argParser = new ArgParser(args);
        CommandLineArgs commandLineArgs = argParser.parse(CommandLineArgs.class);
        System.out.println(commandLineArgs);

        Set<ToolSandbox.Permission> permissions = Arrays.stream(ToolSandbox.Permission.values()).collect(Collectors.toSet());
        Tools tools = new Tools(new ToolSandbox(permissions));
        AgentLoop agentLoop = AgentLoop.builder()
                .model(buildModel(commandLineArgs))
                .tools(ToolExecutor.withTools(tools))
                .specs(toolSpecificationsFrom(tools))
                .terminal(TerminalIO.getInstance())
                .build();
        while (true) {
            String question = terminal.read("You: ");
            if (question == null || question.isBlank() || question.equalsIgnoreCase("exit")) {
                break;
            }
            agentLoop.run(question);
        }
    }

    public static List<ToolSpecification> toolSpecificationsFrom(Object objectWithTools) {
        return Arrays.stream(objectWithTools.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(Agent::toToolSpecification)
                .toList();
    }

    private static ToolSpecification toToolSpecification(Method method) {
        Tool tool = method.getAnnotation(Tool.class);

        String name = tool.name().isBlank() ? method.getName() : tool.name();
        String description = String.join("\n", tool.value());

        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(name)
                .description(description);

        JsonObjectSchema parametersSchema = parametersSchemaFrom(method);
        if (parametersSchema != null) {
            builder.parameters(parametersSchema);
        }

        return builder.build();
    }

    private static JsonObjectSchema parametersSchemaFrom(Method method) {
        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0) {
            return null;
        }

        JsonObjectSchema.Builder schema = JsonObjectSchema.builder();

        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            Class<?> type = parameter.getType();

            if (type == String.class) {
                schema.addStringProperty(paramName);
            } else if (type == int.class || type == Integer.class
                    || type == long.class || type == Long.class
                    || type == short.class || type == Short.class) {
                schema.addIntegerProperty(paramName);
            } else if (type == double.class || type == Double.class
                    || type == float.class || type == Float.class) {
                schema.addNumberProperty(paramName);
            } else if (type == boolean.class || type == Boolean.class) {
                schema.addBooleanProperty(paramName);
            } else if (type.isEnum()) {
                List<String> values = Arrays.stream(type.getEnumConstants())
                        .map(Object::toString)
                        .toList();
                schema.addEnumProperty(paramName, values);
            } else {
                schema.addStringProperty(paramName);
            }

            schema.required(paramName);
        }

        return schema.build();
    }
    private static ChatModel buildModel(CommandLineArgs commandLineArgs) {
        return ChatModelFactory.openAiChatModelBuilder()
                .baseUrl(commandLineArgs.getBaseUrl())
                .apiKey(commandLineArgs.getApiKey())
                .modelName(commandLineArgs.getModelName())
                .httpClientBuilder(Utils.createHttpClientBuilder(commandLineArgs.getTimeout()))
                .build();
    }
}
