package me.vasan.jimple.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for object operations comparing interpreter vs compiler performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G", "--enable-preview"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class ObjectBenchmark extends BenchmarkInfrastructure {
    
    private String simpleObjectAccess;
    private String nestedObjectAccess;
    private String objectManipulation;
    private String multipleObjects;
    
    @Setup
    public void setup() throws Exception {
        // Simple object property access
        simpleObjectAccess = 
            "let obj = {x: 10, y: 20, z: 30};\n" +
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 1000) {\n" +
            "    result = result + obj.x + obj.y + obj.z;\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Nested object access
        nestedObjectAccess =
            "let person = {\n" +
            "    name: \"Alice\",\n" +
            "    address: {\n" +
            "        street: \"123 Main St\",\n" +
            "        city: \"Anytown\",\n" +
            "        coords: {x: 10.5, y: 20.7}\n" +
            "    },\n" +
            "    age: 30\n" +
            "};\n" +
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 500) {\n" +
            "    result = result + person.address.coords.x + person.address.coords.y + person.age;\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
        
        // Object property manipulation
        objectManipulation = createObjectManipulation(1000);
        
        // Multiple objects interaction
        multipleObjects =
            "let obj1 = {value: 1};\n" +
            "let obj2 = {value: 2};\n" +
            "let obj3 = {value: 3};\n" +
            "let result = 0;\n" +
            "let i = 0;\n" +
            "while (i < 300) {\n" +
            "    obj1.value = obj1.value + 1;\n" +
            "    obj2.value = obj2.value * 2;\n" +
            "    obj3.value = obj3.value + obj1.value;\n" +
            "    result = result + obj1.value + obj2.value + obj3.value;\n" +
            "    i = i + 1;\n" +
            "}\n" +
            "result;\n";
    }
    
    @Benchmark
    public void simpleObjectAccess_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(simpleObjectAccess);
        bh.consume(result);
    }
    
    @Benchmark
    public void simpleObjectAccess_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(simpleObjectAccess, "SimpleObject");
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedObjectAccess_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(nestedObjectAccess);
        bh.consume(result);
    }
    
    @Benchmark
    public void nestedObjectAccess_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(nestedObjectAccess, "NestedObject");
        bh.consume(result);
    }
    
    @Benchmark
    public void objectManipulation_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(objectManipulation);
        bh.consume(result);
    }
    
    @Benchmark
    public void objectManipulation_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(objectManipulation, "ObjectManipulation");
        bh.consume(result);
    }
    
    @Benchmark
    public void multipleObjects_interpreter(Blackhole bh) throws Exception {
        Object result = runInterpreter(multipleObjects);
        bh.consume(result);
    }
    
    @Benchmark
    public void multipleObjects_compiler(Blackhole bh) throws Exception {
        Object result = runCompiler(multipleObjects, "MultipleObjects");
        bh.consume(result);
    }
}