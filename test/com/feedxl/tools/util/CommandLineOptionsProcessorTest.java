package com.feedxl.tools.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandLineOptionsProcessorTest {

    @Test
    public void processesValidArgumentPairs() {
        String[] arguments = {"--arg1", "arg1value", "--arg2", "arg2value"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
    }

    @Test
    public void flagsWhenArgumentsAreNotInPair() {
        String[] arguments = {"--arg1", "arg1value", "--arg2WithNoValue"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertFalse("Arguments not in pairs should be invalid", sut.processInput());
    }

    @Test
    public void flagsIfArgumentsNoPrefixedWithDoubleHyphens() {
        String[] arguments = {"arg1", "arg1value"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertFalse("Argument name should have -- prefix", sut.processInput());
    }

    @Test
    public void canGetArgumentValueByName() {
        String[] arguments = {"--arg1", "arg1value"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        assertEquals("Argument value could not be fetched", "arg1value", sut.getString("arg1"));
    }

    @Test
    public void canGetBooleanValues() {
        String[] arguments = {"--arg1", "false"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        assertEquals("Argument boolean value could not be fetched", false, sut.getBoolean("arg1"));
    }

    @Test
    public void canGetIntValues() {
        String[] arguments = {"--arg1", "5"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        assertEquals("Argument int value could not be fetched", 5, sut.getInt("arg1"));
    }

    @Test
    public void canSetDefaultValues() {
        String[] arguments = {"--arg1", "5"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        sut.populateDefaultsIfMissing("arg2", "4");
        assertEquals("Default value not set", 4, sut.getInt("arg2"));
    }

    @Test
    public void defaultsDontOverrideValues() {
        String[] arguments = {"--arg1", "5"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        sut.populateDefaultsIfMissing("arg1", "4");
        assertEquals("Default value should not override if one exists from input", 5, sut.getInt("arg1"));
    }

    @Test
    public void canValidateIfParametersPresent() {
        String[] arguments = {"--arg1", "5"};
        CommandLineOptionsProcessor sut = new CommandLineOptionsProcessor(arguments);
        assertTrue("Given argument pairs should be valid inputs", sut.processInput());
        assertTrue("Mandatory field specified flagged even if present", sut.validateMandatoryFields("arg1"));
        assertFalse("Mandatory field specified not flagged", sut.validateMandatoryFields("arg2"));
    }
}
