package me.vasan.jimple;

import java.util.ArrayList;
import java.util.List;

enum DeclType {
    VAR, STMT
};

enum StmtType {
    EXPR_STMT, IF_STMT, WHILE_STMT, RETURN_STMT, BLOCK_STMT
};

enum ExprType {
    ASSIGN_EXPR, BINARY_EXPR, UNARY, FUNCTION, CALL, PRIMARY
};

enum PrimaryType {ID, NUM, STR, B, NIL};

enum Op {
    LOR, LAN, 
    EQ, NEQ, GT, GTE, LT, LTE,
    MIN, PLUS, MUL, DIV, NOT, NEG
};

class Node {
    int fline;
    int fcol;
    int tline;
    int tcol;

    void span(int fl, int fc, int tl, int tc) {
        this.fline = fl;
        this.fcol = fc;
        this.tline = tl;
        this.tcol = tc;
    }

    int[] span() {
        int[] sp = { fline, fcol, tline, tcol };
        return sp;
    }
}

class Pgm extends Node {
    List<Decl> decls;
}

class Decl extends Node {
    DeclType type;
    Var var;
    Stmt stmt;

    public String toString() {
        if (type == DeclType.VAR) {
            return var.toString();
        } else {
            return stmt.toString();
        }
    }

    Decl(Stmt s) {
        this.stmt = s;
        this.type = DeclType.STMT;
    }

    Decl(Var v) {
        this.var = v;
        this.type = DeclType.VAR;
    }
}

class Var extends Node {
    Id id;
    Expr rvalue;

    public String toString() {
        return String.format("<Var> Id: %s, Expr: %s", id, rvalue);
    }

    Var(Id id) {
        this.id = id;
        this.rvalue = null;
        span(id.fline, id.fcol, id.fline, id.fcol);
    }

    Var(Id id, Expr rvalue) {
        this.id = id;
        this.rvalue = rvalue;
        span(id.fline, id.fcol, rvalue.fline, rvalue.fcol);
    }
}

class Id extends Node {
    String name;

    Id(String str, int line, int col) {
        this.name = str;
        span(line, col, line, col);
    }

    public String toString() {
        return String.format("<Id> Name: %s", name);
    }
}

/*
 * TODO: Add line, col to everything below
 */
class Stmt extends Node {
    ExprStmt e;
    IfStmt i;
    WhileStmt w;
    ReturnStmt r;
    BlockStmt b;
    StmtType type;

    Stmt(ExprStmt e) {
        this.type = StmtType.EXPR_STMT; this.e = e;
    }

    Stmt(IfStmt e) {
        this.type = StmtType.IF_STMT; this.i = e;
    }

    Stmt(WhileStmt e) {
        this.type = StmtType.WHILE_STMT; this.w = e;
    }

    Stmt(ReturnStmt e) {
        this.type = StmtType.RETURN_STMT; this.r = e;
    }
    
    Stmt(BlockStmt e) {
        this.type = StmtType.BLOCK_STMT; this.b = e;
    }

    public String toString() {
        String child;
        switch (this.type) {
            case EXPR_STMT: child = e.toString(); break;
            case IF_STMT: child = i.toString(); break;
            case WHILE_STMT: child = w.toString(); break;
            case RETURN_STMT: child = r.toString(); break;
            case BLOCK_STMT: child = b.toString(); break;
            default:
                return child = type.toString();
        }

        return String.format("<Stmt> %s, Child expr: %s", type, child);
    }
}

class ExprStmt extends Node {
    Expr e;

    ExprStmt(Expr e) {
        this.e = e;
    }

    public String toString() {
        return e.toString();
    }
}

class IfStmt extends Node {
    Expr cond;
    Stmt then;
    Stmt alt;

    IfStmt(Expr cond, Stmt then) {
        this.cond = cond;
        this.then = then;
    }

    IfStmt(Expr cond, Stmt then, Stmt alt) {
        this.cond = cond;
        this.then = then;
        this.alt = alt;
    }

    public String toString() {
        return String.format("Cond: %s\n Then: %s\n Alt: %s", cond.toString(), then.toString(), alt.toString());
    }
}

