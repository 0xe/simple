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

// objects, arrays
let foo = [1, 2, 3];
foo[3]; // nil

// objects
let Rect = {
    length: 12,
    width: 24,
    name: "hello",
    approved: false
};
```

## standard library functions (LOL)

```
// time & date: clock();
// io: print()
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