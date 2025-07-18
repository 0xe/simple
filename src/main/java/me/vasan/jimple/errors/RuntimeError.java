package me.vasan.jimple.errors;

public class RuntimeError extends Exception {
    String message;
    public RuntimeError(String s) {
        this.message = s;
    }
}
