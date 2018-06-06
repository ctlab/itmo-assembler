package ru.ifmo.genetics.utils.tool;

import org.apache.commons.cli.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import ru.ifmo.genetics.Runner;
import ru.ifmo.genetics.utils.Misc;
import ru.ifmo.genetics.utils.TextUtils;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.BoolParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.IntParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.ParameterBuilder;
import ru.ifmo.genetics.utils.tool.inputParameterBuilder.StringParameterBuilder;
import ru.ifmo.genetics.utils.tool.parameters.OutputParameter;
import ru.ifmo.genetics.utils.tool.parameters.ParameterDescription;
import ru.ifmo.genetics.utils.tool.values.*;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static ru.ifmo.genetics.utils.TextUtils.multiply;
import static ru.ifmo.genetics.utils.TextUtils.objToStr;

public abstract class Tool {
    public static final String IN_PARAM_FILE = "in.properties";
    public static final String OUT_PARAM_FILE = "out.properties";
    public static final String SUCCESS_FILE = "SUCCESS";


    static final Logger mainLogger = Logger.getLogger("MAIN");
    protected final Logger logger;

    public final Progress progress = new Progress();

    @NotNull public final String name;
    @NotNull public final String description;

    public static boolean launchedFromGUI = false;
    public static volatile Throwable lastException;
    public static volatile boolean isInterrupted = false;


    protected Tool(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
        logger = Logger.getLogger(name);
    }


    // ========================== parameters =============================

    // global input parameters, they are set in mainImpl()
    public static final List<Parameter> launchOptions = new ArrayList<Parameter>();

    public static final Parameter<String> workDirParameter = new Parameter<String>(new StringParameterBuilder("work-dir")
            .important()
            .withShortOpt("w")
            .withDefaultValue("workDir")
            .withDescriptionShort("Working directory")
            .withDescription("working directory")
            .withDescriptionRuShort("Рабочая директория")
            .withDescriptionRu("Директория, где будут сохраняться временные файлы (получаемые в процессе работы), " +
                    "а также результаты промежуточных этапов")
            .create());

    public static final Parameter<Integer> availableProcessorsParameter = new Parameter<Integer>(new IntParameterBuilder("available-processors")
            .withShortOpt("p")
            .withDefaultValue(Runtime.getRuntime().availableProcessors())
            .withDefaultComment("all (" + Runtime.getRuntime().availableProcessors() + ")")
            .withDescriptionShort("Available processors")
            .withDescription("available processors")
            .withDescriptionRuShort("Число ядер процессора")
            .withDescriptionRu("Число используемых ядер процессора")
            .create());


    public static final Parameter<Boolean> continueParameter = new Parameter<Boolean>(new BoolParameterBuilder("continue")
            .important()
            .withShortOpt("c")
            .withDescription("continue the previous run from last succeed stage, saved in working directory")
            .create());

    public static final Parameter<Boolean> forceParameter = new Parameter<Boolean>(new BoolParameterBuilder("force")
            .important()
            .withDescription("force run with rewriting old results")
            .create());

    public static final Parameter<String> startParameter = new Parameter<String>(new StringParameterBuilder("start")
            .withShortOpt("s")
            .withDescription("first force run stage (with rewriting old results)")
            .create());
    public static final Parameter<String> finishParameter = new Parameter<String>(new StringParameterBuilder("finish")
            .withShortOpt("f")
            .withDescription("stop after running this stage")
            .create());


    public static final Parameter<Boolean> helpParameter = new Parameter<Boolean>(new BoolParameterBuilder("help")
            .important()
            .withShortOpt("h")
            .withDescription("print short help message")
            .create());
    public static final Parameter<Boolean> helpAllParameter = new Parameter<Boolean>(new BoolParameterBuilder("help-all")
            .withShortOpt("ha")
            .withDescription("print full help message")
            .create());

//    private final Parameter<String> config = new ...
//    options.addOption("c", "config", true,  "sets the config file");

    public static final Parameter<Boolean> verboseParameter = new Parameter<Boolean>(new BoolParameterBuilder("verbose")
            .withShortOpt("v")
            .withDefaultValue(false)
            .withDescriptionShort("Verbose output")
            .withDescription("enable debug output")
            .withDescriptionRuShort("Вывод дополнительной информации")
            .withDescriptionRu("Вывод информации для дебага и др.")
            .create());
    private static boolean verbose;


