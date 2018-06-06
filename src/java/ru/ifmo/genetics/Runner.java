package ru.ifmo.genetics;

import ru.ifmo.genetics.tools.Assembler;
import ru.ifmo.genetics.utils.tool.Parameter;
import ru.ifmo.genetics.utils.tool.Tool;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.BoolParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static ru.ifmo.genetics.utils.TextUtils.fit;

public class Runner {

    protected final List<ToolInfo> tools = new ArrayList<ToolInfo>();
    {
        loadTools();
    }

    protected ToolInfo defaultTool = findTool(Assembler.class);

    protected PrintStream out = System.out;
    protected final List<String> printLater = new ArrayList<String>();


    // Launch options (part of them is processed in .sh or .bat script)
    public static final Parameter<String> memoryParameter = new Parameter<String>(new StringParameterBuilder("memory")
            .important()
            .withShortOpt("m")
            .withDescription("memory to use (for example: 1500M, 4G, etc.)")
            .withDefaultComment("2 Gb")
            .create());
    public static final Parameter<Boolean> eaParameter = new Parameter<Boolean>(new BoolParameterBuilder("enable-assertions")
            .withShortOpt("ea")
            .withDescription("enable assertions")
            .withDefaultComment("assertions disabled")
            .create());
    public static final Parameter<Boolean> guiParameter = new Parameter<Boolean>(new BoolParameterBuilder("gui")
            .withDescription("run GUI")
            .create());
    public static final Parameter<Boolean> toolsParameter = new Parameter<Boolean>(new BoolParameterBuilder("tools")
            .withShortOpt("ts")
            .withDescription("print available tools")
            .create());
    public static final Parameter<String> toolParameter = new Parameter<String>(new StringParameterBuilder("tool")
            .withShortOpt("t")
            .withDescription("set certain tool to run")
            .withDefaultComment(Assembler.NAME)
            .create());


    // methods for override
    protected void printHeader() {
        out.println("ITMO Genome Assembler, version " + getVersion());
        out.println();
    }
    protected void printFirstHelp() {
        out.println("Usage: itmo-assembler [<Launch options>] [<Input parameters>]");
        printLater.add("This package also allows you to run other tools from it.");
        printLater.add("You can run certain tool via option -t <tool-name>, add --help-all to see more information.");
    }

    protected boolean checkOptionsBeforeRunning(String[] args) {
        boolean printVersion = containsOption(args, "--version");
        boolean printFirstHelp = containsOption(args, getOptKeys(Tool.helpParameter))
                && !containsOption(args, getOptKeys(toolParameter));
        boolean runGUI = (args.length == 0) || containsOption(args, getOptKeys(guiParameter));

        if (printVersion) {
            printHeader();
            return true;
        }
        if (printFirstHelp) {
            printHeader();
            printFirstHelp();
            return false;
        }

        if (runGUI) {
            printHeader();
            args = removeSingleOption(args, getOptKeys(guiParameter));
            out.println("Starting GUI...");
            out.println("If you want to work via command line, add -h option to view help.");
            try {
                GUI.mainImpl(args);
            } catch (RuntimeException e) {
                out.println("Can't start GUI! Try to add -h option to work via command line.");
                out.println("Exception: " + e.getMessage());
                System.exit(1);
            }
            return true;
        }

        return false;
    }




    public static void main(String[] args) {
        new Runner().run(args);
    }


