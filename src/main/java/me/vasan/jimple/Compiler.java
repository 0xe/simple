package me.vasan.jimple;

import me.vasan.jimple.errors.RuntimeError;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.classfile.ClassFile.ACC_STATIC;

public class Compiler {

    ClassFile fPgmClassFile = null;
    ClassDesc fPgmClass = null;

    String fName = null;
    String fPath = null;
    int methCounter = 0;

    // This thing is lisp-2 after all
    HashMap<String, MethodInfo> methodTable = null;
    
    // Track function expressions for method generation
    ArrayList<FunctionExpr> functionExpressions = new ArrayList<>();
    int functionExprCounter = 0;

    /* this should be per method as slots _WILL_ be reused! */
    HashMap<String, VarInfo> varTable = null;

    record TypeInfo(TypeKind t, ClassDesc c) {
    };

    record MethodInfo(int counter, String sig) {
    };

    record VarInfo(int slot, TypeInfo type) {
    };
    
    // Helper methods for runtime type conversion
    private void unboxToDouble(CodeBuilder cb) {
        // Convert Object to double - simplified approach
        // Try to cast to Number and call doubleValue(), or return 0.0 on failure
        cb.checkcast(ClassDesc.of("java.lang.Number"));
        cb.invokevirtual(ClassDesc.of("java.lang.Number"), "doubleValue", 
                MethodTypeDesc.ofDescriptor("()D"));
    }
    
    private void unboxToBoolean(CodeBuilder cb) {
        // Convert Object to boolean - simplified approach  
        // Try to cast to Boolean and call booleanValue(), or return false on failure
        cb.checkcast(ClassDesc.of("java.lang.Boolean"));
        cb.invokevirtual(ClassDesc.of("java.lang.Boolean"), "booleanValue", 
                MethodTypeDesc.ofDescriptor("()Z"));
    }
    
    // Helper to compile expression and ensure it results in a double on stack
    private void compileExprAsDouble(Expr e, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        compileExpr(e, classBuilder, mb, cb);
        TypeInfo type = findExprType(e);
        if (type != null && type.t == TypeKind.REFERENCE) {
            unboxToDouble(cb);
        }
        // If it's already double or boolean, leave as-is
        // Boolean will be treated as 0.0/1.0 which works for arithmetic
    }

    public Compiler(String name) {
        String fileName = String.valueOf(Path.of(name).getFileName());
        fName = fileName.split("\\.")[0];
        methodTable = new HashMap<String, MethodInfo>();
        varTable = new HashMap<String, VarInfo>();
        fPath = name;
    }

    public void compile(Pgm ast) throws IOException {
        // add class preamble
        // go through declarations and statements and generate
        // go through fns, expressions and generate
        if (ast != null) {
            createClassStub();
            writeClass(ast.decls);
        }
    }

    private void createClassStub() throws IOException {
        fPgmClassFile = ClassFile.of();
        fPgmClass = ClassDesc.of(fName);
    }

    private TypeInfo findType(Var v) {
        return findExprType(v.rvalue);
    }
    