    static {
        launchOptions.add(Runner.guiParameter);
        launchOptions.add(Runner.toolsParameter);
        launchOptions.add(Runner.toolParameter);
        launchOptions.add(Runner.memoryParameter);
        launchOptions.add(availableProcessorsParameter);
        launchOptions.add(workDirParameter);
        launchOptions.add(continueParameter);
        launchOptions.add(forceParameter);
        launchOptions.add(startParameter);
        launchOptions.add(finishParameter);
        launchOptions.add(Runner.eaParameter);
        launchOptions.add(verboseParameter);
        launchOptions.add(helpParameter);
        launchOptions.add(helpAllParameter);
    }


    // personal (for tool) global input parameters
    public PathInValue workDir = new PathInValue(new FileInValue(workDirParameter));
    public Parameter<Integer> availableProcessors = new Parameter<Integer>(availableProcessorsParameter.description);
    {
        availableProcessors.set(availableProcessorsParameter);
    }


    // input and output parameters
    public final List<Parameter> inputParameters = new ArrayList<Parameter>();
    protected final List<String> inputParameterToolNames = new ArrayList<String>();

    protected <T> Parameter<T> addParameter(ParameterDescription<T> description) {
        assert checkParameterNameUniqueness(description);
        
        Parameter<T> p = Parameter.createParameter(description);

        inputParameters.add(p);
        inputParameterToolNames.add(name);
        return p;
    }

    protected <T extends Tool> T addSubTool(T subTool) {
        assert checkToolNameUniqueness(subTool);
        
        for (Parameter subParameter: subTool.inputParameters) {
            ParameterBuilder builder = ParameterBuilder.createBuilder(subParameter.description);

            if (subParameter.internalValue != null) {
                if (subParameter.internalValue instanceof FixingInValue) {
                    continue;
                }

                builder.optional();
                //noinspection unchecked
                builder.withDefaultValue(subParameter.internalValue);
            }

            // checking for name uniqueness
            for (int i = 0; i < inputParameters.size(); i++) {
                Parameter c = inputParameters.get(i);
                if (c.description.name.equals(subParameter.description.name)) {
                    // change name of parameter c
                    ParameterBuilder cBuilder = new ParameterBuilder(c.description);
                    String toolName = inputParameterToolNames.get(i);
                    cBuilder.withName(toolName + "." + c.description.name);
                    //noinspection unchecked
                    c.replaceDescription(cBuilder.create());

                    // change name of subParameter
                    builder.withName(subTool.name + "." + subParameter.description.name);
                }
            }

            //noinspection unchecked
            subParameter.replaceDescription(builder.create());

            assert checkParameterNameUniqueness(subParameter.description);
            inputParameters.add(subParameter);
            inputParameterToolNames.add(subTool.name);
        }

        setWorkDirInSubTool(subTool);
        return subTool;
    }
    
    protected void setWorkDirInSubTool(Tool subTool) {
        subTool.workDir.set(workDir, subTool.name);
    }

    private boolean checkParameterNameUniqueness(ParameterDescription description) {
        for (Parameter parameter : inputParameters) {
            checkParameterNameUniqueness(parameter, description);
        }
        for (Parameter parameter : launchOptions) {
            checkParameterNameUniqueness(parameter, description);
        }
        return true;
    }

    private void checkParameterNameUniqueness(Parameter parameter, ParameterDescription description) {
        assert !parameter.description.name.equals(description.name) :
                "Two different parameters have the same name '" + description.name + "' " +
                        "in tool " + name;
        if (description.shortOpt != null && parameter.description.shortOpt != null) {
            assert !parameter.description.shortOpt.equals(description.shortOpt) :
                    "Parameters " + parameter.description.name + " and " + description.name +
                    " have the same short option '" + description.shortOpt + "' " +
                    "in tool " + name;
        }
    }
    
    private boolean checkToolNameUniqueness(Tool subTool) {
        for (String name : inputParameterToolNames) {
            assert !name.equals(subTool.name) :
                    "Two different steps have the same name '" + name + "' " +
                            "in tool " + this.name;
        }
        return true;
    }
    
    
    public final List<OutputParameter> outputParameters = new ArrayList<OutputParameter>();

    protected <T> InValue<T> addOutput(@NotNull String name, @NotNull InValue<T> output, Class<T> tClass) {
        OutputParameter<T> p = new OutputParameter<T>(name, output, tClass);
        outputParameters.add(p);
        return p;
    }


    protected void clean() {
        steps.clear();
        cleanImpl();
    }

    /**
     * Runs after runImpl and all steps' runImpl methods have finished.
     * If no run of runImpl were made, then cleanImpl doesn't run!
     */
    protected abstract void cleanImpl();

