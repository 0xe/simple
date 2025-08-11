class NativeFunction {
    String meth;

    public NativeFunction(String meth) {
        this.meth = meth;
    }

    public Object print(Object... args) {
        for (Object a: args) {
            if (a == null) {
                System.out.print("null");
            } else {
                System.out.print(a.toString());
            }
        }
        System.out.println();
        return null;
    }

    public long clock(Object... args) {
        return System.currentTimeMillis();
    }

    public Object call(Object... args) throws Exception {
        if (meth.equals("print"))
            return print(args);
        else if (meth.equals("clock"))
            return clock(args);
        else {
            throw new Exception("NativeFunction#call()");
        }
    }
}