package com.feedxl.tools.util;

import java.util.HashMap;
import java.util.Map;

public class CommandLineOptionsProcessor {
    private String[] arguments;
    private Map<String, String> mappedArguments;

    public CommandLineOptionsProcessor(String[] arguments) {
        this.arguments = arguments;
    }

    public boolean processInput() {
        mappedArguments = new HashMap();
        if (!hasArguments())
            return true;
        if (!hasArgumentsInPair()) {
            printError("Invalid number of arguments");
            return false;
        }
        int i = 0;
        do {
            String rawName = arguments[i];
            if (!hasDoubleHyphenPrefix(rawName)) {
                printError("Invalid argument name : " + rawName);
                return false;
            }
            String name = removeDoubleHyphenPrefix(rawName);
            if (name.equals("")) {
                printError("Empty argument name after -- : " + rawName);
                return false;
            }
            String value = arguments[i + 1];
            mappedArguments.put(name, value);
            i += 2;
        } while (i < arguments.length);
        return true;
    }

    private String getName(String arg) {
        String name = arg;
        if (!hasDoubleHyphenPrefix(name)) {
            printError("Invalid argument name : " + name);
            return "";
        }
        return removeDoubleHyphenPrefix(name);
    }

    private String removeDoubleHyphenPrefix(String name) {
        return name.substring(2);
    }

    private boolean hasDoubleHyphenPrefix(String name) {
        return name.length() >= 2 && name.startsWith("--");
    }

    private boolean hasArgumentsInPair() {
        return arguments.length % 2 == 0;
    }

    private boolean hasArguments() {
        return arguments.length > 0;
    }

    private void printError(String error) {
        System.out.println(error);
    }

    public boolean validateMandatoryFields(String... fields) {
        for (String field : fields) {
            String value = mappedArguments.get(field);
            if (value == null || value.equals("")) {
                printError("Required field missing - " + field);
                return false;
            }
        }
        return true;
    }

    public void populateDefaultsIfMissing(String... values) {
        if (values.length % 2 != 0) {
            throw new RuntimeException("Odd number of values to populate defaults");
        }
        int i = 0;
        do {
            if (mappedArguments.containsKey(values[i])) {
                i += 2;
                continue;
            }
            mappedArguments.put(values[i], values[i + 1]);
            i += 2;
        } while (i < values.length);
    }

    public String getString(String name) {
        return mappedArguments.get(name);
    }

    public boolean hasParam(String name) {
        return mappedArguments.containsKey(name);
    }

    public int getInt(String key) {
        String value = getString(key);
        return Integer.parseInt(value);
    }

    public boolean validateFieldsTogether(String key1, String key2) {
        return (hasParam(key1) && hasParam(key2)) || (!hasParam(key1) && !hasParam(key2));
    }

    public boolean getBoolean(String name) {
        String value = getString(name);
        return Boolean.parseBoolean(value);
    }
}