    /**
     * ALWAYS runs after all tools' actions (after runImpl & clean methods or after loading results).
     */
    protected void postprocessing() {};


    // ========================== run methods =============================

    protected abstract void runImpl() throws ExecutionFailedException, IOException;

    private void simpleRun(boolean forceRun, String start, String finish) throws ExecutionFailedException {
        assert checkArguments();
        progress.reset();
        progress.start();

        {
            try {
                runImpl();
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            } catch (RuntimeException e) {
                if ((e.getCause() != null) && (e.getCause() instanceof IOException)) {
                    throw new ExecutionFailedException(e.getCause());
                }
                throw e;
            }
            progress.setStepsNumber(steps.size());
            runAllSteps(forceRun, start, finish);
            clean();
        }
    }

    /**
     * Only runs this tool without doing any other stuff (creating tool folder,
     * loading/dumping input and output parameters, etc.).
     */
    public void simpleRun() throws ExecutionFailedException {
        simpleRun(true, null, null);
    }


    /**
     * Runs tool as a step with creating tool folder, loading/dumping input and output parameters,
     *   creating SUCCESS file if run was successful.
     *
     * If forseRun is set then all data will be rewritten,
     *   else it will try to continue old run, if it is possible.
     *
     * @return true, if runAsStep called runImpl, else returns false (if SUCCESS file was found)
     */
    private boolean runAsStep(boolean forceRun, String start, String finish) throws ExecutionFailedException {
        progress.reset();

        File workDir = this.workDir.get();
        //noinspection ResultOfMethodCallIgnored
        workDir.mkdirs();

        File success = new File(workDir, SUCCESS_FILE);
        File inputParamFile = new File(workDir, IN_PARAM_FILE);
        File outputParamFile = new File(workDir, OUT_PARAM_FILE);


        forceRun |= !inputParamFile.exists();

        boolean shouldRun = forceRun || (start != null);

        boolean canLoadResult = inputParamFile.exists() && success.exists();
        if (!forceRun) {
            // trying to loading input parameters from previous run
            canLoadResult &= loadUnsetParametersFromProperties(inputParameters, inputParamFile);
        }
        shouldRun |= !canLoadResult;


        if (!shouldRun) {
            // loading result
            infoLaunching("SUCCESS file found for tool " + name + " - loading results...");

            // input parameters was already loaded
            // loading output parameters
            loadOutputParametersFromProperties(outputParameters, outputParamFile);
            postprocessing();

            return false;

        } else {
            infoLaunching("Running tool " + name);

            //noinspection ResultOfMethodCallIgnored
            success.delete();
            //noinspection ResultOfMethodCallIgnored
            outputParamFile.delete();


            dumpInputParameters(inputParameters, inputParamFile);

            // running
            subLevel++;
            String prevName = runningToolName;
            runningToolName = name;

            simpleRun(forceRun, start, finish);

            runningToolName = prevName;
            subLevel--;
            postprocessing();
            if (finish != null) {
                return true;
            }


            dumpOutputParameters(outputParameters, outputParamFile);

            // creating file SUCCESS
            boolean created = false;
            try {
                created = success.createNewFile();
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
            assert created;

            return true;
        }
    }



    /**
     * Runs tool as a step with creating tool folder, loading/dumping input and output parameters,
     *   creating SUCCESS file if run was successful.
     */
    public void run(boolean shouldContinue, boolean forceRun, String start, String finish) {
        // checking and processing --continue and --force options
        try {
            if (shouldContinue && forceRun) {
                throw new IllegalArgumentException("Continue and force options can't be set simultaneously");
            }

            File inputParamFile = new File(workDir.get(), IN_PARAM_FILE);
            if (inputParamFile.exists()) {
                if (shouldContinue) {
                    // OK
                } else if (forceRun) {
                    warn("Force run, all data in working directory will be rewritten!");
                } else {
                    System.err.print("Working directory (" + workDir.get() + "/) "+
                            "contains files from previous run, rewrite them? [Yes(y)/No(n), default:No] ");
                    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                    String ans = "";
                    try {
                        ans = in.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error occurs while reading answer");
                    }
                    if (TextUtils.isYes(ans)) {
                        forceRun = true;
                    } else {
                        System.exit(1);
                        return;
                    }
                }
            } else {
                // OK, no data will be rewritten
                forceRun = true;    // doesn't load anything
            }

            if (forceRun) {
                checkArguments();
            }

        } catch (IllegalArgumentException e) {
            exit(e);
            return;
        } catch (NotSetArgumentException e) {
            exit(e);
            return;
        }

        // running
        try {
            runAsStep(forceRun, start, finish);
        } catch (ExecutionFailedException e) {
            if (Tool.isInterrupted) {
                return;
            }
            error(mainLogger, e.getMessage(), null);    // to show message only
            mainLogger.trace("", (e.getCause() != null)? e.getCause() : e);    // to print stack trace to log file

            lastException = e;
            if (launchedFromGUI) {
                return;     // to show error in GUI
            } else {
                System.exit(1);
            }
        }
    }
    
    /**
     * Runs tool as a step with creating tool folder, loading/dumping input and output parameters,
     *   creating SUCCESS file if run was successful.
     */
    public void run() {
        run(false, false, null, null);
    }


    private final Queue<Tool> steps = new LinkedBlockingQueue<Tool>();

    /**
     * Adds the step that will be executed in future.<br></br>
     * All steps will be executed after runImpl method finished.
     */
    protected void addStep(Tool tool) {
        steps.add(tool);
    }

    private void runAllSteps(boolean forceRun, String start, String finish) throws ExecutionFailedException {
        // checking existence of start and finish tools' names
        checkBoundExistence(start);
        checkBoundExistence(finish);

        // running
        Tool step = null;
        try {
            while (!steps.isEmpty() && !isInterrupted) {
                step = steps.remove();

                if (step.name.equals(start)) {
                    forceRun = true;
                }

                String newStart = getNextBoundName(start, step.name);
                String newFinish = getNextBoundName(finish, step.name);

                progress.startingStep(step);
                forceRun |= step.runAsStep(forceRun, newStart, newFinish);
                progress.finishedCurrentStep();

                if (finish != null && finish.startsWith(step.name)) {
                    break;
                }
            }
            if (isInterrupted) {
                throw new ExecutionFailedException("Thread was interrupted!");
            }
        } finally {
            if (!steps.isEmpty()) {
                // we probably should remove next SUCCESS file
                Tool nextTool = steps.remove();
                if (finish != null && finish.equals(step.name)) {
                    // i.e. we finished all tool's substeps, and nextTool have outdated results
                    File success = new File(nextTool.workDir.get(), SUCCESS_FILE);
                    //noinspection ResultOfMethodCallIgnored
                    success.delete();
                    File inputParamFile = new File(nextTool.workDir.get(), IN_PARAM_FILE);
                    //noinspection ResultOfMethodCallIgnored
                    inputParamFile.delete();
                }
            }
        }
    }


    private void exit(Throwable e) {
        if (lastException != null) {
            return;
        }
        lastException = e;

        try {
            if (e instanceof OutOfMemoryError) {
                error(mainLogger, "Out of memory error! Try to increase memory via option -m <arg>", null);
                mainLogger.trace("", e);
            } else {
                error(mainLogger, "Uncaught exception: " + e, e);
            }
        } finally {
            if (!launchedFromGUI) {
                System.exit(1);
            }
        }
    }


    // ========================== main implementation =============================

    /**
     * @param args command line arguments
     */
    public void mainImpl(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (Tool.isInterrupted) {
                    return;
                }
                try {
                    exit(e);
                } catch (Throwable e1) {
                    System.err.println(e.getMessage());
                }
            }
        });

        // preparing
        List<Parameter> allParameters = new ArrayList<Parameter>();
        allParameters.addAll(inputParameters);
        allParameters.addAll(launchOptions);

        // parsing args
        parseArgs(allParameters, args);
        if (helpParameter.get() || helpAllParameter.get() || (args.length == 0 && !launchedFromGUI)) {
            printHelp(helpAllParameter.get());
            return;
        }

        updateFileLoggers();

        // processing launch options
        verbose = verboseParameter.get();
        if (verbose) {
            ConsoleAppender ca = (ConsoleAppender) Logger.getRootLogger().getAppender("console");
            ca.setThreshold(Level.DEBUG);
        }
        boolean shouldContinue = (continueParameter.get() != null && continueParameter.get());
        boolean forceRun = (forceParameter.get() != null && forceParameter.get());

        mainLogger.setLevel(Level.ALL);
        debug(mainLogger, "Program version = " + Runner.getVersion());
        debug(mainLogger, "Program parameters = " + Arrays.toString(args));
        assert printAssertionsEnabled();
        debug(mainLogger, "Available memory = " + Misc.availableMemoryAsString() + ", used = " + Misc.usedMemoryAsString());
        debug(mainLogger, "Available processors = " + availableProcessors.get());


        // running main tool
        run(shouldContinue, forceRun, startParameter.get(), finishParameter.get());

