package me.vasan.jimple.benchmarks;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.results.format.ResultFormatType;

/**
 * Main runner for Simple language benchmarks
 * Runs all benchmark suites and generates comprehensive reports
 */
public class BenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException {
        
        String benchmarkPattern = ".*";
        if (args.length > 0) {
            benchmarkPattern = args[0];
        }
        
        System.out.println("Simple Language Performance Benchmarks");
        System.out.println("=====================================");
        System.out.println("Comparing Interpreter vs Compiler performance across language features");
        System.out.println();
        
        Options opt = new OptionsBuilder()
            .include(benchmarkPattern)
            .forks(2)
            .warmupIterations(3)
            .measurementIterations(5)
            .resultFormat(ResultFormatType.TEXT)
            .result("benchmark_results.txt")
            .build();
        
        new Runner(opt).run();
        
        System.out.println();
        System.out.println("Benchmarks completed! Results saved to benchmark_results.txt");
        System.out.println();
        System.out.println("Available benchmark suites:");
        System.out.println("- ArithmeticBenchmark: Basic arithmetic operations");
        System.out.println("- FunctionBenchmark: Function calls and recursion");
        System.out.println("- ObjectBenchmark: Object creation and property access");  
        System.out.println("- ControlFlowBenchmark: If/else statements and loops");
        System.out.println();
        System.out.println("To run specific benchmarks:");
        System.out.println("  java -cp target/classes BenchmarkRunner \".*Arithmetic.*\"");
        System.out.println("  java -cp target/classes BenchmarkRunner \".*Function.*\"");
        System.out.println("  java -cp target/classes BenchmarkRunner \".*Object.*\"");
        System.out.println("  java -cp target/classes BenchmarkRunner \".*ControlFlow.*\"");
    }
}