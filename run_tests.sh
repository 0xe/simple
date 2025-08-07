#!/bin/bash

# Simple Language Test Runner
# Tests both interpreter and compiler modes

# Don't exit on error so we can see all test results
# set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test files
TEST_FILES=(
    "test_arithmetic.sim"
    "test_variables.sim" 
    "test_comparisons.sim"
    "test_if_statements.sim"
    "test_while_loops.sim"
    "test_blocks.sim"
    "test_functions.sim"
    "test_function_scopes.sim"
    "test_objects.sim"
    "test_object_methods.sim"
)

echo "Simple Language Test Suite"
echo "=========================="
echo

# Compile the Simple language first
echo "Compiling Simple language..."
find src/main/java -name "*.java" -exec javac --enable-preview --source 25 -d . -cp src/main/java {} \; 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Simple language compiled successfully${NC}"
else
    echo -e "${YELLOW}Some compilation warnings, continuing...${NC}"
fi
echo

# Function to run a single test
run_test() {
    local test_file=$1
    local test_name=$(basename "$test_file" .sim)
    
    echo -n "Testing $test_name... "
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Run interpreter
    echo -n "[I]"
    timeout 10s java --enable-preview me.vasan.jimple.Jimple -i "tests/$test_file" > "tests/${test_name}_interpreter_output.txt" 2>&1
    local interpreter_exit=$?
    
    # Run compiler 
    echo -n "[C]"
    timeout 10s java --enable-preview me.vasan.jimple.Jimple "tests/$test_file" > "tests/${test_name}_compiler_log.txt" 2>&1
    local compiler_exit=$?
    
    # If compilation succeeded, run the compiled program
    local compiled_exit=1
    if [ $compiler_exit -eq 0 ] && [ -f "tests/${test_name}.class" ]; then
        echo -n "[R]"
        timeout 10s java -cp .:tests "${test_name}" > "tests/${test_name}_compiled_output.txt" 2>&1
        compiled_exit=$?
    else
        echo "Compilation failed" > "tests/${test_name}_compiled_output.txt"
    fi
    
    # Check results
    if [ $interpreter_exit -eq 0 ]; then
        if [ $compiled_exit -eq 0 ]; then
            echo -e " ${GREEN}PASS${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            echo -e " ${YELLOW}PARTIAL${NC} (interpreter works, compiler fails)"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    else
        if [ $compiled_exit -eq 0 ]; then
            echo -e " ${YELLOW}PARTIAL${NC} (compiler works, interpreter fails)"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        else
            echo -e " ${RED}FAIL${NC} (both fail)"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    fi
    
    # Clean up class file
    rm -f "tests/${test_name}.class"
}

# Run all tests
echo "Running tests:"
echo "Legend: [I] = Interpreter, [C] = Compiler, [R] = Run compiled"
echo

for test_file in "${TEST_FILES[@]}"; do
    if [ -f "tests/$test_file" ]; then
        run_test "$test_file"
    else
        echo -e "Warning: Test file tests/$test_file not found"
    fi
done

# Summary
echo
echo "Test Summary:"
echo "============="
echo -e "Total tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $PASSED_TESTS${NC}"
echo -e "${RED}Failed:       $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}Some tests failed. Check output files for details.${NC}"
    exit 1
fi