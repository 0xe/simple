package me.vasan.jimple;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class JimpleTestHarness {
    
    private final String testFile;
    private final String testName;
    
    public JimpleTestHarness(String testFile) {
        this.testFile = testFile;
        this.testName = testFile.replace(".sim", "");
    }
    
    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> testFiles() {
        return Arrays.asList(
            "test_arithmetic.sim",
            "test_variables.sim", 
            "test_comparisons.sim",
            "test_if_statements.sim",
            "test_while_loops.sim",
            "test_blocks.sim",
            "test_functions.sim",
            "test_function_scopes.sim",
            "test_objects.sim",
            "test_object_methods.sim",
            "test_basic.sim",
            "test_simple.sim",
            "test_print.sim",
            "test_arithmetic_simple.sim"
        );
    }
    
    @Test
    public void testInterpreter() throws Exception {
        String testFilePath = "tests/" + testFile;
        if (!Files.exists(Paths.get(testFilePath))) {
            System.out.println("Skipping test " + testName + " - file not found: " + testFilePath);
            return;
        }
        
        System.out.println("Running interpreter test: " + testName);
        
        ProcessBuilder pb = new ProcessBuilder(
            "java", "--enable-preview", "-cp", ".", 
            "me.vasan.jimple.Jimple", "-i", testFilePath
        );
        pb.directory(new File("."));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            Assert.fail("Interpreter test " + testName + " timed out");
        }
        
        String output = readProcessOutput(process);
        int exitCode = process.exitValue();
        
        if (exitCode != 0) {
            System.out.println("Interpreter output for " + testName + ":");
            System.out.println(output);
            Assert.fail("Interpreter test " + testName + " failed with exit code " + exitCode);
        }
        
        System.out.println("Interpreter test " + testName + " passed");
    }
    
    @Test
    public void testCompiler() throws Exception {
        String testFilePath = "tests/" + testFile;
        if (!Files.exists(Paths.get(testFilePath))) {
            System.out.println("Skipping test " + testName + " - file not found: " + testFilePath);
            return;
        }
        
        System.out.println("Running compiler test: " + testName);
        
        // First compile the .sim file
        ProcessBuilder compilePb = new ProcessBuilder(
            "java", "--enable-preview", "-cp", ".", 
            "me.vasan.jimple.Jimple", testFilePath
        );
        compilePb.directory(new File("."));
        compilePb.redirectErrorStream(true);
        
        Process compileProcess = compilePb.start();
        boolean compileFinished = compileProcess.waitFor(10, TimeUnit.SECONDS);
        
        if (!compileFinished) {
            compileProcess.destroyForcibly();
            Assert.fail("Compiler test " + testName + " compilation timed out");
        }
        
        String compileOutput = readProcessOutput(compileProcess);
        int compileExitCode = compileProcess.exitValue();
        
        if (compileExitCode != 0) {
            System.out.println("Compilation output for " + testName + ":");
            System.out.println(compileOutput);
            Assert.fail("Compiler test " + testName + " compilation failed with exit code " + compileExitCode);
        }
        
        // Check if class file was generated
        String classFilePath = "tests/" + testName + ".class";
        if (!Files.exists(Paths.get(classFilePath))) {
            Assert.fail("Compiler test " + testName + " - class file not generated: " + classFilePath);
        }
        
        // Run the compiled program
        ProcessBuilder runPb = new ProcessBuilder(
            "java", "-cp", ".:tests", testName
        );
        runPb.directory(new File("."));
        runPb.redirectErrorStream(true);
        
        Process runProcess = runPb.start();
        boolean runFinished = runProcess.waitFor(10, TimeUnit.SECONDS);
        
        if (!runFinished) {
            runProcess.destroyForcibly();
            Assert.fail("Compiler test " + testName + " execution timed out");
        }
        
        String runOutput = readProcessOutput(runProcess);
        int runExitCode = runProcess.exitValue();
        
        // Clean up generated class file
        try {
            Files.deleteIfExists(Paths.get(classFilePath));
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        if (runExitCode != 0) {
            System.out.println("Execution output for " + testName + ":");
            System.out.println(runOutput);
            Assert.fail("Compiler test " + testName + " execution failed with exit code " + runExitCode);
        }
        
        System.out.println("Compiler test " + testName + " passed");
    }
    
    private String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }
}