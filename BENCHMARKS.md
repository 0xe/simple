# Simple Language Performance Benchmarks

This directory contains comprehensive JMH (Java Microbenchmark Harness) benchmarks comparing the performance of the Simple language interpreter versus compiler implementations.

## Overview

The benchmarks measure execution time across different language features to understand the performance characteristics and trade-offs between interpretation and compilation.

## Benchmark Categories

### 1. ArithmeticBenchmark
Tests basic arithmetic operations and mathematical computations:
- **Simple Arithmetic**: Basic operations (+, -, *, /)
- **Complex Arithmetic**: Multi-step mathematical expressions  
- **Intensive Arithmetic**: Loops with heavy mathematical computation

### 2. FunctionBenchmark  
Tests function call overhead and recursion performance:
- **Simple Function Calls**: Basic function invocation in loops
- **Recursive Functions**: Factorial and recursive algorithms
- **Nested Function Calls**: Multiple levels of function composition
- **Fibonacci**: Classic recursive benchmark (small & medium sizes)

### 3. ObjectBenchmark
Tests object creation and property access performance:
- **Simple Object Access**: Basic property reads in loops
- **Nested Object Access**: Deep property traversal
- **Object Manipulation**: Property modification operations
- **Multiple Objects**: Complex object interactions

### 4. ControlFlowBenchmark
Tests conditional logic and loop performance:
- **Simple If/Else**: Basic conditional branching
- **Nested If/Else**: Complex conditional logic
- **While Loops**: Iterative computation
- **Nested Loops**: Multi-dimensional iteration
- **Conditional Logic**: Complex boolean expressions

## Running Benchmarks

### Quick Start
```bash
# Run all benchmarks
./run_benchmarks.sh

# Run specific category
./run_benchmarks.sh arithmetic
./run_benchmarks.sh function  
./run_benchmarks.sh object
./run_benchmarks.sh controlflow
```

### Validation
```bash
# Test that benchmark infrastructure works
./validate_benchmarks.sh
```

### Advanced Usage
```bash
# Run specific benchmark pattern
./run_benchmarks.sh ".*fibonacci.*"

# Run with custom JMH options
java --enable-preview \
     -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     org.openjdk.jmh.Main \
     ".*ArithmeticBenchmark.*" \
     -f 3 -wi 5 -i 10
```

## Understanding Results

### Metrics
- **Mode**: Average time per operation (lower is better)
- **Units**: Microseconds (μs) 
- **Score**: Average execution time
- **Error**: Confidence interval

### Example Output
```
Benchmark                                               Mode  Cnt    Score    Error  Units
ArithmeticBenchmark.simpleArithmetic_interpreter       avgt   10   45.123 ± 2.456  us/op
ArithmeticBenchmark.simpleArithmetic_compiler          avgt   10   12.456 ± 0.789  us/op
```

This shows the compiler is ~3.6x faster than the interpreter for simple arithmetic.

### Analysis Tips
```bash
# Compare interpreter vs compiler performance
grep -E "(interpreter|compiler)" results.txt | sort

# Find top performing benchmarks  
grep "Score" results.txt | sort -k3 -n

# Extract just the performance scores
awk '/Score/ {print $3}' results.txt
```

## Expected Performance Characteristics

### When Compiler Should Excel
- **Arithmetic Operations**: Direct bytecode vs. AST traversal
- **Loop Performance**: JVM optimization vs. repeated interpretation
- **Function Calls**: Compiled method calls vs. interpreter overhead

### When Differences May Be Smaller
- **Object Operations**: Both rely on similar runtime object representations
- **I/O Operations**: Dominated by system calls rather than execution model
- **Simple Scripts**: Setup overhead may dominate short-running code

## Benchmark Configuration

### JVM Settings
- **Heap**: 4GB (-Xms4G -Xmx4G)
- **Preview Features**: Enabled for Java 24
- **Warmup**: 3 iterations, 1 second each
- **Measurement**: 5 iterations, 1 second each
- **Forks**: 2 separate JVM processes

### Benchmark Parameters
- **Blackhole**: Prevents dead code elimination
- **@State(Scope.Benchmark)**: Shared setup across iterations
- **TimeUnit.MICROSECONDS**: Appropriate granularity for these operations

## Extending Benchmarks

### Adding New Benchmarks
1. Create new benchmark class extending `BenchmarkInfrastructure`
2. Add `@Benchmark` methods comparing interpreter vs compiler
3. Use `@Setup` for test data preparation
4. Follow naming convention: `featureName_interpreter` / `featureName_compiler`

### Example New Benchmark
```java
@Benchmark
public void myFeature_interpreter(Blackhole bh) throws Exception {
    Object result = runInterpreter(myTestCode);
    bh.consume(result);
}

@Benchmark  
public void myFeature_compiler(Blackhole bh) throws Exception {
    Object result = runCompiler(myTestCode, "MyFeature");
    bh.consume(result);
}
```

## Troubleshooting

### Common Issues
1. **Compilation Failures**: Ensure `mvn compile` succeeds first
2. **ClassPath Issues**: Maven dependency resolution problems
3. **Preview Features**: Requires `--enable-preview` flag
4. **Memory Issues**: Increase heap size for large benchmarks

### Debug Mode
```bash
# Run with verbose output
java --enable-preview -verbose:class [benchmark command]

# Check classpath
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
cat classpath.txt
```

## Results Analysis

Results are saved in `benchmark_results/` with timestamps. Each run generates:
- **Text Report**: Human-readable results
- **Timestamp**: Unique filename per run  
- **Summary**: Quick performance comparison

Use these results to:
- **Identify Performance Bottlenecks**: Which operations are slowest?
- **Validate Optimizations**: Did compiler changes improve performance?
- **Guide Development**: Which features need performance attention?
- **Document Trade-offs**: When to use interpreter vs compiler?