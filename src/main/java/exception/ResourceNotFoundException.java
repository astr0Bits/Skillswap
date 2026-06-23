package src.main.java.exception;



public class ResourceNotFoundException extends RuntimeException {
 public ResourceNotFoundException(String message) {
     super(message);
 }
}