    private TypeInfo findExprType(Expr e) {
        switch (e.type) {
            case ASSIGN_EXPR -> {
                return findExprType(e.a.e);
            }
            case BINARY_EXPR -> {
                // Most binary operations on numbers return numbers
                switch (e.b.o) {
                    case EQ, NEQ, GT, GTE, LT, LTE, LOR, LAN -> {
                        return new TypeInfo(TypeKind.BOOLEAN, ClassDesc.of("boolean"));
                    }
                    case PLUS -> {
                        // String + anything = String, otherwise numeric
                        TypeInfo leftType = findExprType(e.b.lhs);
                        if (leftType.c.equals(ClassDesc.of("java.lang", "String"))) {
                            return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "String"));
                        }
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
                    }
                    case MIN, MUL, DIV -> {
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
                    }
                }
            }
            case UNARY -> {
                // Return appropriate type based on unary operation
                switch (e.ue.o) {
                    case NOT -> {
                        return new TypeInfo(TypeKind.BOOLEAN, ClassDesc.of("boolean"));
                    }
                    case NEG -> {
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
                    }
                }
                return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
            }
            case CALL -> {
                // Check if it's a special function like print
                if (e.ce.id.name.equals("print") || e.ce.id.name.equals("clock")) {
                    return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
                }
                // User-defined functions now return Objects
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
            }
            case FUNCTION -> {
                return new TypeInfo(TypeKind.REFERENCE,
                        ClassDesc.of("me.vasan.jimple", "FunctionExpr"));
            }
            case PRIMARY -> {
                PrimaryExpr p = e.pe;
                switch (p.type) {
                    case ID -> {
                        // Look up variable type from table
                        VarInfo varInfo = varTable.get(p.id.name);
                        if (varInfo != null) {
                            return varInfo.type;
                        }
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double")); // default
                    }
                    case NUM -> {
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
                    }
                    case STR -> {
                        return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "String"));
                    }
                    case B -> {
                        return new TypeInfo(TypeKind.BOOLEAN, ClassDesc.of("boolean"));
                    }
                    case NIL -> {
                        return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
                    }
                }
            }
            case OBJECT -> {
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("me.vasan.jimple", "SimpleObject"));
            }
            case ARRAY -> {
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("me.vasan.jimple", "SimpleArray"));
            }
            case PROPERTY_ACCESS -> {
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
            }
            case INDEX_ACCESS -> {
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
            }
            case PROPERTY_ASSIGN -> {
                // Property assignment returns the assigned value, but since it gets boxed 
                // in the bytecode, it's always an Object reference
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
            }
        }
        return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object")); // default
    }

    private String findMethodDescriptor(FunctionExpr f) {
        // Since Simple is dynamically typed, we use Object for all parameters and return type
        // This allows any Simple type (string, number, boolean, object, array) to be passed/returned
        StringBuilder descriptor = new StringBuilder("(");
        
        // Add Object parameter for each function argument
        for (int i = 0; i < f.a.size(); i++) {
            descriptor.append("Ljava/lang/Object;");
        }
        
        descriptor.append(")Ljava/lang/Object;"); // Return Object
        return descriptor.toString();
    }
    
    private void compileMethodCall(Var v, CallExpr ce, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        // Method call: obj.method(args) 
        // This is compiled as: obj.method.call([args])
        
        // Get the function object from the property
        compileExpr(ce.object, classBuilder, mb, cb); // Push object
        
        // Cast to SimpleObject if needed (similar to property access)
        TypeInfo objType = findExprType(ce.object);
        if (objType.t == TypeKind.REFERENCE && !objType.c.equals(ClassDesc.of("me.vasan.jimple", "SimpleObject"))) {
            cb.checkcast(ClassDesc.of("me.vasan.jimple.SimpleObject"));
        }
        
        cb.ldc(ce.id.name); // Push method name
        
        // Call SimpleObject.get(String) to get the function object
        cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                "get",
                MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Ljava/lang/Object;"));
        
        // Now we have the function object on the stack
        // Create arguments array and call the function using the same pattern as print()
        cb.ldc(ce.a.size()); // Push argument count
        cb.anewarray(ClassDesc.of("java.lang.Object"));
        
        for (var ai : ce.a) {
            cb.dup();
            switch (ce.a.indexOf(ai)) {
                case 0 -> cb.iconst_0();
                case 1 -> cb.iconst_1();
                case 2 -> cb.iconst_2();
                case 3 -> cb.iconst_3();
                case 4 -> cb.iconst_4();
                case 5 -> cb.iconst_5();
                default -> cb.ldc(ce.a.indexOf(ai));
            }
            
            compileExpr(ai, classBuilder, mb, cb);
            
            // Box primitive types
            TypeInfo argType = findExprType(ai);
            if (argType != null) {
                switch (argType.t) {
                    case DOUBLE -> {
                        cb.invokestatic(ClassDesc.of("java.lang.Double"), 
                                "valueOf", MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                    }
                    case BOOLEAN -> {
                        cb.invokestatic(ClassDesc.of("java.lang.Boolean"), 
                                "valueOf", MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                    }
                }
            }
            
            cb.aastore();
        }
        
        // Call the function object - cast to RuntimeFunction and call its call method  
        // At this point stack is: [function_object, args_array]
        // We need: [RuntimeFunction, args_array] for the method call
        cb.swap(); // Now stack is: [args_array, function_object]
        cb.checkcast(ClassDesc.of("me.vasan.jimple.RuntimeFunction")); // [args_array, RuntimeFunction]
        cb.swap(); // Now stack is: [RuntimeFunction, args_array]
        cb.invokevirtual(ClassDesc.of("me.vasan.jimple.RuntimeFunction"),
                "call",
                MethodTypeDesc.ofDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;"));
        
        // Store result if this is an assignment
        if (v != null) {
            int slot = cb.allocateLocal(TypeKind.REFERENCE);
            cb.astore(slot);
            varTable.put(v.id.name, new VarInfo(slot, new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"))));
        }
    }

    private void compileCallExpr(Var v, CallExpr ce, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        // Check if this is a method call (obj.method())
        if (ce.object != null) {
            compileMethodCall(v, ce, classBuilder, mb, cb);
            return;
        }
        
        MethodInfo m = methodTable.get(ce.id.name);
        if (m != null) {
            // Load 'this' first for instance method call
            cb.aload(0);
            
            // Compile arguments and push onto stack, boxing primitives as needed
            for (Expr arg : ce.a) {
                compileExpr(arg, classBuilder, mb, cb);
                
                // Box primitive types to Object for function calls
                TypeInfo argType = findExprType(arg);
                if (argType != null) {
                    switch (argType.t) {
                        case DOUBLE -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Double"), 
                                    "valueOf", MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                        }
                        case BOOLEAN -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Boolean"), 
                                    "valueOf", MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                        }
                        // References are already objects, no boxing needed
                    }
                }
            }
            
            // Call the user-defined function (now an instance method)
            cb.invokevirtual(ClassDesc.of(fName), "meth" + m.counter, MethodTypeDesc.ofDescriptor(m.sig));
            
            // Store result if this is an assignment
            if (v != null) {
                // Function calls now return Object, so store as Object reference
                int slot = cb.allocateLocal(TypeKind.REFERENCE);
                cb.astore(slot);
                varTable.put(v.id.name, new VarInfo(slot, new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"))));
            }
        } else { // special behaviour for "print" and "clock"?
            if (ce.id.name.equals("print")) {
                cb.new_(ClassDesc.of("NativeFunction"))
                        .dup()
                        .ldc("print")
                        .invokespecial(ClassDesc.of("NativeFunction"),
                                "<init>",
                                MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"))
                        .ldc(ce.a.size()) // Use actual argument count
                        .anewarray(ClassDesc.of("java.lang.Object"));
                for (var ai : ce.a) {
                    cb.dup();
                    switch (ce.a.indexOf(ai)) {
                        case 0 -> {
                            cb.iconst_0();
                        }
                        case 1 -> {
                            cb.iconst_1();
                        }
                        case 2 -> {
                            cb.iconst_2();
                        }
                        case 3 -> {
                            cb.iconst_3();
                        }
                        case 4 -> {
                            cb.iconst_4();
                        }
                        case 5 -> {
                            cb.iconst_5();
                        }
                        default -> {
                            cb.iconst_m1();
                        }
                    }
                    /* Compile the argument expression */
                    compileExpr(ai, classBuilder, mb, cb);
                    
                    /* Box primitive types */
                    TypeInfo argType = findExprType(ai);
                    switch (argType.t) {
                        case DOUBLE -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf", MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                        }
                        case BOOLEAN -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Boolean"), "valueOf", MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                        }
                    }
                    
                    cb.aastore();
                }
                cb.invokevirtual(ClassDesc.of("NativeFunction"),
                                "call",
                                MethodTypeDesc.ofDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;"));
                // Don't pop - let the expression statement handler decide
            } else if (ce.id.name.equals("clock")) {

            } else { /* TODO: error handling */
                throw new Error("bad call function");
            }
        }
    }

    private void compileExpr(Expr e, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        switch (e.type) {
            case ASSIGN_EXPR -> {
                // Compile right-hand side expression first
                compileExpr(e.a.e, classBuilder, mb, cb);
                
                // Store result in variable slot
                VarInfo varInfo = varTable.get(e.a.id.name);
                if (varInfo != null) {
                    // Duplicate value on stack for assignment expression result
                    switch (varInfo.type.t) {
                        case DOUBLE -> {
                            cb.dup2(); // double takes 2 stack slots
                            cb.dstore(varInfo.slot);
                        }
                        case BOOLEAN -> {
                            cb.dup();
                            cb.istore(varInfo.slot);
                        }
                        case REFERENCE -> {
                            cb.dup();
                            cb.astore(varInfo.slot);
                        }
                        default -> {
                            cb.dup();
                            cb.astore(varInfo.slot);
                        }
                    }
                }
            }
            case BINARY_EXPR -> {
                // For arithmetic operations, ensure both operands are doubles
                // Load left operand as double
                compileExprAsDouble(e.b.lhs, classBuilder, mb, cb);
                
                // Load right operand as double
                compileExprAsDouble(e.b.rhs, classBuilder, mb, cb);
                
                // Apply operation
                switch (e.b.o) {
                    case PLUS -> cb.dadd();
                    case MIN -> cb.dsub();
                    case MUL -> cb.dmul();
                    case DIV -> cb.ddiv();
                    case EQ -> {
                        // Compare doubles: dcmpl returns -1, 0, or 1
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifeq(trueLabel);   // Jump to true if equal (result == 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case NEQ -> {
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifne(trueLabel);   // Jump to true if not equal (result != 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case GT -> {
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifgt(trueLabel);   // Jump to true if greater (result > 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case GTE -> {
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifge(trueLabel);   // Jump to true if greater or equal (result >= 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case LT -> {
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.iflt(trueLabel);   // Jump to true if less than (result < 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case LTE -> {
                        cb.dcmpl();
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifle(trueLabel);   // Jump to true if less than or equal (result <= 0)
                        cb.iconst_0();        // Push false
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Push true
                        cb.labelBinding(endLabel);
                    }
                    case LOR -> {
                        // Logical OR: result = (lhs != 0) || (rhs != 0) ? 1 : 0
                        // Stack before: [lhs, rhs]
                        
                        // Simple approach: use bitwise OR on the boolean values
                        // Convert non-zero values to 1, zero values to 0, then OR them
                        
                        // Handle rhs: convert to 0 or 1
                        var rhs_nonzero = cb.newLabel();
                        var rhs_done = cb.newLabel();
                        cb.ifne(rhs_nonzero);    // if rhs != 0, jump
                        cb.iconst_0();           // rhs was 0, push 0
                        cb.goto_(rhs_done);
                        cb.labelBinding(rhs_nonzero);
                        cb.iconst_1();           // rhs was non-zero, push 1
                        cb.labelBinding(rhs_done);
                        // Stack now: [lhs, rhs_bool] where rhs_bool is 0 or 1
                        
                        // Handle lhs: convert to 0 or 1
                        cb.swap();               // Stack: [rhs_bool, lhs]
                        var lhs_nonzero = cb.newLabel();
                        var lhs_done = cb.newLabel();
                        cb.ifne(lhs_nonzero);    // if lhs != 0, jump
                        cb.iconst_0();           // lhs was 0, push 0
                        cb.goto_(lhs_done);
                        cb.labelBinding(lhs_nonzero);
                        cb.iconst_1();           // lhs was non-zero, push 1
                        cb.labelBinding(lhs_done);
                        // Stack now: [rhs_bool, lhs_bool] where both are 0 or 1
                        
                        // Now do bitwise OR
                        cb.ior();
                    }
                    case LAN -> {
                        // Logical AND: result = (lhs != 0) && (rhs != 0) ? 1 : 0
                        // Stack before: [lhs, rhs]
                        
                        // Simple approach: use bitwise AND on the boolean values
                        // Convert non-zero values to 1, zero values to 0, then AND them
                        
                        // Handle rhs: convert to 0 or 1
                        var rhs_nonzero = cb.newLabel();
                        var rhs_done = cb.newLabel();
                        cb.ifne(rhs_nonzero);    // if rhs != 0, jump
                        cb.iconst_0();           // rhs was 0, push 0
                        cb.goto_(rhs_done);
                        cb.labelBinding(rhs_nonzero);
                        cb.iconst_1();           // rhs was non-zero, push 1
                        cb.labelBinding(rhs_done);
                        // Stack now: [lhs, rhs_bool] where rhs_bool is 0 or 1
                        
                        // Handle lhs: convert to 0 or 1
                        cb.swap();               // Stack: [rhs_bool, lhs]
                        var lhs_nonzero = cb.newLabel();
                        var lhs_done = cb.newLabel();
                        cb.ifne(lhs_nonzero);    // if lhs != 0, jump
                        cb.iconst_0();           // lhs was 0, push 0
                        cb.goto_(lhs_done);
                        cb.labelBinding(lhs_nonzero);
                        cb.iconst_1();           // lhs was non-zero, push 1
                        cb.labelBinding(lhs_done);
                        // Stack now: [rhs_bool, lhs_bool] where both are 0 or 1
                        
                        // Now do bitwise AND
                        cb.iand();
                    }
                }
            }
            case UNARY -> {
                compileExpr(e.ue.e, classBuilder, mb, cb);
                switch (e.ue.o) {
                    case NEG -> {
                        cb.dneg();
                    }
                    case NOT -> {
                        // Implement boolean NOT: if value is 0, push 1; if non-zero, push 0
                        var trueLabel = cb.newLabel();
                        var endLabel = cb.newLabel();
                        cb.ifeq(trueLabel);   // Jump to true if value is 0 (false)
                        cb.iconst_0();        // Value was non-zero, so push 0 (false)
                        cb.goto_(endLabel);
                        cb.labelBinding(trueLabel);
                        cb.iconst_1();        // Value was 0, so push 1 (true)
                        cb.labelBinding(endLabel);
                    }
                }
            }
            case CALL -> {
                compileCallExpr(null, e.ce, classBuilder, mb, cb);
            }
            case FUNCTION -> {
                // Add this function expression to the list for method generation
                functionExpressions.add(e.fe);
                String methodName = "funcExpr" + functionExprCounter++;
                
                // Create RuntimeFunction object that references the generated method
                cb.new_(ClassDesc.of("me.vasan.jimple.RuntimeFunction"));
                cb.dup();
                cb.aload(0); // Pass 'this' as the instance
                cb.ldc(methodName); // Method name for this function expression
                cb.invokespecial(ClassDesc.of("me.vasan.jimple.RuntimeFunction"),
                        "<init>",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;Ljava/lang/String;)V"));
            }
            case PRIMARY -> {
                switch (e.pe.type) {
                    case NUM -> {
                        cb.ldc(e.pe.num.doubleValue());
                    }
                    case STR -> {
                        cb.ldc(e.pe.str);
                    }
                    case B -> {
                        if (e.pe.b) {
                            cb.iconst_1();
                        } else {
                            cb.iconst_0();
                        }
                    }
                    case ID -> {
                        VarInfo varInfo = varTable.get(e.pe.id.name);
                        if (varInfo != null) {
                            // Use the stored type information
                            switch (varInfo.type.t) {
                                case DOUBLE -> cb.dload(varInfo.slot);
                                case BOOLEAN -> cb.iload(varInfo.slot);
                                case REFERENCE -> {
                                    cb.aload(varInfo.slot);
                                    // Check if this Object needs to be unboxed based on context
                                    // For function parameters, we might need runtime conversion
                                    if (varInfo.type.c.equals(ClassDesc.of("java.lang", "Object"))) {
                                        // This is a function parameter or Object variable
                                        // We'll handle unboxing at the operation level instead of here
                                        // to avoid always unboxing when it might not be needed
                                    }
                                }
                                default -> cb.aload(varInfo.slot);
                            }
                        } else {
                            // Variable not found - push null as fallback
                            cb.aconst_null();
                        }
                    }
                    case NIL -> {
                        cb.aconst_null();
                    }
                }
            }
            case OBJECT -> {
                // Create new SimpleObject
                cb.new_(ClassDesc.of("me.vasan.jimple.SimpleObject"));
                cb.dup();
                cb.invokespecial(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                        "<init>",
                        MethodTypeDesc.ofDescriptor("()V"));
                
                // Set each property
                for (int i = 0; i < e.oe.keys.size(); i++) {
                    cb.dup(); // Keep object reference on stack
                    cb.ldc(e.oe.keys.get(i)); // Push key
                    compileExpr(e.oe.values.get(i), classBuilder, mb, cb); // Push value
                    
                    // Box primitive types if needed
                    TypeInfo valueType = findExprType(e.oe.values.get(i));
                    switch (valueType.t) {
                        case DOUBLE -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf", 
                                    MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                        }
                        case BOOLEAN -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Boolean"), "valueOf", 
                                    MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                        }
                    }
                    
                    // Call SimpleObject.set(String, Object)
                    cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                            "set",
                            MethodTypeDesc.ofDescriptor("(Ljava/lang/String;Ljava/lang/Object;)V"));
                }
            }
            case ARRAY -> {
                // Create new SimpleArray
                cb.new_(ClassDesc.of("me.vasan.jimple.SimpleArray"));
                cb.dup();
                cb.invokespecial(ClassDesc.of("me.vasan.jimple.SimpleArray"),
                        "<init>",
                        MethodTypeDesc.ofDescriptor("()V"));
                
                // Add each element
                for (int i = 0; i < e.ae.elements.size(); i++) {
                    cb.dup(); // Keep array reference on stack
                    compileExpr(e.ae.elements.get(i), classBuilder, mb, cb); // Push value
                    
                    // Box primitive types if needed
                    TypeInfo valueType = findExprType(e.ae.elements.get(i));
                    switch (valueType.t) {
                        case DOUBLE -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf", 
                                    MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                        }
                        case BOOLEAN -> {
                            cb.invokestatic(ClassDesc.of("java.lang.Boolean"), "valueOf", 
                                    MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                        }
                    }
                    
                    // Call SimpleArray.add(Object)
                    cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleArray"),
                            "add",
                            MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
                }
            }
            case PROPERTY_ACCESS -> {
                // Compile object expression
                compileExpr(e.pae.object, classBuilder, mb, cb);
                
                // If the object expression is not a direct variable reference, 
                // we need to cast it to SimpleObject (for chained access like obj.prop.prop2)
                TypeInfo objType = findExprType(e.pae.object);
                if (objType.t == TypeKind.REFERENCE && !objType.c.equals(ClassDesc.of("me.vasan.jimple", "SimpleObject"))) {
                    // Cast Object back to SimpleObject
                    cb.checkcast(ClassDesc.of("me.vasan.jimple.SimpleObject"));
                }
                
                // Push property name
                cb.ldc(e.pae.property);
                
                // Call SimpleObject.get(String)  
                cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                        "get",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Ljava/lang/Object;"));
            }
            case INDEX_ACCESS -> {
                // Compile array expression
                compileExpr(e.iae.object, classBuilder, mb, cb);
                
                // Compile index expression
                compileExpr(e.iae.index, classBuilder, mb, cb);
                
                // Cast index to int
                cb.checkcast(ClassDesc.of("java.lang.Integer"));
                cb.invokevirtual(ClassDesc.of("java.lang.Integer"),
                        "intValue",
                        MethodTypeDesc.ofDescriptor("()I"));
                
                // Call SimpleArray.get(int)
                cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleArray"),
                        "get", 
                        MethodTypeDesc.ofDescriptor("(I)Ljava/lang/Object;"));
            }
            case PROPERTY_ASSIGN -> {
                // Compile object expression
                compileExpr(e.pas.object, classBuilder, mb, cb);
                
                // Push property name
                cb.ldc(e.pas.property);
                
                // Compile value expression
                compileExpr(e.pas.value, classBuilder, mb, cb);
                
                // Box primitive types if needed
                TypeInfo valueType = findExprType(e.pas.value);
                switch (valueType.t) {
                    case DOUBLE -> {
                        cb.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf", 
                                MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                    }
                    case BOOLEAN -> {
                        cb.invokestatic(ClassDesc.of("java.lang.Boolean"), "valueOf", 
                                MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                    }
                }
                
                // Current stack: [obj, property, value]
                // Need: set(obj, property, value) and return value
                // Simple approach: duplicate value and use temporary storage
                
                cb.dup(); // [obj, property, value, value]
                
                // Store copy of value for later return
                int tempSlot = 10; // Use a high slot number to avoid conflicts  
                cb.astore(tempSlot); // [obj, property, value]
                
                // Call set() which consumes [obj, property, value] and returns void
                cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                        "set",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;Ljava/lang/Object;)V"));
                
                // Stack is now empty, load the stored value as expression result
                cb.aload(tempSlot); // [value]
            }
        }
    }

    private void compileStmt(Stmt s, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        switch (s.type) {
            case EXPR_STMT -> {
                compileExpr(s.e.e, classBuilder, mb, cb);
                // Leave result on stack - will be cleaned up by caller
            }

            case IF_STMT -> {
                // Compile condition
                compileExpr(s.i.cond, classBuilder, mb, cb);
                
                // Create labels for else and end
                var elseLabel = cb.newLabel();
                var endLabel = cb.newLabel();
                
                // Jump to else if condition is false (0)
                cb.ifeq(elseLabel);
                
                // Compile then branch
                compileStmt(s.i.then, classBuilder, mb, cb);
                // Pop result only for expression statements that leave values
                if (s.i.then.type == StmtType.EXPR_STMT) {
                    TypeInfo exprType = findExprType(s.i.then.e.e);
                    if (exprType != null && exprType.t == TypeKind.DOUBLE) {
                        cb.pop2();
                    } else {
                        cb.pop();
                    }
                }
                cb.goto_(endLabel);
                
                // Compile else branch (if exists)
                cb.labelBinding(elseLabel);
                if (s.i.alt != null) {
                    compileStmt(s.i.alt, classBuilder, mb, cb);
                    // Pop result only for expression statements that leave values
                    if (s.i.alt.type == StmtType.EXPR_STMT) {
                        TypeInfo exprType = findExprType(s.i.alt.e.e);
                        if (exprType != null && exprType.t == TypeKind.DOUBLE) {
                            cb.pop2();
                        } else {
                            cb.pop();
                        }
                    }
                }
                
                cb.labelBinding(endLabel);
            }

            case WHILE_STMT -> {
                var startLabel = cb.newLabel();
                var endLabel = cb.newLabel();
                
                // Loop start
                cb.labelBinding(startLabel);
                
                // Compile condition
                compileExpr(s.w.cond, classBuilder, mb, cb);
                
                // Exit loop if condition is false
                cb.ifeq(endLabel);
                
                // Compile loop body  
                compileStmt(s.w.then, classBuilder, mb, cb);
                // Pop result only for expression statements that leave values
                if (s.w.then.type == StmtType.EXPR_STMT) {
                    // Determine if we need POP or POP2 based on expression type
                    TypeInfo exprType = findExprType(s.w.then.e.e);
                    if (exprType != null && exprType.t == TypeKind.DOUBLE) {
                        cb.pop2();
                    } else {
                        cb.pop();
                    }
                }
                
                // Jump back to start
                cb.goto_(startLabel);
                
                // Loop end
                cb.labelBinding(endLabel);
            }

            case RETURN_STMT -> {
                if (s.r.expr != null) {
                    compileExpr(s.r.expr, classBuilder, mb, cb);
                    
                    // Box primitive types for Object return
                    TypeInfo returnType = findExprType(s.r.expr);
                    if (returnType != null) {
                        switch (returnType.t) {
                            case DOUBLE -> {
                                cb.invokestatic(ClassDesc.of("java.lang.Double"), 
                                        "valueOf", MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                            }
                            case BOOLEAN -> {
                                cb.invokestatic(ClassDesc.of("java.lang.Boolean"), 
                                        "valueOf", MethodTypeDesc.ofDescriptor("(Z)Ljava/lang/Boolean;"));
                            }
                            // References are already objects
                        }
                    }
                    cb.areturn(); // Return Object reference
                } else {
                    cb.aconst_null();
                    cb.areturn();
                }
            }

            case BLOCK_STMT -> {
                // Create new variable scope for block
                HashMap<String, VarInfo> savedVarTable = new HashMap<>(varTable);
                
                // Compile declarations in block
                for (Decl decl : s.b.decls) {
                    compileDeclaration(decl, classBuilder, mb, cb);
                    
                    // Clean up stack after each expression statement (similar to main call() method)
                    if (decl.type == DeclType.STMT && decl.stmt.type == StmtType.EXPR_STMT) {
                        TypeInfo exprType = findExprType(decl.stmt.e.e);
                        if (exprType != null && exprType.t == TypeKind.DOUBLE) {
                            cb.pop2();
                        } else {
                            cb.pop();
                        }
                    }
                }
                
                // Restore variable scope
                varTable = savedVarTable;
            }
        }
    }

    private void compileVariableDeclaration(Decl d, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        TypeInfo type = findType(d.var);
        int slot = cb.allocateLocal(type.t);
        
        switch (type.t) {
            case BOOLEAN:
                compileExpr(d.var.rvalue, classBuilder, mb, cb);
                cb.istore(slot);
                varTable.put(d.var.id.name, new VarInfo(slot, type));
                break;
            case DOUBLE:
                compileExpr(d.var.rvalue, classBuilder, mb, cb);
                cb.dstore(slot);
                varTable.put(d.var.id.name, new VarInfo(slot, type));
                break;
            case REFERENCE:
                if (d.var.rvalue.pe != null) {
                    cb.ldc(d.var.rvalue.pe.str);
                    cb.astore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot, type));
                }
                else if (d.var.rvalue.ce != null) {
                    compileCallExpr(d.var, d.var.rvalue.ce, classBuilder, mb, cb);
                }
                else if (d.var.rvalue.oe != null) {
                    // Handle object literal assignment
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.astore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot, type));
                }
                else if (d.var.rvalue.ae != null) {
                    // Handle array literal assignment
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.astore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot, type));
                }
                else if (d.var.rvalue.pae != null) {
                    // Handle property access assignment
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.astore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot, type));
                }
                else if (d.var.rvalue.ue != null) {
                    // Handle unary expression assignment - allocate correct slot type
                    TypeInfo actualType = findExprType(d.var.rvalue);
                    int actualSlot = cb.allocateLocal(actualType.t);
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    if (actualType.t == TypeKind.DOUBLE) {
                        cb.dstore(actualSlot);
                    } else {
                        cb.astore(actualSlot);
                    }
                    varTable.put(d.var.id.name, new VarInfo(actualSlot, actualType));
                }
                else {
                    // Generic expression assignment
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.astore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot, type));
                }
                break;
            case VOID:
                break;
        }
    }

    private void compileDeclaration(Decl d, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        if (d.type == DeclType.VAR) {
            compileVariableDeclaration(d, classBuilder, mb, cb);
        }
        else if (d.type == DeclType.STMT) {
            compileStmt(d.stmt, classBuilder, mb, cb);
        }
    }

    private void collectFunctionExpressions(Decl d) {
        if (d.type == DeclType.VAR && d.var.rvalue != null) {
            collectFunctionExpressionsFromExpr(d.var.rvalue);
        } else if (d.type == DeclType.STMT && d.stmt != null) {
            collectFunctionExpressionsFromStmt(d.stmt);
        }
    }
    
    private void collectFunctionExpressionsFromExpr(Expr e) {
        if (e == null) return;
        
        switch (e.type) {
            case FUNCTION -> {
                functionExpressions.add(e.fe);
            }
            case ASSIGN_EXPR -> {
                if (e.a != null) {
                    collectFunctionExpressionsFromExpr(e.a.e);
                }
            }
            case BINARY_EXPR -> {
                if (e.b != null) {
                    collectFunctionExpressionsFromExpr(e.b.lhs);
                    collectFunctionExpressionsFromExpr(e.b.rhs);
                }
            }
            case UNARY -> {
                if (e.ue != null) {
                    collectFunctionExpressionsFromExpr(e.ue.e);
                }
            }
            case CALL -> {
                if (e.ce != null) {
                    collectFunctionExpressionsFromExpr(e.ce.object);
                    for (Expr arg : e.ce.a) {
                        collectFunctionExpressionsFromExpr(arg);
                    }
                }
            }
            case OBJECT -> {
                if (e.oe != null) {
                    for (Expr value : e.oe.values) {
                        collectFunctionExpressionsFromExpr(value);
                    }
                }
            }
            case ARRAY -> {
                if (e.ae != null) {
                    for (Expr elem : e.ae.elements) {
                        collectFunctionExpressionsFromExpr(elem);
                    }
                }
            }
            case PROPERTY_ACCESS -> {
                if (e.pae != null) {
                    collectFunctionExpressionsFromExpr(e.pae.object);
                }
            }
            case INDEX_ACCESS -> {
                if (e.iae != null) {
                    collectFunctionExpressionsFromExpr(e.iae.object);
                    collectFunctionExpressionsFromExpr(e.iae.index);
                }
            }
            case PROPERTY_ASSIGN -> {
                if (e.pas != null) {
                    collectFunctionExpressionsFromExpr(e.pas.object);
                    collectFunctionExpressionsFromExpr(e.pas.value);
                }
            }
        }
    }
    
    private void collectFunctionExpressionsFromStmt(Stmt s) {
        if (s == null) return;
        
        switch (s.type) {
            case EXPR_STMT -> {
                collectFunctionExpressionsFromExpr(s.e.e);
            }
            case BLOCK_STMT -> {
                if (s.b != null) {
                    for (Decl d : s.b.decls) {
                        collectFunctionExpressions(d);
                    }
                }
            }
            case IF_STMT -> {
                if (s.i != null) {
                    collectFunctionExpressionsFromExpr(s.i.cond);
                    collectFunctionExpressionsFromStmt(s.i.then);
                    collectFunctionExpressionsFromStmt(s.i.alt);
                }
            }
            case WHILE_STMT -> {
                if (s.w != null) {
                    collectFunctionExpressionsFromExpr(s.w.cond);
                    collectFunctionExpressionsFromStmt(s.w.then);
                }
            }
            case RETURN_STMT -> {
                if (s.r != null) {
                    collectFunctionExpressionsFromExpr(s.r.expr);
                }
            }
        }
    }

    private void writeClass(List<Decl> decls) throws IOException {
        fPgmClassFile.buildTo(Path.of(fPath.split("\\.sim")[0] + ".class"), fPgmClass, classBuilder -> {
            // Implement Callable interface
            classBuilder.withInterfaceSymbols(ClassDesc.of("me.vasan.jimple.Callable"));
            
            // Generate constructor
            classBuilder.withMethod("<init>",
                    MethodTypeDesc.ofDescriptor("()V"),
                    ACC_PUBLIC,
                    methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            codeBuilder.aload(0); // Load 'this'
                            codeBuilder.invokespecial(ClassDesc.of("java.lang.Object"), 
                                    "<init>", MethodTypeDesc.ofDescriptor("()V"));
                            codeBuilder.return_();
                        });
                    });
            
            // Pre-populate methodTable with all function declarations
            for (var d : decls) {
                if (d.type == DeclType.VAR && d.var.rvalue.fe != null) {
                    FunctionExpr f = d.var.rvalue.fe;
                    String mDes = findMethodDescriptor(f);
                    methodTable.put(d.var.id.name, new MethodInfo(methCounter++, mDes));
                }
            }
            
            // Reset methCounter for actual method generation
            methCounter = 0;
            
            // Generate user-defined function methods first
            for (var d : decls) {
                if (d.type == DeclType.VAR && d.var.rvalue.fe != null) {
                    FunctionExpr f = d.var.rvalue.fe;
                    String mDes = findMethodDescriptor(f);
                    classBuilder.withMethod("meth" + methCounter,
                            MethodTypeDesc.ofDescriptor(mDes),
                            ACC_PUBLIC,
                            mB -> {
                                mB.withCode(cb2 -> {
                                    // Create new variable scope for function
                                    HashMap<String, VarInfo> savedVarTable = new HashMap<>(varTable);
                                    
                                    // Map function parameters to local variables
                                    // Slot 0 is 'this', parameters start at slot 1
                                    for (int i = 0; i < f.a.size(); i++) {
                                        String paramName = f.a.get(i).name;
                                        // Object parameters take 1 slot each: slot 1, 2, 3, etc.
                                        varTable.put(paramName, new VarInfo(1 + i, new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"))));
                                    }
                                    
                                    // Compile function body
                                    compileStmt(f.b, classBuilder, mB, cb2);
                                    
                                    // If no explicit return, return null
                                    cb2.aconst_null();
                                    cb2.areturn();
                                    
                                    // Restore variable scope
                                    varTable = savedVarTable;
                                });
                            });
                    methCounter++;
                }
            }
            
            // First pass: collect function expressions by processing declarations
            for (var d : decls) {
                collectFunctionExpressions(d);
            }
            
            // Generate methods for function expressions
            for (int i = 0; i < functionExpressions.size(); i++) {
                FunctionExpr f = functionExpressions.get(i);
                String methodName = "funcExpr" + i;
                String mDes = findMethodDescriptor(f);
                
                classBuilder.withMethod(methodName,
                        MethodTypeDesc.ofDescriptor(mDes),
                        ACC_PUBLIC,
                        mB -> {
                            mB.withCode(cb2 -> {
                                // Create new variable scope for function
                                HashMap<String, VarInfo> savedVarTable = new HashMap<>(varTable);
                                
                                // Map function parameters to local variables
                                // Slot 0 is 'this', parameters start at slot 1
                                for (int j = 0; j < f.a.size(); j++) {
                                    String paramName = f.a.get(j).name;
                                    // Object parameters take 1 slot each: slot 1, 2, 3, etc.
                                    varTable.put(paramName, new VarInfo(1 + j, new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"))));
                                }
                                
                                // Compile function body
                                compileStmt(f.b, classBuilder, mB, cb2);
                                
                                // If no explicit return, return null
                                cb2.aconst_null();
                                cb2.areturn();
                                
                                // Restore variable scope
                                varTable = savedVarTable;
                            });
                        });
            }
            
            // Generate call() method - implements Callable interface
            classBuilder.withMethod("call",
                    MethodTypeDesc.ofDescriptor("()Ljava/lang/Object;"),
                    ACC_PUBLIC,
                    methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            // Process each declaration/statement
                            for (var d : decls) {
                                if (d.type == DeclType.VAR) {
                                    // Skip function declarations - already handled above
                                    if (d.var.rvalue.fe != null) {
                                        continue;
                                    }
                                    // Handle variable declarations
                                    compileVariableDeclaration(d, classBuilder, methodBuilder, codeBuilder);
                                } else if (d.type == DeclType.STMT) {
                                    compileStmt(d.stmt, classBuilder, methodBuilder, codeBuilder);
                                    
                                    // Clean up stack after each expression statement
                                    if (d.stmt.type == StmtType.EXPR_STMT) {
                                        TypeInfo exprType = findExprType(d.stmt.e.e);
                                        if (exprType != null && exprType.t == TypeKind.DOUBLE) {
                                            codeBuilder.pop2();
                                        } else {
                                            codeBuilder.pop();
                                        }
                                    }
                                }
                            }
                            
                            // Return null
                            codeBuilder.aconst_null();
                            codeBuilder.areturn();
                        });
                    });
            
            // Generate main method for standalone execution
            classBuilder.withMethod("main",
                    MethodTypeDesc.ofDescriptor("([Ljava/lang/String;)V"),
                    ACC_PUBLIC + ACC_STATIC,
                    methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            // Create instance and call it
                            codeBuilder.new_(fPgmClass);
                            codeBuilder.dup();
                            codeBuilder.invokespecial(fPgmClass, "<init>", 
                                    MethodTypeDesc.ofDescriptor("()V"));
                            codeBuilder.invokevirtual(fPgmClass, "call", 
                                    MethodTypeDesc.ofDescriptor("()Ljava/lang/Object;"));
                            
                            // For REPL classes, print the result
                            if (fName.startsWith("REPLExpr")) {
                                codeBuilder.dup();
                                var nullLabel = codeBuilder.newLabel();
                                var endLabel = codeBuilder.newLabel();
                                codeBuilder.ifnull(nullLabel);
                                
                                codeBuilder.getstatic(ClassDesc.of("java.lang", "System"), "out", 
                                        ClassDesc.of("java.io", "PrintStream"));
                                codeBuilder.swap();
                                codeBuilder.invokevirtual(ClassDesc.of("java.io", "PrintStream"),
                                        "println", MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
                                codeBuilder.goto_(endLabel);
                                
                                codeBuilder.labelBinding(nullLabel);
                                codeBuilder.pop();
                                
                                codeBuilder.labelBinding(endLabel);
                            } else {
                                codeBuilder.pop(); // Discard result for non-REPL
                            }
                            
                            codeBuilder.return_();
                        });
                    });
        });
    }
}
