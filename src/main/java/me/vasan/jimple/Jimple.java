package me.vasan.jimple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.InvocationTargetException;

public class Jimple {
    static final String SIMPLE_VERSION = "v0.1";
    enum RType {
        error,
        warning,
        message
    };

    static boolean sawError = false;
    static boolean compilerMode = false;
    static boolean debugMode = false;

    private static void usage() {
        System.out.println("usage: jimple [options] [script]");
        System.out.println("options:");
        System.out.println("  -i        run in interpreter mode");
        System.out.println("  --ast     print AST and exit");
        System.exit(64);
    }

    static void report(int line, int charPos, String msg, RType rtype) {
        System.err.printf("(line: %s, pos: %s) %s: %s\n", line, charPos, rtype, msg);
        if(rtype == RType.error) sawError = true;
    }

	private static void compile(String name, String input) throws Exception {
		try {
			Scanner scanner = new Scanner(input);
			List<Token> tokens = scanner.scanTokens();
			Parser p = new Parser(tokens);
			Pgm ast = p.parse();
			(new Compiler(name)).compile(ast);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private static void compileAndRun(String input, int exprCount) throws Exception {
        try {
            String tempName = "REPLExpr" + exprCount;
            Scanner scanner = new Scanner(input);
            List<Token> tokens = scanner.scanTokens();
            Parser p = new Parser(tokens);
            Pgm ast = p.parse();
            
            // Compile the expression
            Compiler compiler = new Compiler(tempName + ".sim");
            compiler.compile(ast);
            
            String classFileName = tempName + ".class";
            
            // Decompile and print if debug mode is enabled
            if (debugMode) {
                decompileClassFile(classFileName);
            }
            
            // Load and execute the compiled class using Callable interface
            try {
                // Load the class file
                Path classPath = Paths.get(classFileName);
                if (Files.exists(classPath)) {
                    byte[] classBytes = Files.readAllBytes(classPath);
                    
                    // Create a custom class loader
                    ClassLoader classLoader = new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                            if (name.equals(tempName)) {
                                return defineClass(name, classBytes, 0, classBytes.length);
                            }
                            return super.findClass(name);
                        }
                    };
                    
                    // Load the class and create an instance implementing Callable
                    Class<?> clazz = classLoader.loadClass(tempName);
                    Callable callable = (Callable) clazz.getDeclaredConstructor().newInstance();
                    
                    // Call the script and get the result
                    Object result = callable.call();
                    
                    // Print the result if not null
                    if (result != null) {
                        System.out.println(result);
                    }
                    
                    // Clean up the temporary class file
                    Files.deleteIfExists(classPath);
                }
            } catch (Exception e) {
                System.err.println("Error executing compiled code: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void decompileClassFile(String classFileName) {
        try {
            Path classPath = Paths.get(classFileName);
            if (Files.exists(classPath)) {
                byte[] classBytes = Files.readAllBytes(classPath);
                ClassFile classFile = ClassFile.of();
                ClassModel classModel = classFile.parse(classBytes);
                
                System.out.println("=== Decompiled Class File: " + classFileName + " ===");
                System.out.println("Class: " + classModel.thisClass().asInternalName());
                System.out.println("Superclass: " + classModel.superclass().map(c -> c.asInternalName()).orElse("java.lang.Object"));
                
                // Print methods
                classModel.methods().forEach(method -> {
                    System.out.println("Method: " + method.methodName().stringValue() + 
                                     " " + method.methodType().stringValue());
                    
                    // Print method code if available
                    method.code().ifPresent(code -> {
                        System.out.println("  Code:");
                        code.forEach(instruction -> {
                            System.out.println("    " + instruction);
                        });
                    });
                });
                
                System.out.println("=== End Decompiled Class File ===");
            }
        } catch (Exception e) {
            System.err.println("Error decompiling class file: " + e.getMessage());
        }
    }

    private static void run(String input, Environment env) throws Exception {
        try {
            Scanner scanner = new Scanner(input);
            List<Token> tokens = scanner.scanTokens();
            Parser p = new Parser(tokens);
            Pgm ast = p.parse();
            
            // Print AST if debug mode is enabled
            if (debugMode) {
                System.out.println("AST: " + ast);
            }
            
            Object res = (new Interpreter(env)).interpret(ast);
            System.out.println(res == null ? "nil" : res);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

	private static void compileFile(String path) throws Exception {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		compile(path, new String(bytes, Charset.defaultCharset()));
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
        int exprCount = 0;

        System.out.println("This is Simple " + SIMPLE_VERSION);
        System.out.println("Special commands:");
        System.out.println("  :mode compiler - Switch to compiler mode");
        System.out.println("  :mode interpreter - Switch to interpreter mode");
        System.out.println("  :debug on - Enable debug mode (shows decompiled classfiles)");
        System.out.println("  :debug off - Disable debug mode");
        System.out.println("  :help - Show this help");
        System.out.println("Current mode: " + (compilerMode ? "compiler" : "interpreter"));
        System.out.println("Debug mode: " + (debugMode ? "on" : "off"));
        
        while(true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            
            // Handle special commands
            if (line.startsWith(":")) {
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case ":mode":
                        if (parts.length > 1) {
                            if (parts[1].equals("compiler")) {
                                compilerMode = true;
                                System.out.println("Switched to compiler mode");
                            } else if (parts[1].equals("interpreter")) {
                                compilerMode = false;
                                System.out.println("Switched to interpreter mode");
                            } else {
                                System.out.println("Unknown mode: " + parts[1] + ". Use 'compiler' or 'interpreter'");
                            }
                        } else {
                            System.out.println("Current mode: " + (compilerMode ? "compiler" : "interpreter"));
                        }
                        break;
                    case ":debug":
                        if (parts.length > 1) {
                            if (parts[1].equals("on")) {
                                debugMode = true;
                                System.out.println("Debug mode enabled");
                            } else if (parts[1].equals("off")) {
                                debugMode = false;
                                System.out.println("Debug mode disabled");
                            } else {
                                System.out.println("Unknown debug option: " + parts[1] + ". Use 'on' or 'off'");
                            }
                        } else {
                            System.out.println("Debug mode: " + (debugMode ? "on" : "off"));
                        }
                        break;
                    case ":help":
                        System.out.println("Special commands:");
                        System.out.println("  :mode compiler - Switch to compiler mode");
                        System.out.println("  :mode interpreter - Switch to interpreter mode");
                        System.out.println("  :debug on - Enable debug mode (shows decompiled classfiles)");
                        System.out.println("  :debug off - Disable debug mode");
                        System.out.println("  :help - Show this help");
                        System.out.println("Current mode: " + (compilerMode ? "compiler" : "interpreter"));
                        System.out.println("Debug mode: " + (debugMode ? "on" : "off"));
                        break;
                    default:
                        System.out.println("Unknown command: " + parts[0] + ". Type :help for available commands");
                        break;
                }
            } else {
                // Execute the expression based on current mode
                if (compilerMode) {
                    compileAndRun(line, exprCount++);
                } else {
                    run(line, env);
                }
                sawError = false;
            }
        }
    }

    private static void printAst(String filename) throws Exception {
        String input = Files.readString(Paths.get(filename));
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Pgm program = parser.parse();
        System.out.println(program);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 2) {
            usage();
        } else if (args.length == 1) {
            compileFile(args[0]);
        } else if (args.length == 2) {
            if (args[0].equals("-i")) {
                runFile(args[1]);
            } else if (args[0].equals("--ast")) {
                printAst(args[1]);
            } else {
                usage();
            }
        } else {
            runPrompt();
        }
    }
}
