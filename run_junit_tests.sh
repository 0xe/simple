#!/bin/bash

# Jimple JUnit Test Runner Script
# Usage: ./run_junit_tests.sh [TestClassName]

# Set up classpath
JUNIT_JAR="/Users/satish/.m2/repository/junit/junit/4.13.1/junit-4.13.1.jar"
HAMCREST_JAR="/Users/satish/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
CLASSPATH=".:src/main/java:${JUNIT_JAR}:${HAMCREST_JAR}"

echo "Jimple JUnit Test Runner"
echo "========================"

# Compile all Java files first
echo "Compiling main classes..."
find src/main/java -name "*.java" -exec javac --enable-preview --source 25 -d . -cp src/main/java {} \; 2>/dev/null

echo "Compiling test classes..."
find src/test/java -name "*.java" -exec javac --enable-preview --source 25 -d . -cp "${CLASSPATH}" {} \; 2>/dev/null

echo ""

# Run tests
if [ $# -eq 0 ]; then
    echo "Running all tests with TestRunner..."
    java -cp "${CLASSPATH}" me.vasan.jimple.TestRunner
else
    echo "Running specific test: $1"
    java -cp "${CLASSPATH}" org.junit.runner.JUnitCore me.vasan.jimple.$1
fi