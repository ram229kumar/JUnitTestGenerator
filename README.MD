# Introduction

A Spring boot application which can automatically generate simple JUnit5 test cases.
This project takes Java Class Code(For now future developments could change to an entire application) via a REST API, compiles the code dynamically and then uses a simple parser to extract the classes & methods in them to generate corresponding JUnit test class.

# Prerequisites

- Java 17+
- IDE (This Application is developed using Intellij)
- Postman (To test the application)

# Usage

## Cloning the repo
 
- In order to use the repo we are going to clone it first to our local systems which can be done either using CLI or Git UI.
```shell
git clone https://github.com/ram229kumar/JUnitTestGenerator.git
```

- Once the repo is cloned you can build the project using the UI.
- After that you can run the application by going to the main [JUnitTestGeneratorApplication.java](/src/main/java/com/umich/junittestgenerator/JUnitTestGeneratorApplication.java)

## API Usage

- Since the run is started you have to see the terminal scrolling with showing "Completed initialization in Z(some time) ms" at the end if the application is cloned and built properly.
- Open postman application and create a new request using the URL : ```http://localhost:8080/api/test-generator/generate``` with ```POST``` Method.
- Sample Request body (JSON Format)
```json
{
    "sourceCode": "public class MyClass { public int square(int num) { return num * num; } public String greet(String name) { return \"Hello, \" + name; } }"
}
```

- Once the request been sent you will be able to see the following output.

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyClassTest {

    @Test
    public void testSquare() {
        MyClass obj = new MyClass();
        // TODO: add test logic for method square
    }

    @Test
    public void testGreet() {
        MyClass obj = new MyClass();
        // TODO: add test logic for method greet
    }

}
```

- March 10th 2025
- The updated code's sample request:

```json
{
    "sourceCode": "public class MyClass { public int square(int num) { return num * num; } public String greet(String name) { return \"Hello, \" + name; } }"
}
```

- Response
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

public class MyClassTest {

    private final MyClass instance = new MyClass();

    @Test
    public void testGreet() {
        String result = instance.greet(null);
        assertNotNull(result);
    }

    @Test
    public void testSquare() {
        int result = instance.square(0);
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void testSquareWithParams(int param) {
        int result = instance.square(param);
        assertTrue(result >= 0);
    }

}
```

- March 25th Update
- The new endpoint is ```http://localhost:8080/api/test-generator/ai-generate```
- With this we can see the test cases generated from the OpenAI LLM based on the input provided.

```json
{
  "sourceCode": "public class MyClass { public int square(int num) { return num * num; } public String greet(String name) { return \"Hello, \" + name; } }"
}
```

- Response:

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MyClassTest {

    private MyClass myClass;

    @BeforeEach
    public void setUp() {
        myClass = new MyClass();
    }

    @Test
    public void testSquareWithValidInput() {
        int input = 5;
        int expected = 25;
        int result = myClass.square(input);
        assertEquals(expected, result);
    }

    @Test
    public void testSquareWithNegativeInput() {
        int input = -4;
        int expected = 16;
        int result = myClass.square(input);
        assertEquals(expected, result);
    }

    @Test
    public void testSquareWithZero() {
        int input = 0;
        int expected = 0;
        int result = myClass.square(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGreetWithValidName() {
        String name = "Alice";
        String expected = "Hello, Alice";
        String result = myClass.greet(name);
        assertEquals(expected, result);
    }

    @Test
    public void testGreetWithEmptyName() {
        String name = "";
        String expected = "Hello, ";
        String result = myClass.greet(name);
        assertEquals(expected, result);
    }

    @Test
    public void testGreetWithNullName() {
        String result = myClass.greet(null);
        assertEquals("Hello, null", result);
    }
}
```

- Also if there is a need the API will mock the needed inputs and gives an output class with the mocked service implementation.

- Request

```json
{
    "sourceCode": "public class MyService { private final ExternalApiClient apiClient; public MyService(ExternalApiClient apiClient) { this.apiClient = apiClient; } public String fetchData(String id) { if (id == null || id.isEmpty()) { throw new IllegalArgumentException(\"ID cannot be null or empty\"); } return apiClient.getDataFromApi(id); } }"
}
```

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MyServiceTest {

    @Mock
    private ExternalApiClient apiClient;

    @InjectMocks
    private MyService myService;

    public MyServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFetchDataValidId() {
        String id = "validId";
        String expectedData = "data";
        
        when(apiClient.getDataFromApi(id)).thenReturn(expectedData);

        String result = myService.fetchData(id);

        assertEquals(expectedData, result);
        verify(apiClient, times(1)).getDataFromApi(id);
    }

    @Test
    public void testFetchDataInvalidIdNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            myService.fetchData(null);
        });

        assertEquals("ID cannot be null or empty", exception.getMessage());
        verify(apiClient, never()).getDataFromApi(anyString());
    }

    @Test
    public void testFetchDataInvalidIdEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            myService.fetchData("");
        });

        assertEquals("ID cannot be null or empty", exception.getMessage());
        verify(apiClient, never()).getDataFromApi(anyString());
    }

    @Test
    public void testFetchDataApiClientThrowsException() {
        String id = "validId";

        when(apiClient.getDataFromApi(id)).thenThrow(new RuntimeException("API Error"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            myService.fetchData(id);
        });

        assertEquals("API Error", exception.getMessage());
        verify(apiClient, times(1)).getDataFromApi(id);
    }
}
```