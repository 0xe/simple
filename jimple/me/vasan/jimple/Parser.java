package me.vasan.jimple;

import java.util.ArrayList;
import java.util.List;

import static me.vasan.jimple.TT.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Op token_type_to_op(TT t) {
        switch(t) {
            case EQUAL_EQUAL: return Op.EQ;
            case BANG_EQUAL: return Op.NEQ;
            case GREATER: return Op.GT;
            case GREATER_EQUAL: return Op.GTE;
            case LESSER: return Op.LT;
            case LESSER_EQUAL: return Op.LTE;
            case MINUS: return Op.MIN;
            case PLUS: return Op.PLUS;
            case STAR: return Op.MUL;
            case SLASH: return Op.DIV;
            case BANG: return Op.NOT;
            case OR: return Op.LOR;
            case AND: return Op.LAN;
            default: return null; 
        }
    }

    Token peek() throws EofReached {
        if (current < size()) {
            Token t = tokens.get(current);
            String debugEnabled = System.getProperty("ParserDebug");
            if (debugEnabled != null && debugEnabled.equals("1"))
                System.out.println("Peek [" + current + "]: " + t);
            if (t.type == TT.COMMENT) { 
                advance();
                return peek(); 
            } else 
                return t;
        } else {
            throw new EofReached(String.format("%d", current));
        }
    }

    boolean match(TT... types) throws EofReached {
        for (TT t: types) {
            if (peek().type == t) {
                advance();
                return true;
            }
        }
        return false;
    }

    boolean consume(TT type) throws EofReached {
        if (peek().type == type && current < size()) {
            advance();
            return true;
        } else {
            return false;
        }
    }

    Token next() throws EofReached {
        Token t = peek();
        advance();
        return t;
    }

    int size() {
        return tokens.size();
    };

    Token previous() {
        if ((current - 1) >= 0)
            return tokens.get(current - 1);
        else 
            return null;
    }

    void advance() {
        String debugEnabled = System.getProperty("ParserDebug");
        if (debugEnabled != null && debugEnabled.equals("1")) {
            System.out.println("advance() -> ");
            (new Exception()).printStackTrace();
        }
        current++;
    }

    void rollback() {
        current--;
    }

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Pgm parse() throws SyntaxError {
        Pgm p = new Pgm();
        p.decls = new ArrayList<Decl>();
        try {
            while (current < size() && peek().type != TT.EOF) {
                p.decls.add(parse_decl());
                consume(TT.SEMICOLON);
            }
        } catch (EofReached e) {}
        return p;
    }

    Decl parse_decl() throws SyntaxError, EofReached {
        Token t = next();
        if (t.type == LET)
            return new Decl(parse_var());
        else { 
            rollback();
            return new Decl(parse_stmt());
        }
    }

    Var parse_var() throws SyntaxError, EofReached {
        Token t = next();
        if (t.type != TT.IDENTIFIER) {
            throw new SyntaxError("parse_var");
        }

        Id i = new Id(t.lexeme, t.line, t.charPos);
        t = next();

        if (t.type == TT.SEMICOLON || t.type == TT.EOF) {
            return new Var(i);
        } else if (t.type == TT.EQUAL) {
            Expr e = parse_expr();
            return new Var(i, e);
        } else {
            throw new SyntaxError("parse_var");
        }
    }

    Stmt parse_stmt() throws SyntaxError, EofReached {
        Token t = next();
        Stmt s = null;
        switch (t.type) {
            case IF: s = parse_if_stmt(); break;
            case WHILE: s = parse_while_stmt(); break;
            case RETURN: s = parse_return_stmt(); break;
            case LEFT_BRACE: rollback(); s = parse_block_stmt(); break;
            default: { rollback(); s = parse_expr_stmt(); }
        }
        return s;
    }

    Stmt parse_if_stmt() throws SyntaxError, EofReached {
        Token t = next(); Expr cond = null; Stmt then = null; Stmt alt = null;
        if (t.type == TT.LEFT_PAREN) {
            cond = parse_expr(); t = next();
            if (t.type == TT.RIGHT_PAREN) {
                then = parse_stmt();
                t = peek();
                if (t.type == TT.ELSE) {
                    advance(); alt = parse_stmt();
                    return new Stmt(new IfStmt(cond, then, alt));
                } else {
                    return new Stmt(new IfStmt(cond, then));
                }
            } else {
                throw new SyntaxError("parse_if_stmt");
            }
        } else {
            throw new SyntaxError("parse_if_stmt");
        }
    }

    Stmt parse_while_stmt() throws SyntaxError, EofReached {
        Token t = next(); Expr cond = null; Stmt then = null;
        if (t.type == TT.LEFT_PAREN) {
            cond = parse_expr();
            t = next();
            if (t.type == TT.RIGHT_PAREN) {
                then = parse_stmt();
                return new Stmt(new WhileStmt(cond, then));
            } else {
                throw new SyntaxError("parse_while_stmt");
            }
        } else {
            throw new SyntaxError("parse_while_stmt");
        }
    }

    Stmt parse_return_stmt() throws SyntaxError, EofReached {
        Token t = peek(); Expr e = null;
        if (t.type == TT.SEMICOLON) {
            advance(); return new Stmt(new ReturnStmt());
        } else {
            e = parse_expr(); t = peek();
            if (t.type == TT.SEMICOLON) {
                advance(); return new Stmt(new ReturnStmt(e));
            } else {
                throw new SyntaxError("parse_return_stmt");
            }
        }
    }

    Stmt parse_block_stmt() throws SyntaxError, EofReached {
        if (!consume(LEFT_BRACE))
            throw new SyntaxError("parse_block_stmt");
        List<Decl> decls = new ArrayList<Decl>();

        while (peek().type != TT.RIGHT_BRACE && current < size()) {
            decls.add(parse_decl());
            consume(TT.SEMICOLON);
        }

        if (peek().type == TT.RIGHT_BRACE && current < size())
            advance();

        return new Stmt(new BlockStmt(decls));
    }

    Stmt parse_expr_stmt() throws SyntaxError, EofReached {
        Expr e = parse_expr();
        Token t = peek();
        if (t.type == TT.SEMICOLON || t.type == TT.RETURN || t.type == TT.EOF) {
            advance(); return new Stmt(new ExprStmt(e));
        /* uncomment for implicit return of last expression w/o semi-colon
        } else if (t.type == TT.RIGHT_BRACE) {
            return new Stmt(new ExprStmt(e)); 
        */
        } else {
            throw new SyntaxError("parse_expr_stmt()");
        }
    }

    Expr parse_expr() throws SyntaxError, EofReached {
        return assign_expr();
    }

    Expr assign_expr() throws SyntaxError, EofReached {
        Token t = peek();
        if (t.type == TT.IDENTIFIER) {
            advance(); Token t_ = peek();
            if (t_.type == TT.EQUAL) {
                advance();
                return new Expr(
                    new AssignExpr(
                        new Id(t.lexeme, t.line, t.charPos),
                        assign_expr()));
            } else {
                rollback(); // function calls
                return logic_or();
            }
        }
        return logic_or();
    }

    Expr logic_or() throws SyntaxError, EofReached {
        Expr expr = logic_and();

        while (match(OR)) {
            Token op = previous();
            Expr right = logic_and();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr logic_and() throws SyntaxError, EofReached {
        Expr expr = equality();

        while (match(AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr equality() throws SyntaxError, EofReached {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token op = previous();
            Expr right = comparison();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr comparison() throws SyntaxError, EofReached {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESSER, LESSER_EQUAL)) {
            Token op = previous();
            Expr right = term();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr term() throws SyntaxError, EofReached {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr factor() throws SyntaxError, EofReached {
        Expr expr = unary();

        while (match(STAR, SLASH)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr(new BinaryExpr(expr, right, token_type_to_op(op.type)));
        }

        return expr;
    }

    Expr unary() throws SyntaxError, EofReached {
        Token t = peek();
        if (t.isUnary()) {
            advance();
            return new Expr(new UnaryExpr(unary(), token_type_to_op(t.type)));
        } else {
            return function();
        }
    }

    Expr function() throws SyntaxError, EofReached {
        Token t = peek();
        if (t.type == TT.FUNCTION) {
            ArrayList<Id> args = new ArrayList<Id>();
            advance();
            consume(LEFT_PAREN);
            while (peek().type != RIGHT_PAREN) {
                // TODO: what if it's not an Id?
                Token t_ = next();
                args.add(new Id(t_.lexeme, t_.line, t_.charPos));
                if (peek().type == COMMA)
                    consume(COMMA);
            }
            consume(RIGHT_PAREN);
            Stmt s = parse_block_stmt();
            return new Expr(new FunctionExpr(args, s));
        } else {
            return call();
        }
    }

    Expr call() throws SyntaxError, EofReached {
        Expr expr = primary();
        
        while (true) {
            if (match(LEFT_PAREN)) {
                ArrayList<Expr> args = new ArrayList<Expr>();
                if (peek().type != RIGHT_PAREN) {
                    do {
                        args.add(parse_expr());
                    } while (match(COMMA));
                }
                consume(RIGHT_PAREN);
                
                if (expr.type == ExprType.PRIMARY && expr.pe.type == PrimaryType.ID) {
                    expr = new Expr(new CallExpr(expr.pe.id, args));
                } else {
                    throw new SyntaxError("Can only call functions");
                }
            } else if (match(DOT)) {
                Token propertyToken = peek();
                if (propertyToken.type != IDENTIFIER) {
                    throw new SyntaxError("Expected property name after '.'");
                }
                advance();
                expr = new Expr(new PropertyAccessExpr(expr, propertyToken.lexeme));
            } else {
                break;
            }
        }
        
        return expr;
    }

    Expr primary() throws SyntaxError, EofReached {
        Token t = peek();
        if (t.isPrimary()) {
            advance();
            return new Expr(new PrimaryExpr(t));
        } else if (t.type == LEFT_BRACE) {
            return parseObject();
        } else {
            throw new SyntaxError("primary");
        }
    }

    Expr parseObject() throws SyntaxError, EofReached {
        consume(LEFT_BRACE);
        
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Expr> values = new ArrayList<>();
        
        if (peek().type != RIGHT_BRACE) {
            do {
                Token keyToken = peek();
                if (keyToken.type != IDENTIFIER) {
                    throw new SyntaxError("Expected property name");
                }
                advance();
                
                consume(COLON);
                
                keys.add(keyToken.lexeme);
                values.add(parse_expr());
                
            } while (match(COMMA));
        }
        
        consume(RIGHT_BRACE);
        return new Expr(new ObjectExpr(keys, values));
    }
}