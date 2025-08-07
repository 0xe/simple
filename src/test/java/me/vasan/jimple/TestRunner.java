package me.vasan.jimple;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Main test runner that can be executed from command line or IDE
 * Usage: java -cp ".:target/test-classes:target/classes" me.vasan.jimple.TestRunner
 */
public class TestRunner {
    public static void main(String[] args) {
        System.out.println("Jimple Language Test Suite");
        System.out.println("===========================");
        
        Class<?>[] testClasses = {
            EnvironmentTest.class,
            InterpreterIntegrationTest.class,
            CompilerIntegrationTest.class,
            JimpleTestHarness.class
        };
        
        for (Class<?> testClass : testClasses) {
            System.out.println("\nRunning " + testClass.getSimpleName() + ":");
            System.out.println("-".repeat(40));
            
            JUnitCore junit = new JUnitCore();
            Result result = junit.run(testClass);
            
            System.out.println("Tests run: " + result.getRunCount());
            System.out.println("Failures: " + result.getFailureCount());
            System.out.println("Ignored: " + result.getIgnoreCount());
            System.out.println("Time: " + result.getRunTime() + "ms");
            
            if (result.getFailureCount() > 0) {
                System.out.println("\nFailures:");
                for (Failure failure : result.getFailures()) {
                    System.out.println("- " + failure.getTestHeader());
                    System.out.println("  " + failure.getMessage());
                    if (failure.getException() != null) {
                        System.out.println("  " + failure.getException().getClass().getSimpleName());
                    }
                }
            }
            
            if (result.wasSuccessful()) {
                System.out.println("✓ All tests passed");
            } else {
                System.out.println("✗ Some tests failed");
            }
        }
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Test suite completed");
    }
}