//        if (!launchedFromGUI) {
//            System.exit(0);
//        }
    }

    public static void parseArgs(List<Parameter> params, String[] args) {
        // parsing args
        Options options = new Options();
        for (Parameter p : params) {
            options.addOption(p.getAsOption());
        }

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            error(mainLogger, "Cannot parse command line", e);
            System.exit(1);
            return;
        }

        // setting parameters
        for (Parameter p : params) {
            String key = p.description.name;
            String[] value;
            if (cmd.hasOption(key)) {
                value = cmd.getOptionValues(key);
                if (value == null) {
                    if (p.description.tClass == Boolean.class) {    // option doesn't have an argument
                        value = new String[]{ "true" };
                    } else {
                        value = new String[]{};
                    }
                }
                parseAndSet(p, value, p.description.tClass, p.description.name);
            }
        }
    }

    private static FileAppender lastLogApp, logApp;

    public static final Date startDate = new Date();
    public static String startTimestamp = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(startDate);

    private void updateFileLoggers() {
        if (logApp != null) {
            Logger.getRootLogger().removeAppender(lastLogApp);
            Logger.getRootLogger().removeAppender(logApp);
        }


        String header = "Log created at " +
                new SimpleDateFormat("dd-MMM-yyyy (EEE) HH:mm:ss").format(startDate);

        File logsDir = workDir.append("logs").get();
        //noinspection ResultOfMethodCallIgnored
        workDir.get().mkdirs();
        //noinspection ResultOfMethodCallIgnored
        logsDir.mkdirs();

        String lastLogFile = workDir.append("log").toString();
        String logFile = logsDir + File.separator + "log_" + startTimestamp;

        lastLogApp = addFileLogger(lastLogFile, header);
        logApp = addFileLogger(logFile, header);
    }
    
    private FileAppender addFileLogger(String logFile, String header) {
        // adding header to logs file
        try {
            PrintWriter pw = new PrintWriter(logFile);
            pw.println(header);
            pw.close();
        } catch (FileNotFoundException e) {
            warn(mainLogger, "Can't create log file");
        }

        // adding file logger
        EnhancedPatternLayout layout = new EnhancedPatternLayout("%d{dd-MMM-yy  HH:mm:ss,SSS}  %-5p  %m%n");
        try {
            FileAppender fa = new FileAppender(layout, logFile);
            fa.activateOptions();
            Logger.getRootLogger().addAppender(fa);
            fa.setThreshold(Level.ALL);
            return fa;
        } catch (IOException e) {
            warn(mainLogger, "Can't create log file appender");
        }
        return null;
    }

    public File[] getLogFiles() {
        return new File[]{new File(lastLogApp.getFile()), new File(logApp.getFile())};
    }

    private boolean printAssertionsEnabled() {
        info(mainLogger, "Assertions checking is enabled !");
        return true;
    }


    // ========================== private auxiliary methods =============================

    protected boolean checkArguments() {
        for (Parameter p : inputParameters) {
            if (p.description.mandatory && p.get() == null) {
                String shortOpt = "";
                if (p.description.shortOpt != null) {
                    shortOpt = " (-" + p.description.shortOpt + ")";
                }
                throw new NotSetArgumentException("Mandatory argument --" + p.description.name + shortOpt + " not set");
            }
        }
        return true;
    }

    private void checkBoundExistence(String bound) {
        if (bound == null) {
            return;
        }

        String firstName = getFirstBoundName(bound);
        boolean found = false;
        for (Tool tool : steps) {
            found |= tool.name.equals(firstName);
        }
        if (!found) {
            throw new IllegalArgumentException("There is no substep with name '" + firstName +
                    "' in step " + name + "!");
        }
    }


    /**
     * @return canLoadResult (true, if no parameter with different values (set and in properties) found)
     */
    private static boolean loadUnsetParametersFromProperties(List<Parameter> parameters, File propertiesFile) throws ExecutionFailedException {
        if (!propertiesFile.exists()) {
            return false;
        }

        boolean canLoadResult = true;
        PropertiesConfiguration properties = loadProperties(propertiesFile);

        for (Parameter parameter : parameters) {
            String key = parameter.description.name;
            Class tClass = parameter.description.tClass;

            String[] strPropValue = (tClass.isArray()) ? properties.getStringArray(key) : new String[]{properties.getString(key)};
            Object[] propValue = parse(strPropValue, tClass, key);

            if (parameter.get() == null) {
                // parameter not set
                set(parameter, propValue, tClass);
            } else {
                // parameter has a value
                boolean equals = checkEquals(parameter, propValue, tClass);
                if (!equals &&
                        ((parameter.description.defaultValue != null && Misc.checkEquals(parameter.get(), parameter.description.defaultValue.get())) ||
                        (parameter.description.memoryParameter && Long.parseLong((String) propValue[0]) < (Long) parameter.get()))) {
                    set(parameter, propValue, tClass);
                    equals = true;
                }
                canLoadResult &= equals;
                if (!equals) {
                    debug(mainLogger, "Parameter " + parameter.description.name + " changed from last run");
                    debug(mainLogger, "last value = " + objToStr(propValue) + ", cur value = " + objToStr(parameter.get()) + ", " +
                            "def value = " + objToStr(parameter.description.defaultValue));
                }
            }
        }

        return canLoadResult;
    }

    private static void loadOutputParametersFromProperties(List<OutputParameter> parameters, File propertiesFile) throws ExecutionFailedException {
        PropertiesConfiguration properties = loadProperties(propertiesFile);
        for (OutputParameter parameter : parameters) {
            String key = parameter.name;
            Class tClass = parameter.tClass;
            String[] value = (tClass.isArray()) ? properties.getStringArray(key) : new String[]{properties.getString(key)};
            parseAndSet(parameter, value, tClass, key);
        }
    }
    
    private static void dumpInputParameters(List<Parameter> parameters, File propertiesFile) throws ExecutionFailedException {
        PropertiesConfiguration inProperties = new PropertiesConfiguration();
        inProperties.setDelimiterParsingDisabled(true);
        for (Parameter parameter : parameters) {
            addProperty(inProperties, parameter, parameter.description.name);
        }
        dumpProperties(inProperties, propertiesFile);
    }
    
    private static void dumpOutputParameters(List<OutputParameter> parameters, File propertiesFile) throws ExecutionFailedException {
        PropertiesConfiguration outProperties = new PropertiesConfiguration();
        outProperties.setDelimiterParsingDisabled(true);
        for (OutputParameter parameter : parameters) {
            addProperty(outProperties, parameter, parameter.name);
        }
        dumpProperties(outProperties, propertiesFile);
    }


    private static void parseAndSet(OutValue parameter, String[] strValue, Class tClass, String parameterName) {
        Object[] value = parse(strValue, tClass, parameterName);
        set(parameter, value, tClass);
    }

    private static void set(OutValue parameter, Object[] value, Class tClass) {
        if (tClass.isArray()) {
            //noinspection unchecked
            parameter.set(value);
        } else {
            //noinspection unchecked
            parameter.set(value[0]);
        }
    }
    
    private static Object[] parse(String[] value, Class tClass, String parameterName) {
        if (value == null) {
            return null;
        }

        if (value.length == 0 || value[0] == null) {
            return (tClass.isArray()) ? (Object[]) Array.newInstance(tClass.getComponentType(), value.length) : new Object[]{null};
        }

        if (tClass.isArray()) {
            Class tElementClass = tClass.getComponentType();

            // re-parsing array
            String initStr = TextUtils.arrayToString(value);
            value = parseArray(initStr);

            // converting
            Object[] newValue = (Object[]) Array.newInstance(tElementClass, value.length);
            for (int i = 0; i < value.length; i++) {
                try {
                    newValue[i] = convert(value[i], tElementClass);
                } catch (Exception e) {
                    throw new RuntimeException("Can't convert value '" + value[i] + "' of parameter '" + parameterName + "' to type '" + tClass + "'", e);
                }
            }
            return newValue;

        } else {
            // converting
            Object newValue = null;
            try {
                newValue = convert(value[0], tClass);
            } catch (Exception e) {
                throw new RuntimeException("Can't convert value '" + value[0] + "' of parameter '" + parameterName + "' to type '" + tClass + "'", e);
            }

            return new Object[]{newValue};
        }
    }
    
    private static Object convert(Object value, Class tClass) throws Exception {
        //noinspection unchecked
        Constructor constr = tClass.getConstructor(value.getClass());
        value = constr.newInstance(value);
        return value;
    }
    
    private static String[] parseArray(String s) {
        StringTokenizer st = new StringTokenizer(s, "[, ]");
        String[] array = new String[st.countTokens()];
        for (int i = 0; i < array.length; i++) {
            array[i] = st.nextToken();
        }
        return array;
    }


    private static boolean checkEquals(InValue parameter, Object[] value, Class tClass) {
        boolean res;
        if (tClass.isArray()) {
            res = Misc.checkEquals((Object[]) parameter.get(), value);
        } else {
            res = Misc.checkEquals(parameter.get(), value[0]);
        }
        return res;
    }



    private static PropertiesConfiguration loadProperties(File propertiesFile) throws ExecutionFailedException {
        try {
            PropertiesConfiguration res = new PropertiesConfiguration();
            res.setDelimiterParsingDisabled(true);
            Reader r = new BufferedReader(new FileReader(propertiesFile));
            res.load(r);
            return res;
        } catch (ConfigurationException e) {
            throw new ExecutionFailedException("Can't load properties from " + propertiesFile, e);
        } catch (FileNotFoundException e) {
            throw new ExecutionFailedException("File '" + propertiesFile + "'" + " with properties not found", e);
        }
    }

    private static void dumpProperties(PropertiesConfiguration config, File propertiesFile) throws ExecutionFailedException {
        PrintWriter out;
        try {
            out = new PrintWriter(propertiesFile);
            config.save(out);
            out.close();
        } catch (IOException e) {
            throw new ExecutionFailedException("Can't dump configuration", e);
        } catch (ConfigurationException e) {
            throw new ExecutionFailedException("Can't dump configuration", e);
        }
    }

    private static void addProperty(Configuration properties, InValue parameter, String name) {
        Object value = parameter.get();
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] objArray = (Object[]) value;
                for (int i = 0; i < objArray.length; i++) {
                    properties.addProperty(name, objectToString(objArray[i]));
                }
