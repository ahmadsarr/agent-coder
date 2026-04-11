package org.sl.coderia.core.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class ToolRegistry {
    private final Map<String, ToolDef> registry = new ConcurrentHashMap<>();

    public ToolRegistry register(String name, ToolDef callable) {
        registry.put(name, callable);
        return this;
    }
    public ToolDef get(String name) {
        return registry.get(name);
    }

    public List<Method> getToolMethods(){
        return registry.values().stream().map(ToolDef::getMethod).toList();
    }

    public static ToolRegistry withTools(Object... tools) {
        ToolRegistry instance = new ToolRegistry();
        for (Object tool : tools) {
            Arrays.stream(tool.getClass().getMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .filter(method -> method.isAnnotationPresent(Tool.class))
                    .forEach(method -> {
                        String name = method.getName();
                        instance.register(name, new ToolDef(tool, method));
                    });
        }

        return instance;
    }


}
