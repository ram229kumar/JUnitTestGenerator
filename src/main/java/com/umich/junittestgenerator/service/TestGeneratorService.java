package com.umich.junittestgenerator.service;

import com.umich.junittestgenerator.util.JavaParserUtil;
import org.springframework.stereotype.Service;

@Service
public class TestGeneratorService {

    public String generateTestCases(String sourceCode) {
        // Extract the class name using a simple regex
        String className = JavaParserUtil.extractClassName(sourceCode);
        // Extract the method names
        String[] methods = JavaParserUtil.extractMethods(sourceCode);

        // Build a simple JUnit test class as a String
        StringBuilder testClass = new StringBuilder();
        testClass.append("import org.junit.jupiter.api.Test;\n")
                .append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                .append("public class ").append(className).append("Test {\n\n");

        // For each method, add a basic test method skeleton
        for (String method : methods) {
            testClass.append("    @Test\n")
                    .append("    public void test").append(capitalize(method)).append("() {\n")
                    .append("        ").append(className).append(" obj = new ").append(className).append("();\n")
                    .append("        // TODO: add test logic for method ").append(method).append("\n")
                    .append("    }\n\n");
        }

        testClass.append("}");
        return testClass.toString();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
