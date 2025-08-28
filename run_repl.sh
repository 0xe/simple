#!/bin/bash

# Simple REPL launcher script
# This script compiles and runs the REPL

echo "Compiling Simple REPL..."
javac --enable-preview --source 24 -d . -cp src/main/java src/main/java/me/vasan/jimple/*.java src/main/java/me/vasan/jimple/errors/*.java

if [ $? -eq 0 ]; then
    echo "Starting Simple REPL..."
    echo "Use :help for available commands"
    echo ""
    java --enable-preview me.vasan.jimple.Jimple
else
    echo "Compilation failed!"
    exit 1
fi