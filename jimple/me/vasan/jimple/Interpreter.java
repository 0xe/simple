package me.vasan.jimple;

import java.util.ArrayList;

public class Interpreter {
    Environment root;

    public Interpreter(Environment env) {
        root = env;
    }

    public Object interpret(Pgm ast) throws RuntimeError {
        return interpret(ast, root);
    }

    public Object interpret(Pgm ast, Environment env) throws RuntimeError {
        Object ret = null;
        if (ast instanceof Pgm)
            for (Decl decl: ast.decls) {
                ret = interpret(decl, env);
            }
        return ret;
    }

    /* 
     * Do we need separate logic for declarations and statements?
     */
    public Object interpret(Decl d, Environment env) throws RuntimeError {
        if (d.type == DeclType.VAR) {
            Var v = d.var;
            env.put(v.id.name, interpret(v.rvalue, env));
            return null;
        } else if (d.type == DeclType.STMT) {
            return interpret(d.stmt, env);
        } else {
            return null;
        }
    }

    public Object interpret(Stmt s, Environment env) throws RuntimeError {
        switch (s.type) { // ExprStmt's don't return any values
            case EXPR_STMT: { 
                return interpret(s.e.e, env); 
            }
            case IF_STMT: {
                Boolean cond = (Boolean) interpret(s.i.cond, env);
                if (cond) {
                    return interpret(s.i.then, env);
                } else {
                    if (s.i.alt != null)
                        return interpret(s.i.alt, env);
                    else {
                        return false;
                    }
                }
            }
            case WHILE_STMT: {
                Object val = null;
                while ((Boolean) interpret(s.w.cond, env)) {
                    val = interpret(s.w.then, env);
                }
                return val;
            }
            case RETURN_STMT: {
                return interpret(s.r.expr, env);
            }
            case BLOCK_STMT: { // very similar to interpret(pgm)
                Environment block = new Environment(env);
                Object ret = null;
                for (Decl decl: s.b.decls) {
                    ret = interpret(decl, block);
                }
                return ret;
            }
            default:
                return null;
        }
    }

    public Object interpret(Expr e, Environment env) throws RuntimeError {
        switch (e.type) {
            case PRIMARY:
                switch (e.pe.type) {
                    case NUM:
                        return e.pe.num;
                    case B:
                        return e.pe.b;
                    case NIL:
                        return null;
                    case STR:
                        return e.pe.str;
                    case ID: 
                        return env.get(e.pe.id.name);
                }
            case BINARY_EXPR:
                Object lhs = interpret(e.b.lhs, env);
                Object rhs = interpret(e.b.rhs, env);
                switch (e.b.o) {
                    case PLUS:
                        if (lhs instanceof String) return lhs + ((String) rhs);
                        return ((Number) lhs).doubleValue() + ((Number) rhs).doubleValue();
                    case MIN:
                        return ((Number) lhs).doubleValue() - ((Number) rhs).doubleValue();
                    case MUL:
                        return ((Number) lhs).doubleValue() * ((Number) rhs).doubleValue();
                    case DIV:
                        return ((Number) lhs).doubleValue() / ((Number) rhs).doubleValue();
                    case EQ: // TODO: UGH?
                        if (lhs instanceof Number)
                            return (((Number) lhs).doubleValue()) == (((Number) rhs).doubleValue());
                        else if (lhs instanceof Boolean)
                            return (Boolean) lhs == (Boolean) rhs;
                        else 
                            return lhs == rhs;
                    case NEQ: // TODO
                        if (lhs instanceof Number)
                            return (((Number) lhs).doubleValue()) != (((Number) rhs).doubleValue());
                        else if (lhs instanceof Boolean)
                            return (Boolean) lhs == (Boolean) rhs;
                        else 
                            return lhs == rhs;
                    case GT:
                        return (((Number) lhs).doubleValue()) > (((Number) rhs).doubleValue());
                    case GTE:
                        return (((Number) lhs).doubleValue()) >= (((Number) rhs).doubleValue());
                    case LT:
                        return (((Number) lhs).doubleValue()) < (((Number) rhs).doubleValue());
                    case LTE:
                        return (((Number) lhs).doubleValue()) <= (((Number) rhs).doubleValue());
                    case LOR:
                        return (Boolean) lhs || (Boolean) rhs;
                    case LAN:
                        return (Boolean) lhs && (Boolean) rhs;
                    default:
                        throw new RuntimeError("interpret(expr, env)");
                }
            case UNARY:
                switch (e.ue.o) {
                    case NEG:
                        return -1 * ((Number) interpret(e.ue.e, env)).doubleValue();
                    case NOT:
                        return !(Boolean) interpret(e.ue.e, env);
                    default:
                        throw new RuntimeError("interpret(expr, env)");
                }
            case CALL: {
                Environment fnEnv = new Environment(env);
                Object fnRef = env.get(e.ce.id.name);
                if (fnRef instanceof FunctionExpr) {
                    FunctionExpr fe = (FunctionExpr) fnRef;

                    /* set arguments */
                    for (int i = 0; i < fe.a.size(); i++) {
                        fnEnv.put(fe.a.get(i).name, interpret(e.ce.a.get(i), env));
                    }
                    return interpret(fe.b, fnEnv);                
                }
                else if (fnRef instanceof NativeFunction) {
                    NativeFunction fn = (NativeFunction) fnRef;
                    
                    /* set arguments */
                    ArrayList<Object> args = new ArrayList<Object>();
                    for (int i = 0; i < e.ce.a.size(); i++) {
                        args.add(interpret(e.ce.a.get(i), env));
                    }
                    return fn.call(args.toArray());
                }
            }
            case FUNCTION: { // LOL: why not drop the whole node in there? ;-)
                return e.fe;
            }
            case ASSIGN_EXPR:
                Object val = interpret(e.a.e, env);
                env.update(e.a.id.name, val);
                return val;
            default:
                return null;
        }
    }
}
