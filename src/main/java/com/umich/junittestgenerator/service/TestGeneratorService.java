package com.umich.junittestgenerator.service;

import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestGeneratorService {

    private final LLMClient llmClient;

    public TestGeneratorService(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public String generateTestCases(String sourceCode){
        return llmClient.generateTestCases(sourceCode);
    }
    public String generateTestCasesOld(String sourceCode) {
        try {
            // Extract the class name from the source code
            String className = extractClassName(sourceCode);
            if (className == null) {
                return "Error: Unable to extract class name.";
            }

            // Write the source code to a temporary file
            String tempDir = "./temp";
            Files.createDirectories(Paths.get(tempDir));
            String javaFilePath = tempDir + "/" + className + ".java";
            Files.write(Paths.get(javaFilePath), sourceCode.getBytes());

            if (requiresStubs(sourceCode)) {
                generateStubsIfNeeded(sourceCode, tempDir);
            }

            // Compile the source file
            File[] javaFiles = new File(tempDir).listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles == null) return "Error: No Java files found in temp directory.";

            String[] compileArgs = new String[javaFiles.length];
            for (int i = 0; i < javaFiles.length; i++) {
                compileArgs[i] = javaFiles[i].getPath();
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream errOut = new ByteArrayOutputStream();
            int compilationResult = compiler.run(null, null, errOut, compileArgs);
            if (compilationResult != 0) {
                return "Error: Compilation failed. " + errOut.toString();
            }

            // Load the compiled class using a URLClassLoader
            File tempDirectory = new File(tempDir);
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{tempDirectory.toURI().toURL()});
            Class<?> clazz = Class.forName(className, true, classLoader);

            // Generate the test class using reflection
            String testClassContent = generateTestClass(clazz);
            // Write the generated test class to a Java file
            String testClassPath = tempDir + "/" + className + "Test.java";
            Files.write(Paths.get(testClassPath), testClassContent.getBytes());
            return testClassContent;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String extractClassName(String sourceCode) {
        // Naively extract the class name from "public class ClassName"
        String keyword = "public class ";
        int index = sourceCode.indexOf(keyword);
        if (index == -1) return null;
        int start = index + keyword.length();
        // Look for space or "{" after the class name
        int end = sourceCode.indexOf(" ", start);
        if (end == -1) {
            end = sourceCode.indexOf("{", start);
        }
        if (end == -1) return null;
        return sourceCode.substring(start, end).trim();
    }

    private String generateTestClass(Class<?> clazz) {
        StringBuilder testClass = new StringBuilder();
        String testClassName = clazz.getSimpleName() + "Test";

        // Header imports (always include both JUnit + Mockito)
        testClass.append("import org.junit.jupiter.api.Test;\n")
                .append("import static org.junit.jupiter.api.Assertions.*;\n")
                .append("import static org.mockito.Mockito.*;\n")
                .append("import org.mockito.Mockito;\n\n")
                .append("public class ").append(testClassName).append(" {\n\n");

        // Determine constructor type
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> targetConstructor = constructors.length > 0 ? constructors[0] : null;

        if (targetConstructor != null && targetConstructor.getParameterCount() > 0) {
            // ✅ Constructor with dependencies - use mocks
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            StringJoiner mocksJoiner = new StringJoiner(", ");
            StringBuilder mockFields = new StringBuilder();

            for (Class<?> paramType : paramTypes) {
                String mockName = "mock" + paramType.getSimpleName();
                mockFields.append("    ").append(paramType.getSimpleName())
                        .append(" ").append(mockName)
                        .append(" = mock(").append(paramType.getSimpleName()).append(".class);\n");
                mocksJoiner.add(mockName);
            }

            testClass.append(mockFields.toString()).append("\n");
            testClass.append("    ").append(clazz.getSimpleName()).append(" instance = new ")
                    .append(clazz.getSimpleName()).append("(").append(mocksJoiner.toString()).append(");\n\n");

        } else {
            // ✅ No-arg constructor - use plain instance
            testClass.append("    ").append(clazz.getSimpleName()).append(" instance = new ")
                    .append(clazz.getSimpleName()).append("();\n\n");
        }

        // Test method generation
        for (Method method : clazz.getDeclaredMethods()) {
            testClass.append("    @Test\n")
                    .append("    public void test").append(capitalize(method.getName())).append("() {\n");

            if (method.getParameterCount() == 0) {
                if (method.getReturnType() != void.class) {
                    testClass.append("        ").append(method.getReturnType().getSimpleName())
                            .append(" result = instance.").append(method.getName()).append("();\n")
                            .append("        assertNotNull(result);\n");
                } else {
                    testClass.append("        instance.").append(method.getName()).append("();\n");
                }
            } else {
                String dummyParams = generateDummyParameters(method.getParameterTypes());
                if (method.getReturnType() != void.class) {
                    testClass.append("        ").append(method.getReturnType().getSimpleName())
                            .append(" result = instance.").append(method.getName())
                            .append("(").append(dummyParams).append(");\n")
                            .append("        assertNotNull(result);\n");
                } else {
                    testClass.append("        instance.").append(method.getName())
                            .append("(").append(dummyParams).append(");\n");
                }
            }

            testClass.append("    }\n\n");

            // Optional parameterized test
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == int.class) {
                testClass.append("    @org.junit.jupiter.params.ParameterizedTest\n")
                        .append("    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 3})\n")
                        .append("    public void test").append(capitalize(method.getName()))
                        .append("WithParams(int param) {\n");
                if (method.getReturnType() != void.class) {
                    testClass.append("        int result = instance.").append(method.getName()).append("(param);\n")
                            .append("        assertTrue(result >= 0);\n");
                } else {
                    testClass.append("        instance.").append(method.getName()).append("(param);\n");
                }
                testClass.append("    }\n\n");
            }
        }

        testClass.append("}");
        return testClass.toString();
    }


    private String generateDummyParameters(Class<?>[] parameterTypes) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Class<?> paramType : parameterTypes) {
            if (paramType == int.class) {
                joiner.add("0");
            } else if (paramType == double.class) {
                joiner.add("0.0");
            } else if (paramType == boolean.class) {
                joiner.add("false");
            } else if (paramType == long.class) {
                joiner.add("0L");
            } else {
                joiner.add("null");
            }
        }
        return joiner.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void generateStubsIfNeeded(String sourceCode, String tempDir) throws IOException {
        // Find all class references with "new ClassName(" or type declarations
        Set<String> classNames = new HashSet<>();

        // Capture class types from constructor
        Pattern constructorPattern = Pattern.compile("public\\s+\\w+\\s*\\(([^)]*)\\)");
        Matcher constructorMatcher = constructorPattern.matcher(sourceCode);
        if (constructorMatcher.find()) {
            String[] params = constructorMatcher.group(1).split(",");
            for (String param : params) {
                String[] parts = param.trim().split(" ");
                if (parts.length >= 2) {
                    classNames.add(parts[0].trim());
                }
            }
        }

        // Optional: Capture field types or method calls (more accurate)
        Pattern typePattern = Pattern.compile("(\\w+)\\s+\\w+\\s*(=|;)");
        Matcher typeMatcher = typePattern.matcher(sourceCode);
        while (typeMatcher.find()) {
            classNames.add(typeMatcher.group(1).trim());
        }

        // Filter out Java standard types
        Set<String> javaTypes = Set.of("int", "String", "boolean", "double", "float", "long", "char", "void");

        for (String className : classNames) {
            if (javaTypes.contains(className)) continue;

            String stubPath = tempDir + "/" + className + ".java";
            File stubFile = new File(stubPath);
            if (!stubFile.exists()) {
                String stubCode = "public class " + className + " {\n"
                        + "    public String getDataFromApi(String id) { return \"mock-data\"; }\n"
                        + "}";
                Files.write(Paths.get(stubPath), stubCode.getBytes());
            }
        }
    }

    private boolean requiresStubs(String sourceCode) {
        // Identify if there are any external classes that are not part of Java's primitive or standard types
        Set<String> classNames = new HashSet<>();

        // Capture class types from constructor or field definitions
        Pattern constructorPattern = Pattern.compile("public\\s+\\w+\\s*\\(([^)]*)\\)");
        Matcher constructorMatcher = constructorPattern.matcher(sourceCode);
        if (constructorMatcher.find()) {
            String[] params = constructorMatcher.group(1).split(",");
            for (String param : params) {
                String[] parts = param.trim().split(" ");
                if (parts.length >= 2) {
                    classNames.add(parts[0].trim());
                }
            }
        }

        // Capture field types or method calls (additional external classes)
        Pattern typePattern = Pattern.compile("(\\w+)\\s+\\w+\\s*(=|;)");
        Matcher typeMatcher = typePattern.matcher(sourceCode);
        while (typeMatcher.find()) {
            classNames.add(typeMatcher.group(1).trim());
        }

        // Filter out Java standard types
        Set<String> javaTypes = Set.of("int", "String", "boolean", "double", "float", "long", "char", "void");

        // If there are external class dependencies that are not part of the Java standard types, return true
        for (String className : classNames) {
            if (!javaTypes.contains(className)) {
            }
        }

        return false;
    }

}
