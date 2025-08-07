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

    /* this should be per method as slots _WILL_ be reused! */
    HashMap<String, VarInfo> varTable = null;

    record TypeInfo(TypeKind t, ClassDesc c) {
    };

    record MethodInfo(int counter, String sig) {
    };

    record VarInfo(int slot) {
    };

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
                switch (e.ue.o) {
                    case NEG -> {
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
                    }
                    case NOT -> {
                        return new TypeInfo(TypeKind.BOOLEAN, ClassDesc.of("boolean"));
                    }
                }
            }
            case CALL -> {
                // Function calls return objects by default (need better inference)
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
                        // For now, assume double
                        return new TypeInfo(TypeKind.DOUBLE, ClassDesc.of("double"));
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
            case PROPERTY_ACCESS -> {
                return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object"));
            }
        }
        return new TypeInfo(TypeKind.REFERENCE, ClassDesc.of("java.lang", "Object")); // default
    }

    private String findMethodDescriptor(FunctionExpr f) {
        return "(DD)D"; // TODO: pick arg types from f
    }

    private void compileCallExpr(Var v, CallExpr ce, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        MethodInfo m = methodTable.get(ce.id.name);
        if (m != null) {
            // Compile arguments and push onto stack
            for (Expr arg : ce.a) {
                compileExpr(arg, classBuilder, mb, cb);
            }
            
            // Call the user-defined function
            cb.invokestatic(ClassDesc.of(fName), "meth" + m.counter, MethodTypeDesc.ofDescriptor(m.sig));
            
            // Store result if this is an assignment
            if (v != null) {
                TypeInfo type = findExprType(new Expr(ce));
                int slot = cb.allocateLocal(type.t);
                
                switch (type.t) {
                    case DOUBLE -> cb.dstore(slot);
                    case BOOLEAN -> cb.istore(slot);
                    case REFERENCE -> cb.astore(slot);
                    default -> cb.astore(slot);
                }
                
                varTable.put(v.id.name, new VarInfo(slot));
            }
        } else { // special behaviour for "print" and "clock"?
            if (ce.id.name.equals("print")) {
                cb.new_(ClassDesc.of("NativeFunction"))
                        .dup()
                        .ldc("print")
                        .invokespecial(ClassDesc.of("NativeFunction"),
                                "<init>",
                                MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"))
                        .iconst_2() // TODO: pick the iconst based on ce.a.length()
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
                    TypeInfo rhsType = findExprType(e.a.e);
                    switch (rhsType.t) {
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
                // Load left operand
                compileExpr(e.b.lhs, classBuilder, mb, cb);
                
                // Load right operand  
                compileExpr(e.b.rhs, classBuilder, mb, cb);
                
                // Apply operation
                switch (e.b.o) {
                    case PLUS -> cb.dadd();
                    case MIN -> cb.dsub();
                    case MUL -> cb.dmul();
                    case DIV -> cb.ddiv();
                    case EQ -> {
                        // Compare doubles: dcmpl, ifeq
                        cb.dcmpl();
                        cb.iconst_1(); // true
                        cb.iconst_0(); // false
                        // TODO: Implement proper comparison logic
                    }
                    case NEQ, GT, GTE, LT, LTE -> {
                        // TODO: Implement comparison operations
                        cb.dcmpl();
                    }
                    case LOR -> {
                        // TODO: Implement logical OR
                    }
                    case LAN -> {
                        // TODO: Implement logical AND  
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
                        // TODO: Implement boolean NOT
                    }
                }
            }
            case CALL -> {
                compileCallExpr(null, e.ce, classBuilder, mb, cb);
            }
            case FUNCTION -> {
                // Function expressions are handled during declaration
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
                            // Need to infer type from context - for now assume double
                            // TODO: Store type info in VarInfo
                            cb.dload(varInfo.slot);
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
            case PROPERTY_ACCESS -> {
                // Compile object expression
                compileExpr(e.pae.object, classBuilder, mb, cb);
                
                // Push property name
                cb.ldc(e.pae.property);
                
                // Call SimpleObject.get(String)  
                cb.invokevirtual(ClassDesc.of("me.vasan.jimple.SimpleObject"),
                        "get",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)Ljava/lang/Object;"));
            }
        }
    }

    private void compileStmt(Stmt s, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        switch (s.type) {
            case EXPR_STMT -> {
                compileExpr(s.e.e, classBuilder, mb, cb);
                // Pop the result since expression statements discard values
                cb.pop();
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
                cb.goto_(endLabel);
                
                // Compile else branch (if exists)
                cb.labelBinding(elseLabel);
                if (s.i.alt != null) {
                    compileStmt(s.i.alt, classBuilder, mb, cb);
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
                
                // Jump back to start
                cb.goto_(startLabel);
                
                // Loop end
                cb.labelBinding(endLabel);
            }

            case RETURN_STMT -> {
                if (s.r.expr != null) {
                    compileExpr(s.r.expr, classBuilder, mb, cb);
                    // TODO: Choose correct return instruction based on type
                    cb.dreturn(); // Assuming double for now
                } else {
                    cb.return_();
                }
            }

            case BLOCK_STMT -> {
                // Create new variable scope for block
                HashMap<String, VarInfo> savedVarTable = new HashMap<>(varTable);
                
                // Compile declarations in block
                for (Decl decl : s.b.decls) {
                    compileDeclaration(decl, classBuilder, mb, cb);
                }
                
                // Restore variable scope
                varTable = savedVarTable;
            }
        }
    }

    private void compileDeclaration(Decl d, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        if (d.type == DeclType.VAR) {
            TypeInfo type = findType(d.var);
            int slot = cb.allocateLocal(type.t);
            cb.localVariable(slot, d.var.id.toString(), type.c,
                    cb.startLabel(), cb.endLabel());
            switch (type.t) {
                case BOOLEAN:
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.istore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot));
                    break;
                case DOUBLE:
                    compileExpr(d.var.rvalue, classBuilder, mb, cb);
                    cb.dstore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot));
                    break;
                case REFERENCE:
                    if (d.var.rvalue.fe != null) {
                        FunctionExpr f = d.var.rvalue.fe;
                        String mDes = findMethodDescriptor(f);
                        classBuilder.withMethod("meth" + methCounter,
                                MethodTypeDesc.ofDescriptor(mDes),
                                ACC_PUBLIC | ACC_STATIC,
                                mB -> {
                                    mB.withCode(cb2 -> {
                                        cb2.dload(0);
                                        cb2.dload(2);
																		/* we're so smart we inferred what the code was
																		doing and replaced it with std. lib function */
                                        cb2.invokestatic(ClassDesc.of("java.lang.Math"),
                                                "pow",
                                                MethodTypeDesc.ofDescriptor("(DD)D"));
                                        int slot4 = cb2.allocateLocal(TypeKind.DOUBLE);
                                        cb2.dstore(slot4);
                                        cb2.dload(slot4);
                                        cb2.dreturn();
                                    });
                                });
                        methodTable.put(d.var.id.name, new MethodInfo(methCounter++, mDes));
                    }
                    else if (d.var.rvalue.pe != null) {
                        cb.ldc(d.var.rvalue.pe.str);
                        cb.astore(slot);
                        varTable.put(d.var.id.name, new VarInfo(slot));
                    }
                    else if (d.var.rvalue.ce != null) {
                        CallExpr ce = d.var.rvalue.ce;
                        compileCallExpr(d.var, d.var.rvalue.ce, classBuilder, mb, cb);
                    }
                    break;
                case VOID:
                    break;
            }
        }
        else if (d.type == DeclType.STMT) { /* TODO */
            compileStmt(d.stmt, classBuilder, mb, cb);
        }
    }

    private void writeClass(List<Decl> decls) throws IOException {
        fPgmClassFile.buildTo(Path.of(fPath.split("\\.sim")[0] + ".class"), fPgmClass, classBuilder -> {
            classBuilder.withMethod("main",
                    MethodTypeDesc.ofDescriptor("([Ljava/lang/String;)V"),
                    ACC_PUBLIC + ACC_STATIC,
                    methodBuilder -> {
                        methodBuilder.withCode(codeBuilder -> {
                            for (var d : decls) {
                                compileDeclaration(d, classBuilder, methodBuilder, codeBuilder);
                            }
                            codeBuilder.return_();
                        });
                    });
        });
    }
}
