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
```

## standard library functions (TODO)

```
// time & date: clock();
// io: print(); read(); open(); close(); getdirentries();
// string, mathematics?
```