//                String[] x = properties.getStringArray(name);
//                debug(mainLogger, true, "added property " + name + ": " + Arrays.toString(x) + " of length " + x.length);
            } else {
                properties.addProperty(name, objectToString(value));
            }
        }
    }

    public static String objectToString(Object propValue) {
        if (propValue instanceof File) {
            return ((File) propValue).getAbsolutePath();
        }
        if (propValue instanceof PathInValue) {
            return ((PathInValue) propValue).get().getAbsolutePath();
        }

        
        if (propValue.getClass().isArray()) {
            Object[] x = (Object[]) propValue;
            return Arrays.toString(x);
        }
        

        return propValue.toString();
    }


    private static String getFirstBoundName(String curBound) {
        if (curBound != null) {
            int i = curBound.indexOf('.');
            if (i != -1) {
                curBound = curBound.substring(0, i);
            }
            return curBound;
        }
        return null;
    }

    private static String getNextBoundName(String curBound, String subToolName) {
        if (curBound != null && curBound.startsWith(subToolName)) {
            if (curBound.length() == subToolName.length()) {
                return null;
            }
            return curBound.substring(subToolName.length() + 1);
        }
        return null;
    }




    // ========================== help, config =============================

    public void printHelp(boolean all) {
        System.out.println("Tool:           " + name);
        System.out.println("Description:    " + description);
        System.out.println("Input parameters " + (all ? "(all)" : "(only important)") + ":");

        for (Parameter p : inputParameters) {
            if (all || p.description.important) {
                String dh = p.description.printHelp();
                System.out.println("\t" + dh);
            }
        }
        
        System.out.println();
        System.out.println("Launch options " + (all ? "(all)" : "(only important)") + ":");
        for (Parameter p : launchOptions) {
            if (all || p.description.important) {
                String dh = p.description.printHelp();
                System.out.println("\t" + dh);
            }
        }

        if (!all) {
            System.out.println();
            System.out.println("To see all parameters and options add --help-all.");
        }
    }

    @Deprecated
    public void printConfig() {
        // TODO: ReWrite it
        String pack = getClass().getPackage().getName();
        String name = getClass().getSimpleName();
        System.out.println("// Package " + pack);
        System.out.println("// Class " + name);

        for (Parameter p : inputParameters) {
            String c = p.description.printConfig();
            if (c != null) {
                System.out.println(name + "." + c);
            }
        }
        
        System.out.println();
    }



    // ========================== simplified set methods =============================

    protected static <T> void setFix(Parameter<T> parameter, InValue<T> valueIn) {
        parameter.set(new ForwardingFixingInValue<T>(valueIn));
    }

    protected static <T> void setFix(Parameter<T> parameter, T value) {
        parameter.set(new SimpleFixingInValue<T>(value));
    }

    protected static <T> void setDefault(Parameter<T> parameter, InValue<T> value) {
        assert !(value instanceof FixingInValue);
        parameter.set(value);
    }

    protected static <T> void setFixDefault(Parameter<T> parameter) {
        assert parameter.description.defaultValue != null :
                "null default value of parameter " + parameter.description.name;
        setFix(parameter, parameter.description.defaultValue);
    }




    // ========================== output methods =============================

    // logging
    private static int subLevel = 0;    // depth of nesting. 0 = init, 1 = main tool, 2 = sub tool.
    private static String runningToolName = "";

    public static void error(Logger logger, Object message, Throwable e) {
        if (curProgress.length() > 0) {
            System.err.println();
        }
        logger.error(getPrefix(false, false, "ERROR: ", logger) + message, e);
    }
    public static void warn(Logger logger, Object message) {
        clearProgressImpl();
        logger.warn(getPrefix(false, true, "WARN: ", logger) + message);
        System.err.print(curProgress);
    }
    public static void info(Logger logger, Object message) {
        clearProgressImpl();
        logger.info(getPrefix(true, true, "", logger) + message);
        System.err.print(curProgress);
    }
    public static void debug(Logger logger, Object message) {
        if (verbose) clearProgressImpl();
        logger.debug(getPrefix(true, true, "", logger) + message);
        if (verbose) System.err.print(curProgress);
    }

    protected void error(Object message, Throwable e) {
        error(logger, message, e);
    }
    protected void error(Object message) {
        error(logger, message, null);
    }
    protected void warn(Object message) {
        warn(logger, message);
    }
    protected void info(Object message) {
        info(logger, message);
    }
    private void infoLaunching(Object message) {
        mainLogger.info(getPrefix(true, false, "", mainLogger) + message);
    }
    protected void debug(Object message) {
        debug(logger, message);
    }


    private static String getPrefix(boolean printTree, boolean isComment, String typeStr, Logger logger) {
        return (printTree? multiply("|  ", subLevel) : "")
                + (isComment? " " : "") + typeStr
                + (!logger.getName().equals(runningToolName) && printTree ? logger.getName()+": " : "");
    }



    // text progress
    private static String curProgress = "";
    
    protected void showProgress(String newProgress) {
        debug(newProgress);
        showProgressOnly(newProgress);
    }
    
    public static void showProgressOnly(String newProgress) {
        System.err.print(
                ((newProgress.length() < curProgress.length()) ?
                        "\r" + multiply(" ", curProgress.length())     // cleaning previous message
                        : "") +
                "\r" + newProgress
        );
        curProgress = newProgress;
    }

    protected void addProgress(String appendStr) {
        debug(appendStr);
        addProgressOnly(appendStr);
    }

    public static void addProgressOnly(String appendStr) {
        System.err.print(appendStr);
        curProgress += appendStr;
    }

    public static void clearProgress() {
        clearProgressImpl();
        curProgress = "";
    }

    private static void clearProgressImpl() {
        if (curProgress.length() > 0) {
            System.err.print("\r" + multiply(" ", curProgress.length()) + "\r");
        }
    }



    // ========================== miscellaneous methods =============================
    /**
     * Executes command, waits for it to finnish, checks the exit code and throws exception, if not zero.
     */
    public void execCommand(String command) throws IOException, InterruptedException, ExecutionFailedException {
        execCommand(command, true, true);
    }

    /**
     * Executes command, waits for it to finnish, checks the exit code and throws exception, if not zero.
     */
    public void execCommandWithoutLogging(String command) throws IOException, InterruptedException, ExecutionFailedException {
        execCommand(command, false, true);
    }

    /**
     * Executes command and waits for it to finnish. Returns exit code.
     */
    public int execCommandWithoutChecking(String command) throws ExecutionFailedException, IOException, InterruptedException {
        return execCommand(command, false, false);
    }

    private int execCommand(String command, boolean logging, boolean checking) throws IOException, InterruptedException, ExecutionFailedException {
        String commandOnly = TextUtils.getFirstWord(command);
        if (logging) {
            info("Running " + commandOnly + " command...");
        }

        debug("Command = '" + command + "'");
        Process p = Runtime.getRuntime().exec(command);
        int exitValue = p.waitFor();
        if (logging) {
            info("Done, exit code = " + exitValue);
        } else {
            debug("Done, exit code = " + exitValue);
        }

        if (checking && exitValue != 0) {
            String erS = "Command execution log:" + TextUtils.NL + TextUtils.getLogText(p);
            info(erS);
            throw new ExecutionFailedException("Non-zero return code of running " + commandOnly + " command");
        }
        return exitValue;
    }
    
    public String getStageName(int curLang) {
        return name;        // if we have no other info
    }
}
