class RuntimeError extends Exception {
    String message;
    public RuntimeError(String s) {
        this.message = s;
    }
}