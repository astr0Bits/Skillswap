package service;

import model.Session;
import model.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import enums.SessionMode;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class SessionEmailService {

    private final JavaMailSender mailSender;

    public SessionEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBookingConfirmation(User learner, User mentor,
                                        Session session, String skillName) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("dalal1087226@gmail.com");
        helper.setTo(learner.getEmail()); // always the registered email, no user input
        helper.setSubject("SkillSwap – Session Booking Confirmed: " + skillName);

        boolean isOnline = SessionMode.ONLINE.equals(session.getMode());
        helper.setText(buildEmailBody(learner, mentor, session, skillName, isOnline), true);

        mailSender.send(message);
    }

    private String buildEmailBody(User learner, User mentor, Session session,
                                   String skillName, boolean isOnline) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a");
        String scheduledTime = session.getScheduledTime() != null
                ? session.getScheduledTime().format(fmt)
                : "To be confirmed by mentor";

        String modeBlock;
        if (isOnline) {
            modeBlock = """
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">
                     Format
                  </td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <span style="background:#eef2ff;color:#4f46e5;padding:2px 10px;
                                 border-radius:20px;font-size:12px;font-weight:600;">
                       Online
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                     Teams Link
                  </td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <a href="%s" style="color:#4f46e5;font-weight:600;word-break:break-all;">
                      Join Microsoft Teams Meeting
                    </a><br>
                    <small style="color:#94a3b8;font-size:11px;">%s</small>
                  </td>
                </tr>
                <tr>
                  <td colspan="2" style="padding:14px;background:#eef2ff;border-radius:8px;">
                    <strong style="color:#4f46e5;"> What to prepare:</strong><br>
                    <span style="font-size:13px;color:#374151;line-height:1.9;">
                      • Stable internet connection (Wi-Fi recommended)<br>
                      • Microsoft Teams installed — or open in your browser<br>
                      • Notebook or digital notes app ready<br>
                      • Any files or questions you want to discuss<br>
                      • A quiet space free from distractions
                    </span>
                  </td>
                </tr>
                """.formatted(session.getMeetingLink(), session.getMeetingLink());
        } else {
            String loc = (session.getLocation() != null && !session.getLocation().isBlank())
                    ? session.getLocation() : "Your mentor will confirm the location shortly";
            modeBlock = """
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">
                    📡 Format
                  </td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <span style="background:#fef3c7;color:#92400e;padding:2px 10px;
                                 border-radius:20px;font-size:12px;font-weight:600;">
                      📍 In-Person
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                    🗺️ Location
                  </td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                </tr>
                <tr>
                  <td colspan="2" style="padding:14px;background:#fef9ec;border-radius:8px;">
                    <strong style="color:#92400e;">🎒 What to bring:</strong><br>
                    <span style="font-size:13px;color:#374151;line-height:1.9;">
                      • Notebook and pen for notes<br>
                      • Your laptop or tablet if relevant<br>
                      • Any materials or questions prepared in advance<br>
                      • Water bottle and a light snack<br>
                      • Arrive 5 minutes early
                    </span>
                  </td>
                </tr>
                """.formatted(loc);
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:'DM Sans',Arial,sans-serif;background:#f5f7fc;">
              <div style="max-width:600px;margin:32px auto;background:white;border-radius:16px;
                          overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                <!-- Header -->
                <div style="background:linear-gradient(135deg,#4f46e5,#818cf8);padding:32px 36px;">
                  <h1 style="margin:0;color:white;font-size:22px;">📚 SkillSwap UAE</h1>
                  <p style="margin:8px 0 0;color:#c7d2fe;font-size:14px;">
                    Session Booking Confirmation
                  </p>
                </div>

                <!-- Greeting -->
                <div style="padding:30px 36px 0;">
                  <p style="color:#0f172a;font-size:16px;margin:0 0 6px;">
                    Hi <strong>%s</strong> 
                  </p>
                  <p style="color:#64748b;font-size:14px;margin:0 0 24px;">
                    Your session has been successfully booked!
                    Your mentor will confirm the exact time shortly.
                  </p>
                </div>

                <!-- Session Details Table -->
                <div style="padding:0 36px 20px;">
                  <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">
                         Skill
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                         Mentor
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                         Mentor Email
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                         Proposed Date
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                         Duration
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">60 minutes</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">
                         Status
                      </td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">
                        <span style="background:#fef3c7;color:#92400e;padding:3px 10px;
                                     border-radius:20px;font-size:12px;font-weight:600;">
                           PENDING CONFIRMATION
                        </span>
                      </td>
                    </tr>
                    %s
                  </table>
                </div>

                <!-- Next Steps -->
                <div style="margin:0 36px 30px;padding:16px;background:#f0fdf4;
                            border-radius:10px;border-left:4px solid #10b981;">
                  <strong style="color:#065f46;font-size:14px;">⚠️ Next Steps</strong><br>
                  <span style="color:#047857;font-size:13px;line-height:1.7;">
                    Your mentor will review and confirm this session within 24 hours.<br>
                    You'll receive another email once it's confirmed.<br>
                    To cancel, please do so at least 24 hours before the session.
                  </span>
                </div>

                <!-- Footer -->
                <div style="padding:18px 36px;background:#f8faff;
                            border-top:1px solid #e2e8f0;text-align:center;">
                  <p style="margin:0;color:#94a3b8;font-size:12px;line-height:1.7;">
                    SkillSwap UAE · Automated confirmation email<br>
                    Session ID: <strong>#%s</strong>
                  </p>
                </div>

              </div>
            </body>
            </html>
            """.formatted(
                learner.getName(),
                skillName,
                mentor.getName(),
                mentor.getEmail(),
                scheduledTime,
                modeBlock,
                session.getId()
            );
    }
}