class WhileStmt extends Node {
    Expr cond;
    Stmt then;

    WhileStmt(Expr cond, Stmt then) {
        this.cond = cond;
        this.then = then;
    }

    public String toString() {
        return String.format("Cond: %s\n Then: %s", cond.toString(), then.toString());
    }
}

class ReturnStmt extends Node {
    Expr expr;

    ReturnStmt() {}

    ReturnStmt(Expr ex) {
        this.expr = ex;
    }

    public String toString() {
        return String.format("Expr: %s", expr.toString());
    }
}

// TODO: This is the exact same as Pgm. Why have two?
class BlockStmt extends Node {
    List<Decl> decls;

    BlockStmt(List<Decl> decls) {
        this.decls = decls;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<BlockStmt>: Decls: \n"));
        for (Decl d: decls) {
            sb.append("<Decl> ");
            sb.append(d.toString());
        }
        return sb.toString();
    }
}

/*
 * This was a slog to write. No wonder craftinginterpreters generates this.
 * But: now i understand why.
 */
class Expr extends Node {
    AssignExpr a;
    BinaryExpr b;
    UnaryExpr ue;
    FunctionExpr fe;
    CallExpr ce;
    PrimaryExpr pe;

    ExprType type;

    Expr() {
        
    }

    Expr(AssignExpr e) {
        this.a = e; this.type = ExprType.ASSIGN_EXPR;
    }

    Expr(BinaryExpr e) {
        this.b = e; this.type = ExprType.BINARY_EXPR;
    }

    Expr(UnaryExpr e) {
        this.ue = e; this.type = ExprType.UNARY;
    }

    Expr(FunctionExpr e) {
        this.fe = e; this.type = ExprType.FUNCTION;
    }

    Expr(CallExpr e) {
        this.ce = e; this.type = ExprType.CALL;
    }

    Expr(PrimaryExpr e) {
        this.pe = e; this.type = ExprType.PRIMARY;
    }
}

class AssignExpr {
    Id id;
    Expr e; 

    AssignExpr(Id id, Expr e) {
        this.id = id; this.e = e;        
    }
}

/* LogicOr, LogicAnd, Equality, Comparison, Term, Factor, ... */
class BinaryExpr {
    Expr lhs;
    Expr rhs;
    Op o;

    BinaryExpr(Expr lhs, Expr rhs, Op o) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.o = o;
    }
}

class UnaryExpr {
    Op o;
    Expr e; 

    UnaryExpr(Expr e, Op o) {
        this.e = e; this.o = o;
    }
}

class FunctionExpr {
    ArrayList<Id> a;
    Stmt b;

    FunctionExpr(ArrayList<Id> a, Stmt b) {
        this.a = a; this.b = b;
    }
}

class CallExpr {
    Id id;
    ArrayList<Expr> a;

    CallExpr(Id id, ArrayList<Expr> a) {
        this.id = id; this.a = a;
    }
}

class PrimaryExpr {
    Id id;
    Number num;
    String str;
    Boolean b;
    Object nil;
    PrimaryType type;

    PrimaryExpr(Token t) throws SyntaxError {
        switch (t.type) {
            case TRUE: this.b = true; this.type = PrimaryType.B; break;
            case FALSE: this.b = false; this.type = PrimaryType.B; break;
            case NIL: this.nil = null; this.type = PrimaryType.NIL; break;
            case NUMBER: this.num = Double.parseDouble(t.lexeme); this.type = PrimaryType.NUM; break;
            case STRING: this.str = t.lexeme; this.type = PrimaryType.STR; break;
            case IDENTIFIER: this.id = new Id(t.lexeme, t.line, t.charPos); this.type = PrimaryType.ID; break;
            default: throw new SyntaxError("parse_primary_expr()");
        }
    }

    public String toString() {
        String child;
        switch (type) {
            case B: child = b.toString(); break;
            case NIL: child = nil.toString(); break;
            case NUM: child = num.toString(); break;
            case STR: child = str.toString(); break;
            case ID: child = id.toString(); break;
            default: child = ""; // XXX?
        }
        return String.format("<Primary> %s, child: %s", type, child);
    }
}
