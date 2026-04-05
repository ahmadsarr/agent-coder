package org.sl.coderia.core.utils;

public class ArgParser {
    private final String[] args;

    public ArgParser(String[] args) {
        this.args = args;
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
