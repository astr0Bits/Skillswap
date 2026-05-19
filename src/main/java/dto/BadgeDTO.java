// dto/BadgeDTO.java
package dto;

public class BadgeDTO {
    private String name;
    private String icon;
    private boolean earned;
    private String requirement;
    
    public BadgeDTO() {}
    
    public BadgeDTO(String name, String icon, boolean earned, String requirement) {
        this.name = name;
        this.icon = icon;
        this.earned = earned;
        this.requirement = requirement;
    }
    
    // getters & setters (same as before)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isEarned() { return earned; }
    public void setEarned(boolean earned) { this.earned = earned; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
}