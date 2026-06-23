package src.main.java.exception;

public class PasswordReuseException extends RuntimeException {
    public PasswordReuseException(String message) {
        super(message);
    }
}
