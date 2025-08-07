SHELL := /bin/bash
all: jimple

cimple: clean
	mkdir -p target
	clang -g -Wall -o target/c_simple cimple/*.c
	./target/c_simple ./tests/baby_fib.sim

build:
	javac jimple/me/vasan/jimple/*.java

# Pass -DParserDebug=1 to debug the parser
jimple: clean build
	# pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/hello.sim; popd
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple; popd

fib: build
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/fib.sim; popd

expt: build
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/expt.sim; popd

scoping: build 
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/scoping.sim; popd

objects: build
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/object_basic.sim; popd

objects-nested: build
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/object_nested.sim; popd

objects-complex: build
	pushd jimple; CLASSPATH=. java me/vasan/jimple/Jimple ${PWD}/tests/object_complex.sim; popd

clean:
	rm -rf target/*
	rm -rf jimple/me/vasan/jimple/*.class
