package me.vasan.jimple;

import java.util.Hashtable;

class Environment {
    private Hashtable<String, Object> entries;
    Environment parent;

    Environment() {
        entries = new Hashtable<String, Object>();
        this.parent = null;
        initRootEnv();
    }

    Environment(Environment parent) {
        entries = new Hashtable<String, Object>();
        this.parent = parent;
    }

    /*
     * Add interop with native functions?
     */
    void initRootEnv() {
        entries.put("print", new NativeFunction("print"));
        entries.put("clock", new NativeFunction("clock"));
    }

    boolean exists(String key) {
        return entries.containsKey(key);
    }

    void put(String key, Object val) {
        // TODO: const checks can happen here
        // TODO: global updates can happen here
        entries.put(key, val);
    }

    void update(String key, Object val) {
        if (exists(key))
            entries.put(key, val);
        else
            if (this.parent != null && this.parent.exists(key))
                this.parent.put(key, val);
            else 
                entries.put(key, val);
    }

    Object get(String key) {
        Object val = entries.get(key);
        if (val == null)
            return this.parent.get(key);
        return val;
    }
}