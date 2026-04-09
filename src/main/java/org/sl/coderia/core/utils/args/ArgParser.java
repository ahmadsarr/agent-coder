package org.sl.coderia.core.utils.args;

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
            Object defaultValue,
            Type type
    ) {}

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
                        "No public no-arg constructor found for " + type.getName()));

        T instance = constructor.newInstance();
        if (Arrays.asList(args).contains("--help")) {
            System.out.println(instance.help());
            System.exit(0);
        }

        Map<String, Argument> argumentByFieldName = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Arg.class))
                .map(f -> {
                    Arg annotation = f.getAnnotation(Arg.class);
                    return new Argument(f.getName(), annotation.name(), annotation.description(),
                            annotation.defaultValue(), f.getType());
                })
                .collect(Collectors.toMap(
                        Argument::fieldName,
                        Function.identity(),
                        (a1, a2) -> a2
                ));

        for (Map.Entry<String, Argument> entry : argumentByFieldName.entrySet()) {
            String fieldName = entry.getKey();
            Argument arg = entry.getValue();
            try {
                String setterName = "set"
                        + fieldName.substring(0, 1).toUpperCase()
                        + fieldName.substring(1);



                Field field = type.getDeclaredField(fieldName);
                Object value = getArg(arg.argName(), null);


                if (value == null)
                    value = arg.defaultValue();
                if (value == null)
                    continue;

                type.getMethod(setterName, field.getType()).invoke(instance, convert(arg.type,value));
            } catch (Exception e) {
                System.err.println("Error parsing argument '" + arg.argName() + "': " + e.getMessage());
            }
        }

        return instance;
    }

    private Object convert(Type type, Object value) {
        if (value == null ) return null;
        if (type.equals(String.class))   return value;
        if (type.equals(int.class))      return Integer.parseInt(value.toString());
        if (type.equals(long.class))     return Long.parseLong(value.toString());
        if (type.equals(double.class))   return Double.parseDouble(value.toString());
        if (type.equals(boolean.class))  return Boolean.parseBoolean(value.toString());
        return null;
    }

    private String getArg(String key, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("=")) {
                String[] kv = args[i].split("=", 2);
                if (kv[0].equals(key)) return kv[1];
                continue;
            }
            // Support --key value
            if (args[i].equals(key) && i + 1 < args.length) {
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
