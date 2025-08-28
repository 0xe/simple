package me.vasan.jimple.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for function operations comparing interpreter vs compiler performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class FunctionBenchmark extends BenchmarkInfrastructure {
    
    private String simpleFunctionCall;
    private String recursiveFunctionCall;
    private String nestedFunctionCalls;
    private String fibonacciSmall;
    private String fibonacciMedium;
    
    @Setup
    public void setup() throws Exception {
        // Simple function call
        simpleFunctionCall = 
            "let add = function(a, b) {\n" +
            "    return a + b;\n" +
            "};\n" +
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 100) {\n" +
            "    result = add(result, i);\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Recursive function
        recursiveFunctionCall =
            "let factorial = function(n) {\n" +
            "    if (n <= 1) {\n" +
            "        return 1;\n" +
            "    } else {\n" +
            "        return n * factorial(n - 1);\n" +
            "    }\n" +
            "};\n" +
            "factorial(10);\n";
        
        // Nested function calls
        nestedFunctionCalls =
            "let multiply = function(a, b) {\n" +
            "    return a * b;\n" +
            "};\n" +
            "let square = function(x) {\n" +
            "    return multiply(x, x);\n" +
            "};\n" +
            "let sumOfSquares = function(a, b) {\n" +
            "    return square(a) + square(b);\n" +
            "};\n" +
            "let result = 0;\n" +
            "let i = 1;\n" +
            "while (i <= 20) {\n" +
            "    result = result + sumOfSquares(i, i + 1);\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Fibonacci benchmarks
        fibonacciSmall = createFibonacci(10);
        fibonacciMedium = createFibonacci(15);
    }
    
    @Benchmark
    public void simpleFunctionCall_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(simpleFunctionCall);
        bh.consume(result);
    }
    
    @Benchmark
    public void simpleFunctionCall_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(simpleFunctionCall, "SimpleFunction");
        bh.consume(result);
    }
    
    @Benchmark
    public void recursiveFunction_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(recursiveFunctionCall);
        bh.consume(result);
    }
    
    @Benchmark
    public void recursiveFunction_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(recursiveFunctionCall, "RecursiveFunction");
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedFunctionCalls_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(nestedFunctionCalls);
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedFunctionCalls_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(nestedFunctionCalls, "NestedFunctions");
        bh.consume(result);
    }
    
    @Benchmark
    public void fibonacciSmall_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(fibonacciSmall);
        bh.consume(result);
    }
    
    @Benchmark
    public void fibonacciSmall_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(fibonacciSmall, "FibSmall");
        bh.consume(result);
    }
    
    @Benchmark
    public void fibonacciMedium_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(fibonacciMedium);
        bh.consume(result);
    }
    
    @Benchmark
    public void fibonacciMedium_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(fibonacciMedium, "FibMedium");
        bh.consume(result);
    }
}