#!/bin/bash

# Simple Language Benchmark Runner
# Runs JMH benchmarks comparing interpreter vs compiler performance

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Simple Language Performance Benchmarks${NC}"
echo -e "${BLUE}=======================================${NC}"
echo

# Compile the project first
echo -e "${YELLOW}Compiling Simple language and benchmarks...${NC}"
mvn compile -q

if [ $? -ne 0 ]; then
    echo -e "${RED}Compilation failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Compilation successful${NC}"
echo

# Check for benchmark pattern argument
BENCHMARK_PATTERN=".*"
BENCHMARK_NAME="All"

if [ $# -eq 1 ]; then
    case $1 in
        "arithmetic"|"Arithmetic")
            BENCHMARK_PATTERN=".*ArithmeticBenchmark.*"
            BENCHMARK_NAME="Arithmetic"
            ;;
        "function"|"Function")
            BENCHMARK_PATTERN=".*FunctionBenchmark.*"
            BENCHMARK_NAME="Function"
            ;;
        "object"|"Object")
            BENCHMARK_PATTERN=".*ObjectBenchmark.*"
            BENCHMARK_NAME="Object"
            ;;
        "controlflow"|"ControlFlow"|"control")
            BENCHMARK_PATTERN=".*ControlFlowBenchmark.*"
            BENCHMARK_NAME="ControlFlow"
            ;;
        *)
            BENCHMARK_PATTERN="$1"
            BENCHMARK_NAME="Custom($1)"
            ;;
    esac
fi

echo -e "${YELLOW}Running $BENCHMARK_NAME benchmarks...${NC}"
echo "Pattern: $BENCHMARK_PATTERN"
echo

# Create output directory if it doesn't exist
mkdir -p benchmark_results

# Generate timestamp for unique result files
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULT_FILE="benchmark_results/results_${BENCHMARK_NAME,,}_$TIMESTAMP.txt"

# Run benchmarks with proper classpath and JVM arguments
java --enable-preview \
     -Xms4G -Xmx4G \
     -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
     org.openjdk.jmh.Main \
     "$BENCHMARK_PATTERN" \
     -rf text \
     -rff "$RESULT_FILE"

if [ $? -eq 0 ]; then
    echo
    echo -e "${GREEN}Benchmarks completed successfully!${NC}"
    echo -e "Results saved to: ${BLUE}$RESULT_FILE${NC}"
    echo
    echo -e "${YELLOW}Quick summary:${NC}"
    if [ -f "$RESULT_FILE" ]; then
        echo "----------------------------------------"
        grep "Benchmark\|Score" "$RESULT_FILE" | head -20
        echo "----------------------------------------"
        echo "(showing first 20 lines - see full results in $RESULT_FILE)"
    fi
else
    echo -e "${RED}Benchmarks failed!${NC}"
    exit 1
fi

echo
echo -e "${YELLOW}Available benchmark categories:${NC}"
echo "  ./run_benchmarks.sh arithmetic    - Arithmetic operations"
echo "  ./run_benchmarks.sh function      - Function calls and recursion"  
echo "  ./run_benchmarks.sh object        - Object operations"
echo "  ./run_benchmarks.sh controlflow   - Control flow (if/while)"
echo "  ./run_benchmarks.sh               - All benchmarks"
echo
echo -e "${YELLOW}Example analysis:${NC}"
echo "  grep 'interpreter\\|compiler' $RESULT_FILE | sort"
echo "  grep 'Score' $RESULT_FILE"