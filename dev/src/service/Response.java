package service;

public class Response<T> {
    private T value;
    private String message;
    private boolean success;

    /** Success response */
    public Response(T value, String message) {
        this.value = value;
        this.message = message;
        this.success = true;
    }

    /** Error response — value is null, success is false */
    public Response(String errorMessage) {
        this.value = null;
        this.message = errorMessage;
        this.success = false;
    }

    public T getValue() { return value; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}
