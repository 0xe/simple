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
        switch (v.rvalue.type) {
            case ASSIGN_EXPR, BINARY_EXPR, UNARY, CALL -> {
                // TODO: infer type from expression?
            }
            case FUNCTION -> {
                return new TypeInfo(TypeKind.ReferenceType,
                        ClassDesc.of("me.vasan.jimple", "FunctionExpr"));
            }
            case PRIMARY -> {
                PrimaryExpr p = v.rvalue.pe;
                switch (p.type) {
                    case ID -> {
                        // TODO: identifier type should be resolved
                    }
                    case NUM -> {
                        return new TypeInfo(TypeKind.DoubleType, ClassDesc.of("double"));
                    }
                    case STR -> {
                        return new TypeInfo(TypeKind.ReferenceType, ClassDesc.of("java.lang", "String"));
                    }
                    case B -> {
                        return new TypeInfo(TypeKind.BooleanType, ClassDesc.of("boolean"));
                    }
                    case NIL -> {
                        return new TypeInfo(TypeKind.VoidType, ClassDesc.of("null"));
                    }
                }
            }
        }
        return new TypeInfo(TypeKind.ReferenceType, ClassDesc.of("java.lang", "Reference")); // default
    }

    private String findMethodDescriptor(FunctionExpr f) {
        return "(DD)D"; // TODO: pick arg types from f
    }

    private void compileCallExpr(Var v, CallExpr ce, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        MethodInfo m = methodTable.get(ce.id.name);
        if (m != null) {
            // TODO: push the right arguments and type the return slot based on the descriptor
            int slot3 = cb.allocateLocal(TypeKind.DoubleType);
            for (var ei : ce.a) {
                cb.ldc(ei.pe.num.doubleValue()); /* TODO: should eval expression here! */
            }
            cb.invokestatic(ClassDesc.of(fName), STR."meth\{m.counter}", MethodTypeDesc.ofDescriptor(m.sig));
            cb.dstore(slot3); // store returned value back to the assigned var
            if (v != null)
                varTable.put(v.id.name, new VarInfo(slot3));
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
                    /* TODO: only PrimaryExpr with Id for now */
                    if (varTable.get(ai.pe.id.name) != null)
                        /* TODO: infer types!! ha ha ha */
                        if (ai.pe.id.name.endsWith("msg"))
                            cb.aload(varTable.get(ai.pe.id.name).slot);
                        else {
                            cb.dload(varTable.get(ai.pe.id.name).slot);
                            cb.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf", MethodTypeDesc.ofDescriptor("(D)Ljava/lang/Double;"));
                        }
                    cb.aastore();
                }
                cb.invokevirtual(ClassDesc.of("NativeFunction"),
                                "call",
                                MethodTypeDesc.ofDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;"))
                        .pop();
            } else if (ce.id.name.equals("clock")) {

            } else { /* TODO: error handling */
                throw new Error("bad call function");
            }
        }
    }

    private void compileExpr(Expr e, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        switch (e.type) {
            case ASSIGN_EXPR -> {

            }
            case BINARY_EXPR -> {

            }
            case UNARY -> {

            }
            case CALL -> { // TODO: wtf?
                compileCallExpr(null, e.ce, classBuilder, mb, cb);
            }
            case FUNCTION -> {

            }
            case PRIMARY -> {

            }
        }
    }

    private void compileStmt(Stmt s, ClassBuilder classBuilder, MethodBuilder mb, CodeBuilder cb) {
        switch (s.type) {
            case EXPR_STMT -> {
                compileExpr(s.e.e, classBuilder, mb, cb);
            }

            case IF_STMT -> {
            }

            case WHILE_STMT -> {
            }

            case RETURN_STMT -> {
            }

            case BLOCK_STMT -> {
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
                case BooleanType:
                    cb.bipush(d.var.rvalue.pe.b ? 1 : 0);
                    cb.istore(slot); // TODO: overflow check in case of short type
                    varTable.put(d.var.id.name, new VarInfo(slot));
                    break;
                case DoubleType:
                    cb.ldc(d.var.rvalue.pe.num.doubleValue());
                    cb.dstore(slot);
                    varTable.put(d.var.id.name, new VarInfo(slot));
                    break;
                case ReferenceType:
                    if (d.var.rvalue.fe != null) {
                        FunctionExpr f = d.var.rvalue.fe;
                        String mDes = findMethodDescriptor(f);
                        classBuilder.withMethod(STR."meth\{methCounter}",
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
                                        int slot4 = cb2.allocateLocal(TypeKind.DoubleType);
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
                case VoidType:
                    break;
            }
        }
        else if (d.type == DeclType.STMT) { /* TODO */
            compileStmt(d.stmt, classBuilder, mb, cb);
        }
    }

    private void writeClass(List<Decl> decls) throws IOException {
        fPgmClassFile.buildTo(Path.of(STR."\{fPath.split(".sim")[0]}.class"), fPgmClass, classBuilder -> {
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
