#!/bin/bash

# Benchmark Validation Script
# Quick test to ensure benchmark infrastructure works

echo "Simple Language Benchmark Validation"
echo "===================================="

# Compile first
echo "1. Compiling project..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi
echo "✅ Compilation successful"

# Test basic benchmark infrastructure by checking if class file exists
echo "2. Testing benchmark infrastructure..."
if [ -f "target/classes/me/vasan/jimple/benchmarks/BenchmarkInfrastructure.class" ]; then
    echo "✅ Benchmark infrastructure OK"
else
    echo "❌ Benchmark infrastructure test failed"
    exit 1
fi

# Run a quick validation test
echo "3. Running quick validation benchmark..."
java --enable-preview -cp target/classes me.vasan.jimple.benchmarks.SimpleBenchmarkRunner

if [ $? -eq 0 ]; then
    echo "✅ Benchmark validation successful!"
    echo ""
    echo "Ready to run full benchmarks with:"
    echo "  ./run_benchmarks.sh"
else
    echo "❌ Benchmark validation failed"
    echo "Check that all dependencies are properly configured"
    exit 1
fi