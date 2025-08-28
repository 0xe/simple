package me.vasan.jimple;

import org.junit.Test;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Unit tests that mirror the tests run by run_tests.sh
 * Each test corresponds to a .sim file in the tests/ directory
 */
public class CompilerTests {
    
    private String runCompilerAndExecute(String simCode, String className) throws Exception {
        String tempFile = className + ".sim";
        String classFile = className + ".class";
        
        // Write code to a temporary file
        Files.write(Paths.get(tempFile), simCode.getBytes());
        
        try {
            // Compile the .sim file
            ProcessBuilder compilePb = new ProcessBuilder(
                "java", "--enable-preview", "-cp", "target/classes:.", 
                "me.vasan.jimple.Jimple", tempFile
            );
            compilePb.directory(new File("."));
            compilePb.redirectErrorStream(true);
            
            Process compileProcess = compilePb.start();
            compileProcess.waitFor();
            
            if (compileProcess.exitValue() != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    throw new RuntimeException("Compilation failed: " + error.toString());
                }
            }
            
            // Check if class file was generated
            if (!Files.exists(Paths.get(classFile))) {
                throw new RuntimeException("Class file not generated: " + classFile);
            }
            
            // Execute the compiled program
            ProcessBuilder runPb = new ProcessBuilder("java", "--enable-preview", "-cp", "target/classes:.:tests", className);
            runPb.directory(new File("."));
            runPb.redirectErrorStream(true);
            
            Process runProcess = runPb.start();
            runProcess.waitFor();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString().trim();
            }
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(Paths.get(tempFile));
            Files.deleteIfExists(Paths.get(classFile));
        }
    }
    
    @Test
    public void testArithmetic() throws Exception {
        String code = """
            // Test basic arithmetic operations
            let a = 10;
            let b = 5;

            let sum = a + b;
            let diff = a - b;
            let prod = a * b;
            let quot = a / b;

            print("Sum: ", sum);
            print("Diff: ", diff);
            print("Product: ", prod);
            print("Quotient: ", quot);

            // Test unary operations
//            let neg = -a;
//            print("Negative: ", neg);

            // Test compound expressions
            let complex = (a + b) * (a - b);
            print("Complex: ", complex);
            """;
        
        String output = runCompilerAndExecute(code, "TestArithmetic");
        Assert.assertTrue("Should contain sum result", output.contains("Sum: 15"));
        Assert.assertTrue("Should contain diff result", output.contains("Diff: 5"));
        Assert.assertTrue("Should contain product result", output.contains("Product: 50"));
        Assert.assertTrue("Should contain quotient result", output.contains("Quotient: 2"));
        // TODO: unary -ve is broken in the compiler
//        Assert.assertTrue("Should contain negative result", output.contains("Negative: -10"));
        Assert.assertTrue("Should contain complex result", output.contains("Complex: 75"));
    }
    
    @Test
    public void testVariables() throws Exception {
        String code = """
            // Test variable declarations and assignments
            let x = 42;
            let y = 3.14;
            let name = "Simple Language";
            let flag = true;

            print("Number: ", x);
            print("Float: ", y);
            print("String: ", name);
            print("Boolean: ", flag);

            // Test variable reassignment
            x = x + 10;
            flag = false;

            print("Updated number: ", x);
            print("Updated boolean: ", flag);
            """;
        
        String output = runCompilerAndExecute(code, "TestVariables");
        Assert.assertTrue("Should contain initial number", output.contains("Number: 42"));
        Assert.assertTrue("Should contain float", output.contains("Float: 3.14"));
        Assert.assertTrue("Should contain string", output.contains("String: Simple Language"));
        Assert.assertTrue("Should contain initial boolean", output.contains("Boolean: true"));
        Assert.assertTrue("Should contain updated number", output.contains("Updated number: 52"));
        Assert.assertTrue("Should contain updated boolean", output.contains("Updated boolean: false"));
    }
    
    @Test
    public void testComparisons() throws Exception {
        String code = """
            // Test comparison operations
            let a = 10;
            let b = 5;

            print("10 == 5: ", a == b);
            print("10 != 5: ", a != b);
            print("10 > 5: ", a > b);
            print("10 >= 5: ", a >= b);
            print("10 < 5: ", a < b);
            print("10 <= 5: ", a <= b);

            // Test boolean operations
            let t = true;
            let f = false;

            print("true && false: ", t && f);
            print("true || false: ", t || f);
            print("!true: ", !t);
            """;
        
        String output = runCompilerAndExecute(code, "TestComparisons");
        Assert.assertTrue("Should contain equality comparison", output.contains("10 == 5: false"));
        Assert.assertTrue("Should contain inequality comparison", output.contains("10 != 5: true"));
        Assert.assertTrue("Should contain greater than", output.contains("10 > 5: true"));
        Assert.assertTrue("Should contain greater than or equal", output.contains("10 >= 5: true"));
        Assert.assertTrue("Should contain less than", output.contains("10 < 5: false"));
        Assert.assertTrue("Should contain less than or equal", output.contains("10 <= 5: false"));
        Assert.assertTrue("Should contain logical AND", output.contains("true && false: false"));
        Assert.assertTrue("Should contain logical OR", output.contains("true || false: true"));
        Assert.assertTrue("Should contain logical NOT", output.contains("!true: false"));
    }
    
    @Test
    public void testIfStatements() throws Exception {
        String code = """
            // Test if statements
            let x = 10;

            if (x > 5) {
                print("x is greater than 5");
            }

            if (x < 5) {
                print("x is less than 5");
            } else {
                print("x is not less than 5");
            }

            // Test nested if
            if (x > 0) {
                if (x > 5) {
                    print("x is positive and greater than 5");
                } else {
                    print("x is positive but not greater than 5");
                }
            } else {
                print("x is not positive");
            }

            // Test if with complex conditions
            let y = 3;
            if (x > 5 && y < 5) {
                print("Both conditions are true");
            }
            """;
        
        String output = runCompilerAndExecute(code, "TestIfStatements");
        Assert.assertTrue("Should contain first if result", output.contains("x is greater than 5"));
        Assert.assertTrue("Should contain else result", output.contains("x is not less than 5"));
        Assert.assertTrue("Should contain nested if result", output.contains("x is positive and greater than 5"));
        Assert.assertTrue("Should contain complex condition result", output.contains("Both conditions are true"));
    }
    
    @Test
    public void testWhileLoops() throws Exception {
        String code = """
            // Test while loops
            let count = 0;

            print("Counting to 3:");
            while (count < 3) {
                print("Count: ", count);
                count = count + 1;
            }

            // Test while with more complex condition
            let x = 10;
            let y = 1;

            print("Decreasing x while y increases:");
            while (x > y) {
                print("x: ", x, ", y: ", y);
                x = x - 1;
                y = y + 1;
            }

            print("Final values - x: ", x, ", y: ", y);
            """;
        
        String output = runCompilerAndExecute(code, "TestWhileLoops");
        Assert.assertTrue("Should contain counting header", output.contains("Counting to 3:"));
        Assert.assertTrue("Should contain count 0", output.contains("Count: 0"));
        Assert.assertTrue("Should contain count 1", output.contains("Count: 1"));
        Assert.assertTrue("Should contain count 2", output.contains("Count: 2"));
        Assert.assertTrue("Should contain decreasing header", output.contains("Decreasing x while y increases:"));
        Assert.assertTrue("Should contain final values", output.contains("Final values"));
    }
    
    @Test
    public void testBlocks() throws Exception {
        String code = """
            // Test block scoping
            let outer = "outer variable";

            print("Before block: ", outer);

            {
                let inner = "inner variable";
                let outer = "shadowed outer";  // shadows outer variable
                
                print("In block - outer: ", outer);
                print("In block - inner: ", inner);
            }

            print("After block: ", outer);

            // Test nested blocks
            {
                let level1 = "level 1";
                {
                    let level2 = "level 2";
                    print("Nested block - level1: ", level1);
                    print("Nested block - level2: ", level2);
                }
            }
            """;
        
        String output = runCompilerAndExecute(code, "TestBlocks");
        Assert.assertTrue("Should contain before block", output.contains("Before block: outer variable"));
        Assert.assertTrue("Should contain shadowed outer", output.contains("In block - outer: shadowed outer"));
        Assert.assertTrue("Should contain inner variable", output.contains("In block - inner: inner variable"));
        Assert.assertTrue("Should contain after block", output.contains("After block: outer variable"));
        Assert.assertTrue("Should contain nested level1", output.contains("Nested block - level1: level 1"));
        Assert.assertTrue("Should contain nested level2", output.contains("Nested block - level2: level 2"));
    }
    
    @Test
    public void testFunctions() throws Exception {
        String code = """
            // Test function definitions and calls
            let add = function(a, b) {
                return a + b;
            };

            let multiply = function(x, y) {
                return x * y;
            };

            // Test simple function calls
            let sum = add(5, 3);
            let product = multiply(4, 6);

            print("5 + 3 = ", sum);
            print("4 * 6 = ", product);

            // Test function with conditional logic
            let max = function(a, b) {
                if (a > b) {
                    return a;
                } else {
                    return b;
                }
            };

            print("max(10, 7) = ", max(10, 7));
            print("max(3, 8) = ", max(3, 8));

            // Test recursive function
            let factorial = function(n) {
                if (n <= 1) {
                    return 1;
                } else {
                    return n * factorial(n - 1);
                }
            };

            print("factorial(5) = ", factorial(5));
            """;
        
        String output = runCompilerAndExecute(code, "TestFunctions");
        Assert.assertTrue("Should contain addition result", output.contains("5 + 3 = 8"));
        Assert.assertTrue("Should contain multiplication result", output.contains("4 * 6 = 24"));
        Assert.assertTrue("Should contain max(10, 7)", output.contains("max(10, 7) = 10"));
        Assert.assertTrue("Should contain max(3, 8)", output.contains("max(3, 8) = 8"));
        Assert.assertTrue("Should contain factorial result", output.contains("factorial(5) = 120"));
    }
    
    @Test
    public void testFunctionScopes() throws Exception {
        String code = """
            // Test function scoping and closures
            let global = "global variable";

            let outerFunc = function() {
                let outer = "outer function variable";
                
                let innerFunc = function() {
                    print("From inner: ", global);
                    print("From inner: ", outer);
                    return "inner result";
                };
                
                return innerFunc();
            };

            let result = outerFunc();
            print("Result: ", result);

            // Test function parameters vs local variables
            let test = function(param) {
                let local = "local var";
                param = param + " modified";
                print("Parameter: ", param);
                print("Local: ", local);
                return param;
            };

            let original = "original value";
            let modified = test(original);
            print("Original unchanged: ", original);
            print("Modified: ", modified);
            """;
        
        String output = runCompilerAndExecute(code, "TestFunctionScopes");
        Assert.assertTrue("Should access global from inner", output.contains("From inner: global variable"));
        Assert.assertTrue("Should access outer from inner", output.contains("From inner: outer function variable"));
        Assert.assertTrue("Should contain inner result", output.contains("Result: inner result"));
        Assert.assertTrue("Should contain modified parameter", output.contains("Parameter: original value modified"));
        Assert.assertTrue("Should contain local variable", output.contains("Local: local var"));
        Assert.assertTrue("Should keep original unchanged", output.contains("Original unchanged: original value"));
        Assert.assertTrue("Should contain modified result", output.contains("Modified: original value modified"));
    }
    
    @Test
    public void testObjects() throws Exception {
        String code = """
            // Test object creation and property access
            let person = {name: "Alice", age: 30};

            print("Name: ", person.name);
            print("Age: ", person.age);

            // Test object with mixed types
            let mixed = {
                number: 42,
                string: "hello",
                boolean: true
            };

            print("Number property: ", mixed.number);
            print("String property: ", mixed.string);
            print("Boolean property: ", mixed.boolean);

            // Test nested objects
            let company = {
                name: "Tech Corp",
                ceo: {
                    name: "Bob",
                    age: 45
                }
            };

            print("Company: ", company.name);
            print("CEO: ", company.ceo.name);
            print("CEO Age: ", company.ceo.age);
            """;
        
        String output = runCompilerAndExecute(code, "TestObjects");
        Assert.assertTrue("Should contain person name", output.contains("Name: Alice"));
        Assert.assertTrue("Should contain person age", output.contains("Age: 30"));
        Assert.assertTrue("Should contain number property", output.contains("Number property: 42"));
        Assert.assertTrue("Should contain string property", output.contains("String property: hello"));
        Assert.assertTrue("Should contain boolean property", output.contains("Boolean property: true"));
        Assert.assertTrue("Should contain company name", output.contains("Company: Tech Corp"));
        Assert.assertTrue("Should contain CEO name", output.contains("CEO: Bob"));
        Assert.assertTrue("Should contain CEO age", output.contains("CEO Age: 45"));
    }
    
    @Test
    public void testObjectMethods() throws Exception {
        String code = """
            // Test objects with function properties (methods)
            let calculator = {
                value: 0,
                add: function(x) {
                    return calculator.value + x;
                },
                multiply: function(x) {
                    return calculator.value * x;
                }
            };

            calculator.value = 10;
            print("Initial value: ", calculator.value);
            print("Add 5: ", calculator.add(5));
            print("Multiply by 3: ", calculator.multiply(3));

            // Test object with complex methods
            let counter = {
                count: 0,
                increment: function() {
                    counter.count = counter.count + 1;
                    return counter.count;
                },
                reset: function() {
                    counter.count = 0;
                    return counter.count;
                }
            };

            print("Count: ", counter.count);
            print("Increment: ", counter.increment());
            print("Increment: ", counter.increment());
            print("Reset: ", counter.reset());
            """;
        
        String output = runCompilerAndExecute(code, "TestObjectMethods");
        Assert.assertTrue("Should contain initial value", output.contains("Initial value: 10"));
        Assert.assertTrue("Should contain add result", output.contains("Add 5: 15"));
        Assert.assertTrue("Should contain multiply result", output.contains("Multiply by 3: 30"));
        Assert.assertTrue("Should contain initial count", output.contains("Count: 0"));
        Assert.assertTrue("Should contain first increment", output.contains("Increment: 1"));
        Assert.assertTrue("Should contain second increment", output.contains("Increment: 2"));
        Assert.assertTrue("Should contain reset result", output.contains("Reset: 0"));
    }
}