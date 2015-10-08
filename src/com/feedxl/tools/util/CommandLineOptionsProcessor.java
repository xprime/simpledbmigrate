package com.feedxl.tools.util;

import java.util.HashMap;
import java.util.Map;

public class CommandLineOptionsProcessor {
    private String[] args;
    private Map<String, String> argsMap;

    public CommandLineOptionsProcessor(String[] args) {
        this.args = args;
    }

    public boolean processInput() {
    	argsMap = new HashMap();
    	if (args.length == 0)
    		return true;
        if (args.length % 2 != 0) {
            printError("Invalid number of arguments");
            return false;
        }
        int i = 0;
        do {
            String key = args[i];
            if (key.length() <= 1) {
                printError("Invalid argument name "+key);
                return false;
            }
            key = key.substring(2);
            String value = args[i + 1];
            argsMap.put(key, value);
            i += 2;
        } while (i < args.length);
        return true;
    }

    private void printError(String error) {
        System.out.println(error);
    }

    public boolean validateMandatoryFields(String... fields) {
        for (String field : fields) {
            String value = argsMap.get(field);
            if (value == null || value.equals("")) {
                printError("Required field missing - "+field);
                return false;
            }
        }
        return true;
    }

    public void populateDefaultsIfMissing(String ... values) {
        if (values.length % 2 != 0) {
            throw new RuntimeException("Odd number of values to populate defaults");
        }
        int i=0;
        do {
            if (argsMap.containsKey(values[i])) {
                i += 2;
                continue;
            }
            argsMap.put(values[i], values[i+1]);
            i += 2;
        } while (i < values.length);
    }

    public String getString(String key) {
        return argsMap.get(key);
    }

    public boolean hasParam(String key) {
        return argsMap.containsKey(key);
    }

    public int getInt(String key) {
        String value = getString(key);
        return Integer.parseInt(value);
    }

    public boolean validateFieldsTogether(String key1, String key2) {
        return (hasParam(key1) && hasParam(key2)) || (!hasParam(key1) && !hasParam(key2));
    }

    public boolean getBoolean(String key) {
        String value = getString(key);
        return Boolean.parseBoolean(value);
    }
}