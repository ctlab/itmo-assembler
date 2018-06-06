package ru.ifmo.genetics.tools;

import org.apache.commons.cli.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.IOException;

public class GetProperty {
    public static void main(String[] args) throws IOException, InterruptedException {
        Options options = new Options();

        options.addOption("h", "help", false, "prints this message");
        options.addOption("c", "config", true,  "sets the config file name, default to config.properties");
        options.addOption("p", "property", true,  "sets the name of a property to lookup");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace(System.err);
            return;
        }

        if (cmd.hasOption("help")) {
            new HelpFormatter().printHelp("get_property", options);
            return;
        }

        String configFileName = cmd.getOptionValue("config", "config.properties");
        Configuration config;
        try {
            config = new PropertiesConfiguration(configFileName);
        } catch (ConfigurationException e) {
            e.printStackTrace(System.err);
            return;
        }


        System.out.println(config.getString(cmd.getOptionValue("property")));
    }
}
