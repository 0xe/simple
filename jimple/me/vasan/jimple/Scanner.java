package me.vasan.jimple;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* liberally "borrowed" from jlox (craftinginterpreters.com) */

/* tokens in our language */
/*
    ( ) { }
    & | ^ , . - + ; / * ~ %

    && || !
    != = ==
    > >= < <= << >>

    id, "foobar", 3829e+23

    else false function if nil
    return true let while

    eof

    // to eof is a comment
*/

class RuntimeError extends Exception {
    String message;
    RuntimeError(String s) { this.message = s;}
}

class SyntaxError extends Exception {
    String message;
    SyntaxError(String s) { this.message = s;}
}

// bleh
class EofReached extends Exception {
    String message;
    EofReached(String s) { this.message = s;}
}

enum TT {
    /* single char */
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, AMP, PIPE, HAT,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, TILDE, MOD,

    /* multi-char */
    AND, OR, BANG,
    BANG_EQUAL, EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL, LESSER, LESSER_EQUAL, LSHIFT, RSHIFT,

    /* unary */
    UMINUS,

    IDENTIFIER, STRING, NUMBER,

    ELSE, FALSE, FUNCTION, IF, NIL,
    RETURN, TRUE, LET, WHILE,

    COMMENT, EOF;
}

class Token {
    final TT type;
    final String lexeme;
    final Object literal;
    final int line;
    final int charPos;

    Token(TT type, String lexeme, Object literal, int line, int charPos) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.charPos = charPos;
    }

    public String toString() {
        return String.format("%16s, %8s, %8s, %8d, %8d", type, lexeme, literal, line, charPos);
    }

    boolean isUnary() {
        return type == TT.BANG || type == TT.MINUS;
    }

    boolean isPrimary() {
        return type == TT.TRUE || type == TT.FALSE || type == TT.NIL || type == TT.NUMBER || type == TT.STRING || type == TT.IDENTIFIER;
    }
}

public class Scanner {
    final String input;
    final List<Token> tokens = new ArrayList<>();
    private static final Map<String, TT> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    TT.AND);
        keywords.put("else",   TT.ELSE);
        keywords.put("false",  TT.FALSE);
        keywords.put("function", TT.FUNCTION);
        keywords.put("if",     TT.IF);
        keywords.put("nil",    TT.NIL);
        keywords.put("or",     TT.OR);
        keywords.put("return", TT.RETURN);
        keywords.put("true",   TT.TRUE);
        keywords.put("let",    TT.LET);
        keywords.put("while",  TT.WHILE);
    }

    private int start = 0;
    private int current = 0;
    private int line = 0;
    private int charPos = 0;

    Scanner(String input)
    {
        this.input = input;
    }

    private void addToken(TT type)
    {
        addToken(type, null);
    }

    private void addToken(TT type, Object literal) {
        String text = null;
        if (literal != null)
            text = literal.toString();
        else
            text = input.substring(start, current);
        tokens.add(new Token(type, text, literal, line, charPos));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (input.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private boolean match_next(char expected) {
        if (current >= input.length()) return false;
        if (input.charAt(current) != expected) return false;

        current++; charPos++;
        return true;
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') { line++; charPos = 0; }
            if (peek() == '\\' && peekNext() == '"') { advance(); charPos++; }
            advance(); charPos++;
        }

        if (current == input.length()) {
            Jimple.report(line, charPos, "Unterminated string", Jimple.RType.error);
        }
        current++;
        String value = input.substring(start + 1, current - 1);
        addToken(TT.STRING, value);
    }

    private boolean isAtEnd() {
        return current == input.length();
    }

    private char peek() {
        if (current == input.length()) return '\0';
        return input.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= input.length()) return '\0';
        return input.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char advance() {
        current++;
        return input.charAt(current - 1);
    }

    // TODO: add support for binary (0b00010010), hex (0xCAFEBABE),
    //       octal (0o37362132) representations
    private void number()
    {
        while (isDigit(peek()))
            advance();

        // look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }

        if(peek() == 'e') {
            advance();
            if(peekNext() == '-' || peekNext() == '+'); advance();
            while(isDigit(peek())) advance();

            if(peek() == '.' && isDigit(peekNext())) {
                advance(); while(isDigit(peek())) advance();
            }
        }

        try {
            addToken(TT.NUMBER,
                    Double.parseDouble(input.substring(start, current)));
        } catch(NumberFormatException nfe) {
            Jimple.report(line, charPos, "Bad number", Jimple.RType.error);
        }
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // See if the identifier is a reserved word.
        String text = input.substring(start, current);

        TT type = keywords.get(text);
        if (type == null) type = TT.IDENTIFIER;
        addToken(type); charPos += current-start-1;
    }

    private void scanToken() throws SyntaxError
    {
        char c = advance();
        charPos++;
        /* TODO: add support for ++ -- += -= /= *= &= |= &= ^= %= <<= >>= etc.*/
        switch(c) {
            /* single char */
            case '(': addToken(TT.LEFT_PAREN); break;
            case ')': addToken(TT.RIGHT_PAREN); break;
            case '{': addToken(TT.LEFT_BRACE); break;
            case '}': addToken(TT.RIGHT_BRACE); break;
            case ',': addToken(TT.COMMA); break;
            case '.': addToken(TT.DOT); break;
            case '-': addToken(TT.MINUS); break;
            case '+': addToken(TT.PLUS); break;
            case ';': addToken(TT.SEMICOLON); break;
            case '*': addToken(TT.STAR); break;
            case '~': addToken(TT.TILDE); break;
            case '%': addToken(TT.MOD); break;
            case '^': addToken(TT.HAT); break;

            /* multi char */
            case '&':
                addToken(match_next('&') ? TT.AND : TT.AMP); break;

            case '|':
                addToken(match_next('|') ? TT.OR : TT.PIPE); break;

            case '!':
                addToken(match_next('=') ? TT.BANG_EQUAL : TT.BANG); break;

            case '=':
                addToken(match_next('=') ? TT.EQUAL_EQUAL : TT.EQUAL); break;

            case '>':
                addToken(match_next('=') ? TT.GREATER_EQUAL :
                         match_next('>') ? TT.RSHIFT : TT.GREATER); break;

            case '<':
                addToken(match_next('=') ? TT.LESSER_EQUAL :
                         match_next('<') ? TT.LSHIFT : TT.LESSER); break;

            case '\n': {
                line++; charPos = 0; break;
            }

            /* comment or could be '/'*/
            case '/': {
                    if (!match_next('/')) {
                            addToken(TT.SLASH);
                            break;
                    } else {
                            while (peek() != '\0' && peek() != '\n') {
                                    advance();
                                    charPos++;
                            }
                            line++;
                            charPos = 0;
                            addToken(TT.COMMENT);
                            break;
                    }
            }

            /* strings */
            case '"': string(); break;

            /* ignore whitespace */
            case ' ':
            case '\r':
            case '\t':
                break;

            /* numbers */
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                number(); break;

            /* nothing */
            default:
                if (isAlpha(c))
                    identifier();
                else {
                    Jimple.report(line, charPos, "Unexpected character: " + c, Jimple.RType.error);
                    throw new SyntaxError("Unexpected character");
                }
        }
    }

    List<Token> scanTokens() throws SyntaxError
    {
        try {
            while (current < input.length()) {
                start = current;
                scanToken();
            }
            tokens.add(new Token(TT.EOF, "", null, line, charPos));
            return tokens;
        } catch(SyntaxError s) {
            Jimple.report(-1, -1, "Syntax Errors Found", Jimple.RType.error);
            return null;
        }
    }
}
