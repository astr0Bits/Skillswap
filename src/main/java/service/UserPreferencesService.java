// service/UserPreferencesService.java
package service;

import dto.UserPreferencesDTO;
import model.User;
import model.UserPreferences;
import repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesService {
    private final UserPreferencesRepository repository;

    public UserPreferencesService(UserPreferencesRepository repository) {
        this.repository = repository;
    }

    public UserPreferencesDTO getPreferences(User user) {
        UserPreferences prefs = repository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));
        return toDTO(prefs);
    }

    @Transactional
    public UserPreferencesDTO updatePreferences(User user, UserPreferencesDTO dto) {
        UserPreferences prefs = repository.findByUser(user)
                .orElseGet(() -> {
                    UserPreferences newPrefs = new UserPreferences();
                    newPrefs.setUser(user);
                    return newPrefs;
                });
        prefs.setLearningGoals(dto.getLearningGoals());
        prefs.setWeeklyHours(dto.getWeeklyHours());
        prefs.setPreferredMode(dto.getPreferredMode());
        prefs.setNotificationsEnabled(dto.getNotificationsEnabled());
        repository.save(prefs);
        return toDTO(prefs);
    }

    private UserPreferences createDefaultPreferences(User user) {
        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefs.setLearningGoals("");
        prefs.setWeeklyHours(5);
        prefs.setPreferredMode("Online");
        prefs.setNotificationsEnabled(true);
        return repository.save(prefs);
    }

    private UserPreferencesDTO toDTO(UserPreferences prefs) {
        return new UserPreferencesDTO(
            prefs.getLearningGoals(),
            prefs.getWeeklyHours(),
            prefs.getPreferredMode(),
            prefs.getNotificationsEnabled()
        );
    }
}