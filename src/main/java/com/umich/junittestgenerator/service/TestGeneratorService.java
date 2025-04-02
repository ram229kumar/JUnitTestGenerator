package com.umich.junittestgenerator.service;

import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;

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

            // Compile the source file
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream errOut = new ByteArrayOutputStream();
            int compilationResult = compiler.run(null, null, errOut, javaFilePath);
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

        // Header for the generated test class
        testClass.append("import org.junit.jupiter.api.Test;\n")
                .append("import org.junit.jupiter.params.ParameterizedTest;\n")
                .append("import org.junit.jupiter.params.provider.ValueSource;\n")
                .append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                .append("public class ").append(testClassName).append(" {\n\n")
                .append("    private final ").append(clazz.getSimpleName()).append(" instance = new ")
                .append(clazz.getSimpleName()).append("();\n\n");

        // Loop through each declared method in the class
        for (Method method : clazz.getDeclaredMethods()) {
            // Generate a basic test method
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

            // If the method has one parameter of type int, add a parameterized test
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == int.class) {
                testClass.append("    @ParameterizedTest\n")
                        .append("    @ValueSource(ints = {1, 2, 3})\n")
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
}
