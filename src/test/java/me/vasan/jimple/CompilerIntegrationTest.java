package me.vasan.jimple;

import org.junit.Test;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Integration tests for the Jimple compiler
 * These tests can be run individually for debugging specific compilation features
 */
public class CompilerIntegrationTest {
    
    private String runCompilerAndExecute(String simCode, String className) throws Exception {
        String tempFile = className + ".sim";
        String classFile = className + ".class";
        
        // Write code to a temporary file
        Files.write(Paths.get(tempFile), simCode.getBytes());
        
        try {
            // Compile the .sim file
            ProcessBuilder compilePb = new ProcessBuilder(
                "java", "--enable-preview", "-cp", ".", 
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
            ProcessBuilder runPb = new ProcessBuilder("java", "-cp", ".", className);
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
    public void testBasicArithmeticCompilation() throws Exception {
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
        
        String output = runCompilerAndExecute(code, "TestArithmetic");
        System.out.println("Compiled arithmetic output: " + output);
        Assert.assertTrue("Compiled output should contain arithmetic results", 
                         output.contains("5") && output.contains("6") && 
                         output.contains("42") && output.contains("5"));
    }
    
    @Test
    public void testVariablesCompilation() throws Exception {
        String code = """
            let x = 10;
            let y = 20;
            let sum = x + y;
            print("Sum: ", sum);
            x = x * 2;
            print("Modified x: ", x);
            """;
        
        String output = runCompilerAndExecute(code, "TestVariables");
        System.out.println("Compiled variables output: " + output);
        Assert.assertTrue("Compiled output should contain variable results", 
                         output.contains("30") && output.contains("20"));
    }
    
    @Test
    public void testConditionsCompilation() throws Exception {
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
        
        String output = runCompilerAndExecute(code, "TestConditions");
        System.out.println("Compiled conditions output: " + output);
        Assert.assertTrue("Compiled output should contain condition results", 
                         output.contains("greater") && output.contains("big"));
    }
    
    @Test
    public void testFunctionsCompilation() throws Exception {
        String code = """
            let add = function(a, b) {
                return a + b;
            };
            
            let result = add(10, 20);
            print("Result: ", result);
            """;
        
        String output = runCompilerAndExecute(code, "TestFunctions");
        System.out.println("Compiled functions output: " + output);
        Assert.assertTrue("Compiled output should contain function result", output.contains("30"));
    }
    
    @Test
    public void testObjectsCompilation() throws Exception {
        String code = """
            let obj = {x: 10, y: 20, z: 30};
            print("X: ", obj.x);
            print("Y: ", obj.y);
            print("Z: ", obj.z);
            """;
        
        String output = runCompilerAndExecute(code, "TestObjects");
        System.out.println("Compiled objects output: " + output);
        Assert.assertTrue("Compiled output should contain object property values", 
                         output.contains("10") && output.contains("20") && output.contains("30"));
    }
    
    @Test 
    public void testWhileLoopsCompilation() throws Exception {
        String code = """
            let i = 0;
            while (i < 3) {
                print("Count: ", i);
                i = i + 1;
            }
            """;
        
        String output = runCompilerAndExecute(code, "TestWhileLoops");
        System.out.println("Compiled while loops output: " + output);
        Assert.assertTrue("Compiled output should contain loop iterations", 
                         output.contains("0") && output.contains("1") && output.contains("2"));
    }
}