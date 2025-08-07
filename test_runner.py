#!/usr/bin/env python3

"""
Simple Language Test Runner
Tests both interpreter and compiler modes
"""

import os
import subprocess
import sys
from pathlib import Path

# Test files to run
TEST_FILES = [
    "test_arithmetic.sim",
    "test_variables.sim", 
    "test_comparisons.sim",
    "test_if_statements.sim",
    "test_while_loops.sim",
    "test_blocks.sim",
    "test_functions.sim",
    "test_function_scopes.sim",
    "test_objects.sim",
    "test_object_methods.sim"
]

# Colors
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    NC = '\033[0m'  # No Color

def run_command(cmd, timeout=10):
    """Run a command and return exit code and output"""
    try:
        result = subprocess.run(
            cmd, 
            shell=True, 
            capture_output=True, 
            text=True, 
            timeout=timeout,
            cwd=Path(__file__).parent
        )
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "Timeout"
    except Exception as e:
        return -2, "", str(e)

def test_file(test_file):
    """Test a single file with both interpreter and compiler"""
    test_name = Path(test_file).stem
    print(f"Testing {test_name}... ", end="", flush=True)
    
    results = {
        'interpreter': {'success': False, 'output': '', 'error': ''},
        'compiler': {'success': False, 'output': '', 'error': ''},
        'execution': {'success': False, 'output': '', 'error': ''}
    }
    
    # Test interpreter
    print("[I]", end="", flush=True)
    cmd = f"java --enable-preview -cp src/main/java me.vasan.jimple.Jimple -i tests/{test_file}"
    exit_code, stdout, stderr = run_command(cmd)
    results['interpreter']['success'] = (exit_code == 0)
    results['interpreter']['output'] = stdout
    results['interpreter']['error'] = stderr
    
    # Test compiler
    print("[C]", end="", flush=True)
    cmd = f"java --enable-preview -cp src/main/java me.vasan.jimple.Jimple tests/{test_file}"
    exit_code, stdout, stderr = run_command(cmd)
    results['compiler']['success'] = (exit_code == 0)
    results['compiler']['output'] = stdout
    results['compiler']['error'] = stderr
    
    # Test compiled execution if compilation succeeded
    class_file = f"tests/{test_name}.class"
    if results['compiler']['success'] and os.path.exists(class_file):
        print("[R]", end="", flush=True)
        cmd = f"java -cp src/main/java:tests {test_name}"
        exit_code, stdout, stderr = run_command(cmd)
        results['execution']['success'] = (exit_code == 0)
        results['execution']['output'] = stdout
        results['execution']['error'] = stderr
        
        # Clean up
        try:
            os.remove(class_file)
        except:
            pass
    
    # Determine overall result
    interpreter_ok = results['interpreter']['success']
    execution_ok = results['execution']['success']
    
    if interpreter_ok and execution_ok:
        print(f" {Colors.GREEN}PASS{Colors.NC}")
        return "pass"
    elif interpreter_ok or execution_ok:
        print(f" {Colors.YELLOW}PARTIAL{Colors.NC}")
        return "partial"
    else:
        print(f" {Colors.RED}FAIL{Colors.NC}")
        return "fail"

def main():
    print("Simple Language Test Suite")
    print("==========================\n")
    
    # Compile the Simple language first
    print("Compiling Simple language...")
    cmd = "javac --enable-preview --source 25 -cp src/main/java src/main/java/me/vasan/jimple/*.java"
    exit_code, stdout, stderr = run_command(cmd, timeout=30)
    
    if exit_code != 0:
        print(f"{Colors.RED}Failed to compile Simple language{Colors.NC}")
        print(f"Error: {stderr}")
        return 1
    
    print(f"{Colors.GREEN}Simple language compiled successfully{Colors.NC}\n")
    
    # Run tests
    print("Running tests:")
    print("Legend: [I] = Interpreter, [C] = Compiler, [R] = Run compiled\n")
    
    total_tests = 0
    passed_tests = 0
    partial_tests = 0
    failed_tests = 0
    
    for test_file in TEST_FILES:
        if os.path.exists(f"tests/{test_file}"):
            total_tests += 1
            result = test_file(test_file)
            if result == "pass":
                passed_tests += 1
            elif result == "partial":
                partial_tests += 1
            else:
                failed_tests += 1
        else:
            print(f"Warning: Test file tests/{test_file} not found")
    
    # Summary
    print(f"\nTest Summary:")
    print(f"=============")
    print(f"Total tests:  {total_tests}")
    print(f"{Colors.GREEN}Passed:       {passed_tests}{Colors.NC}")
    print(f"{Colors.YELLOW}Partial:      {partial_tests}{Colors.NC}")
    print(f"{Colors.RED}Failed:       {failed_tests}{Colors.NC}")
    
    if failed_tests == 0 and partial_tests == 0:
        print(f"\n{Colors.GREEN}All tests passed!{Colors.NC}")
        return 0
    elif failed_tests == 0:
        print(f"\n{Colors.YELLOW}All tests passed with some partial results{Colors.NC}")
        return 0
    else:
        print(f"\n{Colors.RED}Some tests failed{Colors.NC}")
        return 1

if __name__ == "__main__":
    sys.exit(main())