package me.vasan.jimple;

import org.junit.Test;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Integration tests for the Jimple interpreter
 * These tests can be run individually for debugging specific language features
 */
public class InterpreterIntegrationTest {
    
    private String runInterpreter(String simCode) throws Exception {
        // Write code to a temporary file
        String tempFile = "temp_test.sim";
        Files.write(Paths.get(tempFile), simCode.getBytes());
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "--enable-preview", "-cp", "target/classes", 
                "me.vasan.jimple.Jimple", "-i", tempFile
            );
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            process.waitFor();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString().trim();
            }
        } finally {
            Files.deleteIfExists(Paths.get(tempFile));
        }
    }
    
    @Test
    public void testBasicArithmetic() throws Exception {
        String code = """
            let a = 2 + 3;
            let b = 10 - 4;
            let c = 6 * 7;
            let d = 15 / 3;
            print("Sum: ", a);
            print("Diff: ", b);
            print("Product: ", c);
            print("Quotient: ", d);
            """;
        
        String output = runInterpreter(code);
        System.out.println("Arithmetic output: " + output);
        Assert.assertTrue("Output should contain arithmetic results", 
                         output.contains("5") && output.contains("6") && 
                         output.contains("42") && output.contains("5"));
    }
    
    @Test
    public void testVariables() throws Exception {
        String code = """
            let x = 10;
            let y = 20;
            let sum = x + y;
            print("Sum: ", sum);
            x = x * 2;
            print("Modified x: ", x);
            """;
        
        String output = runInterpreter(code);
        System.out.println("Variables output: " + output);
        Assert.assertTrue("Output should contain variable results", 
                         output.contains("30") && output.contains("20"));
    }
    
    @Test
    public void testConditions() throws Exception {
        String code = """
            let x = 5;
            if (x > 3) {
                print("greater");
            } else {
                print("lesser");
            }
            
            if (x < 3) {
                print("small");
            } else {
                print("big");
            }
            """;
        
        String output = runInterpreter(code);
        System.out.println("Conditions output: " + output);
        Assert.assertTrue("Output should contain condition results", 
                         output.contains("greater") && output.contains("big"));
    }
    
    @Test
    public void testFunctions() throws Exception {
        String code = """
            let add = function(a, b) {
                return a + b;
            };
            
            let result = add(10, 20);
            print("Result: ", result);
            """;
        
        String output = runInterpreter(code);
        System.out.println("Functions output: " + output);
        Assert.assertTrue("Output should contain function result", output.contains("30"));
    }
    
    @Test
    public void testObjects() throws Exception {
        String code = """
            let obj = {x: 10, y: 20, z: 30};
            print("X: ", obj.x);
            print("Y: ", obj.y);
            print("Z: ", obj.z);
            """;
        
        String output = runInterpreter(code);
        System.out.println("Objects output: " + output);
        Assert.assertTrue("Output should contain object property values", 
                         output.contains("10") && output.contains("20") && output.contains("30"));
    }
    
    @Test 
    public void testWhileLoops() throws Exception {
        String code = """
            let i = 0;
            while (i < 3) {
                print("Count: ", i);
                i = i + 1;
            }
            """;
        
        String output = runInterpreter(code);
        System.out.println("While loops output: " + output);
        Assert.assertTrue("Output should contain loop iterations", 
                         output.contains("0") && output.contains("1") && output.contains("2"));
    }
}