package ru.ifmo.genetics;

import ru.ifmo.genetics.utils.tool.Tool;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static ru.ifmo.genetics.utils.TextUtils.fit;

public class ToolsScanner {
    public static final String USED_FS = "/";



    public static void main(String[] args) {
        new ToolsScanner(
                "ru/ifmo/genetics/tools", ToolsScanner.class
        ).run();
    }



    protected ToolsScanner(String toolsPath, Class classInYourPackage) {
        this.toolsPath = toolsPath.replace(File.separator, USED_FS);
        this.classInYourPackage = classInYourPackage;
    }

    private final String toolsPath;
    private final Class classInYourPackage;
    private List<String> classes;
    private List<Tool> tools;


    protected void run() {
        System.err.println("Searching for tools...");
        
        findClasses();
        identifyTools();
        printToolsInfoToFile();

        System.err.println("Found " + tools.size() + " tools");
    }



    private void findClasses() {
        classes = new ArrayList<String>();

        String protocol = Runner.getProtocol(classInYourPackage);
        String path = Runner.getSourceRootPath(classInYourPackage);

//        System.out.println("Protocol = " + protocol);
//        System.out.println("Path = " + path);

        if (protocol.equals("file")) {
            File toolsDir = new File(path, toolsPath);
//            System.err.println("recursive walk from " + toolsDir);
            recursiveWalk(toolsDir);

        } else if (protocol.equals("jar")) {
//            System.err.println("scan jar file " + path);
            scanJarFile(path);

        } else {
            throw new RuntimeException("Unknown protocol " + protocol + ", can't scan files");
        }

//        System.err.println("classes.size = " + classes.size());
//        System.err.println("classes[0] = " + classes.get(0));
    }



    private void recursiveWalk(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                recursiveWalk(file);
            } else {
                String fileName = file.getAbsolutePath();
                fileName = fileName.replace(File.separator, USED_FS);
                if (isGoodClassFile(fileName)) {
                    classes.add(fileName);
                }
            }
        }
    }

    private void scanJarFile(String jarFileName) {
        try {
            ZipFile jarFile = new ZipFile(new File(jarFileName));
            Enumeration<? extends ZipEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();
                if (fileName.startsWith(toolsPath) && isGoodClassFile(fileName)) {
                    classes.add(fileName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isGoodClassFile(String fileName) {
        int li = fileName.lastIndexOf(USED_FS);
        fileName = (li != -1) ? fileName.substring(li + 1) : fileName;
        int pi = fileName.lastIndexOf('.');
        String type = (pi != -1) ? fileName.substring(pi + 1) : "";
        return type.equals("class") && !fileName.contains("$");
    }



    private void identifyTools() {
        tools = new ArrayList<Tool>();

        ClassLoader cl = classInYourPackage.getClassLoader();
        for (String classFileName : classes) {
            String className = getClassName(classFileName);
            Class clazz;
            try {
                clazz = cl.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (Tool.class.isAssignableFrom(clazz) && (getMainMethod(clazz) != null)) {
                // clazz extends Tool and has main method
                Tool toolClass = null;
                try {
                    toolClass = (Tool) clazz.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                tools.add(toolClass);
            }
        }
    }

    private Method getMainMethod(Class clazz) {
        Method main = null;
        try {
            main = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
        }
        return main;
    }

    private String getClassName(String fileName) {
        int ind = fileName.indexOf(toolsPath);
        if (ind == -1) {
            throw new RuntimeException("si = -1");
        }
        String className = fileName.substring(ind);

        int pi = className.lastIndexOf('.');
        className = className.substring(0, pi);     // removing ".class"
        className = className.replaceAll(USED_FS, ".");

        return className;
    }


    private void printToolsInfoToFile() {
        Collections.sort(tools, new Comparator<Tool>() {
            @Override
            public int compare(Tool o1, Tool o2) {
                return o1.name.compareTo(o2.name);
            }
        });

        PrintWriter out = null;
        try {
            out = new PrintWriter("TOOLS");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can't create TOOLS file: " + e);
        }

        for (Tool t : tools) {
            out.println(fit(t.name, 40) + " " + fit(t.getClass().getName(), 80) + " " + t.description);
        }

        out.close();
    }

}
