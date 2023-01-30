package me.vasan.jimple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Jimple {
    static final String SIMPLE_VERSION = "v0.1";
    enum RType {
        error,
        warning,
        message
    };

    static boolean sawError = false;

    private static void usage() {
        System.out.println("usage: jimple [script]");
        System.exit(64);
    }

    static void report(int line, int charPos, String msg, RType rtype) {
        System.err.printf("(line: %s, pos: %s) %s: %s\n", line, charPos, rtype, msg);
        if(rtype == RType.error) sawError = true;
    }

    private static void run(String input, Environment env) throws Exception {
        try {
            Scanner scanner = new Scanner(input);
            List<Token> tokens = scanner.scanTokens();
            Parser p = new Parser(tokens);
            Pgm ast = p.parse();
            Object res = (new Interpreter(env)).interpret(ast);
            System.out.println(res == null ? "nil" : res);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void runFile(String path) throws Exception {
        Environment env = new Environment();
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()), env);
        if (sawError) System.exit(65);
    }

    private static void runPrompt() throws Exception {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        Environment env = new Environment();

        System.out.println("This is Simple " + SIMPLE_VERSION);
        while(true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line, env);
            sawError = false;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            usage();
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
}
