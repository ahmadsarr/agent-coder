package org.sl.coderia.core.utils.args;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ArgParser {
    private record Argument(
            String fieldName,
            String argName,
            String description,
            String defaultValue,
            Type type
    ) {
    }

    ;
    private final String[] args;

    public ArgParser(String[] args) {
        this.args = args;
    }

    public <T extends Helper> T parse(Class<T> type) throws Exception {

        Constructor<T> constructor = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> Modifier.isPublic(c.getModifiers()) && c.getParameterCount() == 0)
                .findFirst()
                .map(c -> (Constructor<T>) c)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No public constructor with No-arg was found for " + type.getName()));

        Map<String, Argument> argumentByFieldName = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Arg.class))
                .map(f -> {
                    Arg annotation = f.getAnnotation(Arg.class);
                    return new Argument(f.getName(), annotation.name(), annotation.description(),annotation.defaultValue(), f.getType());
                })
                .collect(Collectors.toMap(
                        Argument::fieldName,
                        Function.identity(),
                        (a1, a2) -> a2
                ));

        T instance = constructor.newInstance();

        for (Map.Entry<String, Argument> entry : argumentByFieldName.entrySet()) {
            try {


            String fieldName = entry.getKey();
            Argument arg = entry.getValue();
            String setterName = "set"
                    + fieldName.substring(0, 1).toUpperCase()
                    + fieldName.substring(1);

            Field field = type.getDeclaredField(fieldName);
            Object value = getArg(arg.argName(), null);
            if(value == null)
                value = convert(entry.getValue().type(),entry.getValue().defaultValue());
            if(value == null)
                continue;
            type.getMethod(setterName, field.getType()).invoke(instance, value);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + "");
            }
        }

        return instance;
    }

    private Object convert(Type type, String value) {
        if(value == null || value.isEmpty())
            return null;
        if (type.equals(String.class)) {
            return value;
        } else if (type.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (type.equals(long.class)) {
            return Long.parseLong(value);
        } else if (type.equals(double.class)) {
            return Double.parseDouble(value);
        } else if (type.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    private String getArg(String key, String defaultValue) {
        for (int i = 0; i < this.args.length - 1; i++) {
            if (args[i].startsWith("=")) {
                String[] kv = args[i].split("=");
                if (kv[0].equals(key)) {
                    return kv[1];
                }
            }
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    public String getStringValue(String key, String defaultValue) {
        return getArg(key, defaultValue);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getArg(key, Boolean.toString(defaultValue)));
    }

    public int getIntValue(String key, int defaultValue) {
        return Integer.parseInt(getArg(key, Integer.toString(defaultValue)));
    }

    public long getLongValue(String key, long defaultValue) {
        return Long.parseLong(getArg(key, Long.toString(defaultValue)));
    }

    public double getDoubleValue(String key, double defaultValue) {
        return Double.parseDouble(getArg(key, Double.toString(defaultValue)));
    }

}
