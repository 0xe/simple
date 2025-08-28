package me.vasan.jimple;

import java.lang.reflect.Method;

public class RuntimeFunction {
    private Object instance;
    private String methodName;
    
    public RuntimeFunction(Object instance, String methodName) {
        this.instance = instance;
        this.methodName = methodName;
    }
    
    public Object call(Object... args) {
        try {
            // Find the method by name
            Class<?> clazz = instance.getClass();
            Method[] methods = clazz.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return method.invoke(instance, args);
                }
            }
            
            throw new RuntimeException("Method not found: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException("Error calling function: " + e.getMessage(), e);
        }
    }
}