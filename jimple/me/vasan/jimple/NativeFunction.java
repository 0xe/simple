package me.vasan.jimple;

public class NativeFunction {
    String meth;

    public NativeFunction(String meth) {
        this.meth = meth;
    }

    public Object print(Object... args) {
        for (Object a: args)
            System.out.print(a.toString());
        System.out.println();
        return null;
    }

    public long clock(Object... args) {
        return System.currentTimeMillis();
    }

    public Object call(Object... args) throws RuntimeError {
        if (meth.equals("print"))
            return print(args);
        else if (meth.equals("clock"))
            return clock(args);
        else {
            throw new RuntimeError("NativeFunction#call()");
        }
    }
}