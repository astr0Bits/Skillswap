package dto;

public class BookSessionRequest {
    private Long mentorId;
    private String skillName;
    private Long skillId;
    private String mode; // "online" or "in-person"

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}