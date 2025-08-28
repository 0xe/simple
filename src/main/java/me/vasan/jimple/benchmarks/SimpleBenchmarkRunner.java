package me.vasan.jimple.benchmarks;

/**
 * Simple benchmark runner for testing the benchmark infrastructure
 * This is a basic version that doesn't use full JMH annotation processing
 */
public class SimpleBenchmarkRunner extends BenchmarkInfrastructure {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Simple Language Benchmark Infrastructure Test");
        System.out.println("============================================");
        
        // Test simple arithmetic
        String simpleArithmetic = 
            "let a = 10;\n" +
            "let b = 5;\n" +
            "let result = (a + b) * (a - b) / a + b;\n" +
            "result;\n";
        
        System.out.println("Testing simple arithmetic...");
        
        // Test interpreter
        System.out.print("Interpreter: ");
        long startTime = System.nanoTime();
        try {
            Object interpResult = runInterpreter(simpleArithmetic);
            long interpTime = System.nanoTime() - startTime;
            System.out.println("OK (" + interpTime/1000000.0 + " ms)");
        } catch (Exception e) {
            System.out.println("FAILED - " + e.getMessage());
        }
        
        // Test compiler
        System.out.print("Compiler: ");
        startTime = System.nanoTime();
        try {
            Object compilerResult = runCompiler(simpleArithmetic, "TestArithmetic");
            long compilerTime = System.nanoTime() - startTime;
            System.out.println("OK (" + compilerTime/1000000.0 + " ms)");
        } catch (Exception e) {
            System.out.println("FAILED - " + e.getMessage());
        }
        
        // Test function calls
        String functionTest =
            "let add = function(a, b) {\n" +
            "    return a + b;\n" +
            "};\n" +
            "let result = add(5, 3);\n" +
            "result;\n";
        
        System.out.println("\nTesting function calls...");
        
        // Test interpreter
        System.out.print("Interpreter: ");
        startTime = System.nanoTime();
        try {
            Object interpResult = runInterpreter(functionTest);
            long interpTime = System.nanoTime() - startTime;
            System.out.println("OK (" + interpTime/1000000.0 + " ms)");
        } catch (Exception e) {
            System.out.println("FAILED - " + e.getMessage());
        }
        
        // Test compiler  
        System.out.print("Compiler: ");
        startTime = System.nanoTime();
        try {
            Object compilerResult = runCompiler(functionTest, "TestFunction");
            long compilerTime = System.nanoTime() - startTime;
            System.out.println("OK (" + compilerTime/1000000.0 + " ms)");
        } catch (Exception e) {
            System.out.println("FAILED - " + e.getMessage());
        }
        
        System.out.println("\nBenchmark infrastructure test completed!");
        System.out.println("\nTo run full JMH benchmarks:");
        System.out.println("1. First generate JMH sources: mvn clean compile");
        System.out.println("2. Then run: ./run_benchmarks.sh");
    }
}