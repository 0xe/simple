package me.vasan.jimple;

/**
 * Interface for compiled Simple scripts.
 * Each compiled script implements this interface to provide a call() method
 * that executes the script and returns its result.
 */
public interface Callable {
    /**
     * Execute the compiled script and return its result.
     * @return The result of executing the script, or null if no result
     */
    Object call();
}