    public void run(String[] args) {
        boolean shouldReturn = checkOptionsBeforeRunning(args);
        if (shouldReturn) {
            return;
        }

        // processing runner options
        boolean printTools = containsOption(args, getOptKeys(toolsParameter));
        if (printTools) {
            printHeader();
            out.println("Available tools (name, description):");
            out.println();
            for (ToolInfo info : tools) {
                out.println(fit(info.name, 40) + " " + info.description);
            }
            out.println();
            out.println("To see help for certain tool run:    itmo-assembler.sh -t <tool-name>");
            System.exit(0);
        }

        // setting tool
        ToolInfo tool = defaultTool;

        boolean toolIsSet = containsOption(args, getOptKeys(toolParameter));
        if (toolIsSet) {
            String toolName = getOptionValue(args, getOptKeys(toolParameter));
            args = removeOptionWithValue(args, getOptKeys(toolParameter));
            tool = null;
            for (ToolInfo info : tools) {
                if (info.name.equals(toolName)) {
                    tool = info;
                }
            }
            if (tool == null) {
                out.println("ERROR: Tool '" + toolName + "' not found !");
                System.exit(1);
            }
        }
        if (tool == null) {
            out.println("ERROR: No tool to run is specified !");
            System.exit(1);
        }

        // loading tool
        Tool toolInst = null;
//        System.err.println("Loading class " + tool.className);
        try {
            ClassLoader classLoader = Runner.class.getClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            Class clazz = classLoader.loadClass(tool.className);
            toolInst = (Tool) clazz.newInstance();
        } catch (InstantiationException e) {
            out.println("ERROR: " + e);
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            out.println("ERROR: " + e);
            e.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException e) {
            out.println("ERROR: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        // running
        toolInst.mainImpl(args);

        // additional help info
        for (String s : printLater) {
            out.println(s);
        }
    }



    public static class ToolInfo {
        public String className, name, description;
    }


    protected void loadTools() {
        InputStream toolsFileStream = ClassLoader.getSystemResourceAsStream("TOOLS");
        BufferedReader reader = new BufferedReader(new InputStreamReader(toolsFileStream));
        try {
            while (reader.ready()) {
                StringTokenizer st = new StringTokenizer(reader.readLine(), " ");

                ToolInfo info = new ToolInfo();
                info.name = st.nextToken();
                info.className = st.nextToken();
                info.description = st.nextToken() + (st.hasMoreTokens() ? st.nextToken("") : "");
                tools.add(info);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error occurs while reading TOOLS file: " + e);
        }
//        System.err.println("Loaded " + tools.size() + " tools");
    }

    protected ToolInfo findTool(Class class1) {
        String className = class1.getName();
        for (ToolInfo info : tools) {
            if (info.className.equals(className)) {
                return info;
            }
        }
        return null;
    }


    // methods for getting additional info
    public static String getVersion() {
        InputStream versionFileStream = ClassLoader.getSystemResourceAsStream("VERSION");
        if (versionFileStream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(versionFileStream));
        String version;
        try {
            version = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("I/O error occurs while reading VERSION file: " + e);
        }
        return version;
    }
    
    public static String getVersionNumber() {
        String version = getVersion();
        int si = version.indexOf(' ');
        if (si == -1) {
            return version;
        }
        return version.substring(0, si);
    }


    public static String getProtocol(Class clazz) {
        return clazz.getResource(clazz.getSimpleName() + ".class").getProtocol();
    }
    public static String getSourceRootPath(Class clazz) {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        File f = new File(path);
        return f.toString();
    }
    public static String getJarFilePath(Class clazz) {
        if (!getProtocol(clazz).equals("jar")) {
            throw new RuntimeException("Not a jar file!");
        }
        return getSourceRootPath(clazz);
    }

    public static String getProtocol() {
        return getProtocol(Runner.class);
    }
    public static String getSourceRootPath() {
        return getSourceRootPath(Runner.class);
    }
    public static String getJarFilePath() {
        return getJarFilePath(Runner.class);
    }


    // simple methods for parse options (of type String[])
    public static boolean containsOption(String[] args, String... optionsToCheck) {
        for (String s : args) {
            for (String opt : optionsToCheck) {
                if (s.equals(opt)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] removeSingleOption(String[] args, String... optionsToCheck) {
        int c = 0;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            for (int j = 0; (j < optionsToCheck.length) && (s != null); j++) {
                String opt = optionsToCheck[j];
                if (s.equals(opt)) {
                    s = null;
                }
            }
            if (s == null) {    // found -> should remove
                args[i] = null;
            } else {
                c++;
            }
        }

        String[] ans = new String[c];
        c = 0;
        for (String s : args) {
            if (s != null) {
                ans[c] = s;
                c++;
            }
        }
        return ans;
    }

    public static String getOptionValue(String[] args, String... optionsToCheck) {
        for (int i = 0; i < args.length; i++) {
            for (String opt : optionsToCheck) {
                if (args[i].equals(opt)) {
                    if (i + 1 >= args.length) {
                        throw new RuntimeException("No value for option '" + opt + "'!");
                    }
                    return args[i + 1];
                }
            }
        }
        return null;
    }

    public static String[] removeOptionWithValue(String[] args, String... optionsToCheck) {
        for (int i = 0; i < args.length; i++) {
            for (String opt : optionsToCheck) {
                if (args[i].equals(opt)) {
                    String[] ans = new String[args.length - 2];
                    System.arraycopy(args, 0, ans, 0, i);
                    System.arraycopy(args, i + 2, ans, i, args.length - 2 - i);
                    return ans;
                }
            }
        }
        return null;
    }

    public static <T> String[] getOptKeys(Parameter<T> parameter) {
        return new String[]{"-" + parameter.description.shortOpt, "--" + parameter.description.name};
    }

}
