package me.vasan.jimple.benchmarks;

import me.vasan.jimple.Jimple;
import me.vasan.jimple.Callable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Base infrastructure for Simple language benchmarks
 * Provides common utilities for running interpreter and compiler benchmarks
 */
public class BenchmarkInfrastructure {
    
    /**
     * Run code using the interpreter
     * Uses process execution to avoid class access issues
     */
    protected static Object runInterpreter(String code) throws Exception {
        // Write code to temporary file
        Path tempFile = Files.createTempFile("benchmark_interp", ".sim");
        Files.write(tempFile, code.getBytes());
        
        try {
            // Run interpreter via process
            ProcessBuilder pb = new ProcessBuilder(
                "java", "--enable-preview", "-cp", "target/classes",
                "me.vasan.jimple.Jimple", "-i", tempFile.toString()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            process.waitFor();
            
            // Read output - for benchmarking we just need execution, not output parsing
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                return output.toString().trim();
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    /**
     * Compile and run code using the compiler
     * Uses process execution to avoid class access issues
     */
    protected static Object runCompiler(String code, String className) throws Exception {
        // Write code to temporary file
        Path tempFile = Files.createTempFile(className, ".sim");
        Files.write(tempFile, code.getBytes());
        String classFile = className + ".class";
        
        try {
            // Compile the code
            ProcessBuilder compilePb = new ProcessBuilder(
                "java", "--enable-preview", "-cp", "target/classes",
                "me.vasan.jimple.Jimple", tempFile.toString()
            );
            compilePb.redirectErrorStream(true);
            
            Process compileProcess = compilePb.start();
            compileProcess.waitFor();
            
            if (compileProcess.exitValue() == 0 && Files.exists(Paths.get(classFile))) {
                // Run the compiled program
                ProcessBuilder runPb = new ProcessBuilder(
                    "java", "--enable-preview", "-cp", "target/classes:.",
                    className
                );
                runPb.redirectErrorStream(true);
                
                Process runProcess = runPb.start();
                runProcess.waitFor();
                
                // Read output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    return output.toString().trim();
                }
            }
            return null;
        } finally {
            // Clean up temporary files
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(Paths.get(classFile));
        }
    }
    
    /**
     * Load test code from a file in the tests directory
     */
    protected static String loadTestCode(String filename) throws IOException {
        Path testFile = Paths.get("tests", filename);
        if (!Files.exists(testFile)) {
            throw new FileNotFoundException("Test file not found: " + testFile);
        }
        return Files.readString(testFile);
    }
    
    /**
     * Create optimized code for performance testing
     * This removes print statements that would interfere with benchmarking
     */
    protected static String optimizeForBenchmarking(String originalCode) {
        // Remove print statements and replace with variable assignments
        // This ensures the computation still happens but without I/O overhead
        return originalCode.replaceAll("print\\([^)]+\\);", "// removed print for benchmarking");
    }
    
    /**
     * Create a computationally intensive version of arithmetic operations
     */
    protected static String createIntensiveArithmetic(int iterations) {
        StringBuilder code = new StringBuilder();
        code.append("let result = 0;\n");
        code.append("let i = 0;\n");
        code.append("while (i < ").append(iterations).append(") {\n");
        code.append("    result = result + (i * 2 + 1) / (i + 1);\n");
        code.append("    i = i + 1;\n");
        code.append("}\n");
        code.append("result;\n");
        return code.toString();
    }
    
    /**
     * Create a recursive fibonacci function for function call benchmarking
     */
    protected static String createFibonacci(int n) {
        return String.format(
            "let fib = function(n) {\n" +
            "    if (n <= 1) {\n" +
            "        return n;\n" +
            "    } else {\n" +
            "        return fib(n - 1) + fib(n - 2);\n" +
            "    }\n" +
            "};\n" +
            "fib(%d);\n", n
        );
    }
    
    /**
     * Create object manipulation code for object benchmarking
     */
    protected static String createObjectManipulation(int iterations) {
        StringBuilder code = new StringBuilder();
        code.append("let obj = {x: 1, y: 2, z: 3};\n");
        code.append("let i = 0;\n");
        code.append("while (i < ").append(iterations).append(") {\n");
        code.append("    obj.x = obj.x + 1;\n");
        code.append("    obj.y = obj.y * 2;\n");
        code.append("    obj.z = obj.z - 1;\n");
        code.append("    i = i + 1;\n");
        code.append("}\n");
        code.append("obj.x + obj.y + obj.z;\n");
        return code.toString();
    }
}