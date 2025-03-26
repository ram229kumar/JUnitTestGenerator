package com.umich.junittestgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class LLMClient {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    Logger logger = Logger.getLogger(getClass().getName());
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    public String generateTestCases(String sourceCode) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String requestBody = getString(sourceCode);

            logger.info("Request Body: " + requestBody); // Log request body

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            logger.info("Response Code: " + responseCode); // Log response code

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    logger.info("Response: " + response.toString()); // Log response

                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonResponse = objectMapper.readTree(response.toString());
                    return jsonResponse.get("choices").get(0).get("message").get("content").asText();
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    logger.info("Error Response: " + errorResponse.toString()); // Log error response
                    return "Error: " + responseCode + " - " + errorResponse.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log stack trace
            return "Error: " + e.getMessage();
        }
    }

    private static String getString(String sourceCode) {
        String prompt = "Generate a JUnit 5 test case for this Java class. If any method interacts with an external service, repository, or another class, use Mockito to mock it Ensure tests include both valid and invalid inputs, exception scenarios, and verification of method calls and Only respond with code as plain text without code block syntax around it:\n" + sourceCode;
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        String requestBody = "{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}],\"max_tokens\":500}";
        return requestBody;
    }
}
