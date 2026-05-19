package service;

import dto.BadgeDTO;
import model.User;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class BadgeService {
    
    // Existing method (for profile page)
    public List<BadgeDTO> getUserBadges(User user, int points, long totalSessions, int reputation) {
        List<BadgeDTO> badges = new ArrayList<>();
        
        if (points >= 500) {
            badges.add(new BadgeDTO("Master", "🏆", true, "500+ points"));
        } else if (points >= 250) {
            badges.add(new BadgeDTO("Adept", "🔥", true, "250+ points"));
        } else if (points >= 100) {
            badges.add(new BadgeDTO("Apprentice", "⚙️", true, "100+ points"));
        } else {
            badges.add(new BadgeDTO("Novice", "🌱", true, "0+ points"));
        }
        
        if (reputation >= 90) {
            badges.add(new BadgeDTO("Five-Star Mentor", "⭐", true, "Rating ≥ 4.5"));
        } else {
            badges.add(new BadgeDTO("Five-Star Mentor", "⭐", false, "Reach 4.5 stars"));
        }
        
        if (totalSessions >= 20) {
            badges.add(new BadgeDTO("Expert Swapper", "🔄", true, "20+ sessions"));
            badges.add(new BadgeDTO("Community Builder", "🤝", true, "5+ sessions"));
        } else if (totalSessions >= 5) {
            badges.add(new BadgeDTO("Community Builder", "🤝", true, "5+ sessions"));
            badges.add(new BadgeDTO("Expert Swapper", "🔄", false, "Complete 20 sessions"));
        } else {
            badges.add(new BadgeDTO("Community Builder", "🤝", false, "Complete 5 sessions"));
            badges.add(new BadgeDTO("Expert Swapper", "🔄", false, "Complete 20 sessions"));
        }
        
        return badges;
    }
    
    // New method for browse page (returns simple string list)
    public List<String> getUserBadgeNames(User user, int reputation, long completedSessions, double avgRating) {
        List<String> badgeNames = new ArrayList<>();
        
        if (reputation >= 90) {
            badgeNames.add("five-star");
        }
        if (completedSessions >= 50) {
            badgeNames.add("master");
        }
        if (completedSessions >= 10 && avgRating >= 4.5) {
            badgeNames.add("verified");
        }
        if (completedSessions >= 20) {
            badgeNames.add("expert");
        }
        
        return badgeNames;
    }
}