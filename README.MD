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
- Open postman application and create a new request using the URL : ```http://localhost:8080/api/generate-tests``` with ```POST``` Method.
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