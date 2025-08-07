## Grammar ##
    // declarations
    pgm  => decl* EOF;
    decl => var | stmt;
    var  => "let" ID ("=" expr)? ";";

    // statements
    stmt       => exprStmt | 
                    ifStmt | 
                 whileStmt | 
                returnStmt | 
                blockStmt;
                
    exprStmt   => expr ";";
    ifStmt     => "if" "(" expr ")" stmt ("else" stmt)?;
    whileStmt  => "while" "(" expr ")" stmt;
    returnStmt => "return" expr? ";";
    blockStmt      => "{" decl* "}";
    assignStmt => "let" ID "=" expr;

    // lexical elements
    ID     => ALPHA (ALPHA | DIGIT)*;
    ALPHA  => "a".."z" | "A".."Z" | "_";
    DIGIT  => "0".."9";
    NUMBER => DIGIT+ ("." DIGIT+)?;
    STRING => "\"" ASCII+ "\""  // no utf-8; \" doesn't occur within ASCII

    // expressions (with precedence)
    expr   => assign;
    assign => ID "=" assign | logic_or;   
    logic_or => logic_and ( "||" logic_and )*;
    logic_and => equality ( "&&" equality )*;
    equality => comparison ( ( "!=" | "==" ) comparison )*;
    comparison => term ( ( ">" | ">=" | "<" | "<=" ) term )*;
    term => factor ( ( "-" | "+" ) factor )*;
    factor => unary ( ( "/" | "*" ) unary )*;
    unary => ("!" | "-") unary | function;
    function => "function" "(" (ID ("," ID)*)? ")" block | call;
    call   => primary ("(" (expr ("," expr)* )? ")" | "." ID)*;
    primary => "true" | "false" | "nil" | NUMBER | STRING | ID | "(" expr ")" | object;
    object => "{" (ID ":" expr ("," ID ":" expr)*)? "}";