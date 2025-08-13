#!/bin/bash

# Simple REPL launcher script
# This script compiles and runs the REPL

echo "Compiling Simple REPL..."
javac -cp . src/main/java/me/vasan/jimple/*.java

if [ $? -eq 0 ]; then
    echo "Starting Simple REPL..."
    echo "Use :help for available commands"
    echo ""
    CLASSPATH=.:src/main/java:me java me.vasan.jimple.Jimple
else
    echo "Compilation failed!"
    exit 1
fi