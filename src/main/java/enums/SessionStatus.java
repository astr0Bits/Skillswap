// enums/SessionStatus.java
package src.main.java.enums;

public enum SessionStatus {
	OPEN, 
    PENDING,      // waiting for mentor approval
    SCHEDULED,    // accepted, ready to happen
    COMPLETED,
    CANCELLED
}