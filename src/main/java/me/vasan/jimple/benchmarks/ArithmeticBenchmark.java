package me.vasan.jimple.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for arithmetic operations comparing interpreter vs compiler performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class ArithmeticBenchmark extends BenchmarkInfrastructure {
    
    private String simpleArithmetic;
    private String complexArithmetic;
    private String intensiveArithmetic;
    
    @Setup
    public void setup() throws Exception {
        // Simple arithmetic operations
        simpleArithmetic = 
            "let a = 10;\n" +
            "let b = 5;\n" +
            "let result = (a + b) * (a - b) / a + b;\n" +
            "result;\n";
        
        // Complex arithmetic with more operations
        complexArithmetic =
            "let x = 15.5;\n" +
            "let y = 3.2;\n" +
            "let z = 7.8;\n" +
            "let result = (x * y + z) / (x - y) + (z * x) - (y / z);\n" +
            "result;\n";
        
        // Computationally intensive arithmetic
        intensiveArithmetic = createIntensiveArithmetic(1000);
    }
    
    @Benchmark
    public void simpleArithmetic_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(simpleArithmetic);
        bh.consume(result);
    }
    
    @Benchmark
    public void simpleArithmetic_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(simpleArithmetic, "SimpleArithmetic");
        bh.consume(result);
    }
    
    @Benchmark
    public void complexArithmetic_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(complexArithmetic);
        bh.consume(result);
    }
    
    @Benchmark
    public void complexArithmetic_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(complexArithmetic, "ComplexArithmetic");
        bh.consume(result);
    }
    
    @Benchmark
    public void intensiveArithmetic_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(intensiveArithmetic);
        bh.consume(result);
    }
    
    @Benchmark
    public void intensiveArithmetic_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(intensiveArithmetic, "IntensiveArithmetic");
        bh.consume(result);
    }
}