//package dto;
//import enums.Role;
///*Captures the data sent from the registration form. 
// * The validation annotations ensure data integrity before it reaches the service layer.
//*/
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//
//public class SignupRequest {
//
//    @NotBlank
//    @Email(message = "Invalid email format")
//    private String email; // required and must be a valid email format.
//
//    @NotBlank
//    @Size(min = 6, max = 40)
//    private String password;// required, length between 6 and 40 characters.
//
//    @NotBlank
//    @Size(min = 2, max = 100)
//    private String name;//required, between 2 and 100 characters.
//
//    @NotBlank
//    private String location; 
//
//    //role required; it should be one of the predefined roles
//    private @NotNull Role role; // Must be LEARNER or SPONSOR
//
//    // Getters and setters
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//
//    public String getPassword() { return password; }
//    public void setPassword(String password) { this.password = password; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public String getLocation() { return location; }
//    public void setLocation(String location) { this.location = location; }
//
//    public @NotNull Role getRole() { return role; }
//    public void setRole(@NotNull Role role) { this.role = role; }
//}

package dto;

import enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import validator.NoHtml;

public class SignupRequest {

    @NotBlank
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @NotBlank
    @Size(min = 2, max = 100)
    @NoHtml   // 🔥 ADDED
    private String name;

    @NotBlank
    @NoHtml   // 🔥 ADDED
    private String location;

    private @NotNull Role role;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
    
    
}