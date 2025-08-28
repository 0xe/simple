#!/bin/bash

# Custom Benchmark Test Runner
# Tests various benchmark scenarios using our custom infrastructure

echo "Simple Language Custom Benchmark Tests"
echo "======================================"

# Ensure we're compiled
mvn compile -q

echo "Running performance comparison tests..."
echo

# Create a test class for performance testing
cat > CustomBenchmarkTest.java << 'EOF'
import me.vasan.jimple.benchmarks.BenchmarkInfrastructure;

public class CustomBenchmarkTest extends BenchmarkInfrastructure {
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("=== Arithmetic Performance Comparison ===");
        
        // Test 1: Simple arithmetic
        String simpleArithmetic = 
            "let a = 10; let b = 5; let result = (a + b) * (a - b) / a + b; result;";
        
        System.out.println("Test: Simple arithmetic");
        long interpTime = timeTest(() -> runInterpreter(simpleArithmetic), 10);
        long compilerTime = timeTest(() -> runCompiler(simpleArithmetic, "TestSimple"), 10);
        System.out.printf("Interpreter: %d ms, Compiler: %d ms (%.2fx)\n", 
            interpTime, compilerTime, (double)interpTime/compilerTime);
        
        System.out.println("\n=== Function Performance Comparison ===");
        
        // Test 2: Function calls
        String functionTest =
            "let add = function(a, b) { return a + b; }; " +
            "let result = 0; let i = 0; " +
            "while (i < 50) { result = add(result, i); i = i + 1; } result;";
        
        System.out.println("Test: Function calls in loop");
        interpTime = timeTest(() -> runInterpreter(functionTest), 5);
        compilerTime = timeTest(() -> runCompiler(functionTest, "TestFunc"), 5);
        System.out.printf("Interpreter: %d ms, Compiler: %d ms (%.2fx)\n", 
            interpTime, compilerTime, (double)interpTime/compilerTime);
        
        System.out.println("\n=== Object Performance Comparison ===");
        
        // Test 3: Object operations
        String objectTest =
            "let obj = {x: 1, y: 2}; " +
            "let i = 0; while (i < 100) { obj.x = obj.x + 1; obj.y = obj.y + 2; i = i + 1; } " +
            "obj.x + obj.y;";
        
        System.out.println("Test: Object property manipulation");
        interpTime = timeTest(() -> runInterpreter(objectTest), 5);
        compilerTime = timeTest(() -> runCompiler(objectTest, "TestObj"), 5);
        System.out.printf("Interpreter: %d ms, Compiler: %d ms (%.2fx)\n", 
            interpTime, compilerTime, (double)interpTime/compilerTime);
            
        System.out.println("\n=== Performance Summary ===");
        System.out.println("✅ All performance tests completed successfully!");
        System.out.println("Note: Results show relative performance between interpreter and compiler.");
        System.out.println("Lower times = better performance. Ratio > 1.0 means compiler is faster.");
    }
    
    private static long timeTest(TestRunner test, int iterations) throws Exception {
        long totalTime = 0;
        
        // Warmup
        for (int i = 0; i < 2; i++) {
            test.run();
        }
        
        // Measure
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            test.run();
            totalTime += System.nanoTime() - start;
        }
        
        return totalTime / (iterations * 1000000); // Convert to milliseconds
    }
    
    @FunctionalInterface
    private interface TestRunner {
        Object run() throws Exception;
    }
}
EOF

# Compile and run the test
javac --enable-preview --source 24 -cp target/classes CustomBenchmarkTest.java
java --enable-preview -cp target/classes:. CustomBenchmarkTest

# Cleanup
rm -f CustomBenchmarkTest.java CustomBenchmarkTest.class TestSimple.class TestFunc.class TestObj.class

echo
echo "Custom benchmark tests completed!"