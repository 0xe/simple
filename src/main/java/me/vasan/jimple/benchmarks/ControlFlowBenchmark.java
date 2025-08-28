package me.vasan.jimple.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for control flow operations comparing interpreter vs compiler performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class ControlFlowBenchmark extends BenchmarkInfrastructure {
    
    private String simpleIfElse;
    private String nestedIfElse;
    private String whileLoop;
    private String nestedLoops;
    private String conditionalLogic;
    
    @Setup
    public void setup() throws Exception {
        // Simple if-else statements
        simpleIfElse = 
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 1000) {\n" +
            "    if (i % 2 == 0) {\n" +
            "        result = result + i;\n" +
            "    } else {\n" +
            "        result = result - i;\n" +
            "    }\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Nested if-else statements
        nestedIfElse =
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 500) {\n" +
            "    if (i % 3 == 0) {\n" +
            "        if (i % 2 == 0) {\n" +
            "            result = result + i * 2;\n" +
            "        } else {\n" +
            "            result = result + i;\n" +
            "        }\n" +
            "    } else {\n" +
            "        if (i % 5 == 0) {\n" +
            "            result = result - i;\n" +
            "        } else {\n" +
            "            result = result + i / 2;\n" +
            "        }\n" +
            "    }\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // While loop intensive
        whileLoop =
            "let result = 1;\n" +
            "let i = 1;\n" +
            "while (i <= 1000) {\n" +
            "    result = result + i * (i + 1) / 2;\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Nested loops
        nestedLoops =
            "let result = 0;\n" +
            "let i = 1;\n" +
            "while (i <= 50) {\n" +
            "    let j = 1;\n" +
            "    while (j <= 20) {\n" +
            "        result = result + i * j;\n" +
            "        j = j + 1;\n" +
            "    }\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Complex conditional logic
        conditionalLogic =
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 1000) {\n" +
            "    if (i > 100 && i < 200) {\n" +
            "        result = result + i * 2;\n" +
            "    } else if (i >= 200 && i < 500) {\n" +
            "        result = result + i;\n" +
            "    } else if (i >= 500 || i < 50) {\n" +
            "        result = result - i;\n" +
            "    } else {\n" +
            "        result = result + i / 3;\n" +
            "    }\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
    }
    
    @Benchmark
    public void simpleIfElse_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(simpleIfElse);
        bh.consume(result);
    }
    
    @Benchmark
    public void simpleIfElse_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(simpleIfElse, "SimpleIf");
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedIfElse_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(nestedIfElse);
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedIfElse_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(nestedIfElse, "NestedIf");
        bh.consume(result);
    }
    
    @Benchmark
    public void whileLoop_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(whileLoop);
        bh.consume(result);
    }
    
    @Benchmark
    public void whileLoop_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(whileLoop, "WhileLoop");
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedLoops_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(nestedLoops);
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedLoops_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(nestedLoops, "NestedLoops");
        bh.consume(result);
    }
    
    @Benchmark
    public void conditionalLogic_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(conditionalLogic);
        bh.consume(result);
    }
    
    @Benchmark
    public void conditionalLogic_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(conditionalLogic, "ConditionalLogic");
        bh.consume(result);
    }
}