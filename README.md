A simple programming language:

## REPL

`make`

## Run a script

`java me/vasan/jimple/Jimple ./tests/hello.sim`

## Syntax (subset of JavaScript)

```javascript
// declarations
let foo; // foo is initialized with nil
let foo = "hello"; // strings
let bar = 12.3; // double-precision floating point
let baz = true; // boolean

// blocks
{
	let bar = 43;
	let baz = false;

	bar = 32;
}

// first-class functions
let quux = function (bar, baz) {
	return bar + baz;
}

// statements
if (foo > 32) {
	bar = 32;
} else {
	baz = 32.1;
}

while (foo > 32) {
	// do something
}

// objects, arrays & immutable types *TODO*
let foo = [1, 2, 3];
foo[3]; // ERROR! -- compile error

// special "args" available at top-lovel *TODO*
let bar = {
    'hello': 32,
    'world': 43
};
const baz = 23;

// type declarations and custom types *TODO*
type Rect = {
    'length': number,
    'width': number,
    'name': string,
    'approved': boolean
};

let f1: Rect = {32, 24, 'hello', true};
let b1 = 12; // basic types are inferred
let b2: number; // without init

// reference types *TODO*
let b3 = &b1; // `&` operator for addressof()
let b4 = *b3; // `*` dereference
*(b3++); // reference manipulation (not supported on the JVM)

// modules *TODO*
let f = require("math.sim");
f.add(2, 3);

// function short-hand syntax *TODO*
let five = (bar, baz) -> {
    bar + baz;
}(2, 3);

// for loops *TODO*
for (i in bar) {
    print(i);
    print(bar[i]);
} // prints 'hello32world43'

for (i in range(0, 10)) {
    print(i);
} // prints 0123456789

// optimizations *TODO*
- inlining
- interned strings
- object shapes
- ...

```

## standard library functions (TODO)

```
// time & date: clock();
// unix/io: print(), read(), open(), close(), getdirentries(), ...
// string, mathematics.
```

## compilation 

```javascript
let pi = 3.14;
let e = 2.718;

let expt = function(a, n) {
    if (n == 0) { 
        return 1; 
    } else { 
        let e = 1; let i = 0; 
        while (i < n) { 
            i = i+1;
            e = e*a;
        }
        return e;
    }
};

print(expt(2,pi));
print(expt(3,e));
```

compiles to a equivalent java class

```java
package expt;

// Imports for supporting classes
import me.vasan.jimple.NativeFunction;
import me.vasan.jimple.errors.RuntimeError;

// Every sim file gets compiled into a top-level class
class expt {
    // Every var decl. gets compiled to a static field
    static double pi = 3.14;
    static double e = 2.718;
    
    // Every function decl. gets compiled into a static function
    public static double expt(double a, double n) {
        if (n == 0) {
            return 1;
        } else {
            double e = 1;
            double i = 0;
            while (i < n) {
                i = i + 1;
                e = e * a;
            }
            return e;
        }
    }

    // Any other statements get shoved into a main() function
    public static void main(String[] args) throws RuntimeError {
        // NativeFunction calls such as print, clock are called appropriately
        new NativeFunction("print").call(expt(2, pi));
        new NativeFunction("print").call(expt(3, e));
    }
}
```