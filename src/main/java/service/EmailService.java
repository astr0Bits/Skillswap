package service;

import model.Session;
import model.ContactMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.email:admin@skillswap.com}")
    private String contactEmail; // configurable recipient for contact form

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl; // base URL for building verification/reset links

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Validates email format using a simple regex.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Sends an HTML email containing a one‑time password (OTP) – used for password reset or MFA.
     */
    public void sendOtpEmail(String to, String otp) {
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Invalid or missing recipient email: " + to);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Your SkillSwap OTP Code");
            helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                "<h2>SkillSwap OTP</h2>" +
                "<p>Your one‑time password (OTP) is: <strong>" + otp + "</strong></p>" +
                "<p>This code will expire in 10 minutes.</p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    /**
     * Sends an email verification link to a new user.
     *
     * @param to    the recipient's email address
     * @param token the verification token (to be appended to the link)
     */
    public void sendVerificationEmail(String to, String token) {
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Invalid or missing recipient email: " + to);
        }
        String verificationUrl = baseUrl + "/api/auth/verify?token=" + token;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Welcome to SkillSwap – Verify Your Email");
            helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                "<h2>Welcome to SkillSwap!</h2>" +
                "<p>Please click the link below to verify your email address and activate your account:</p>" +
                "<p><a href='" + verificationUrl + "'>" + verificationUrl + "</a></p>" +
                "<p>This link will expire in 24 hours.</p>" +
                "<p>If you did not sign up, please ignore this email.</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Sends a password reset email with a token link.
     *
     * @param to    the recipient's email address
     * @param token the reset token (to be used in the link)
     */
    public void sendPasswordResetEmail(String to, String token) {
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Invalid or missing recipient email: " + to);
        }
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("SkillSwap – Password Reset Request");
            helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                "<h2>Password Reset Request</h2>" +
                "<p>We received a request to reset your password. Click the link below to set a new password:</p>" +
                "<p><a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
                "<p>If you did not request this, please ignore this email. Your password will not change.</p>" +
                "<p>This link will expire in 1 hour.</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Sends a plain‑text email with a review and a sentiment summary.
     */
    public void sendRecommendation(String to, String summary, String comment) {
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Invalid or missing recipient email: " + to);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("New User Review & Sentiment Analysis – SkillSwap");
        message.setText("Review:\n" + comment + "\n\nSentiment Summary:\n" + summary);
        mailSender.send(message);
    }

    /**
     * Sends a weekly ChatGPT analysis with business tips (HTML).
     */
    public void sendWeeklyAnalysis(String to, String analysisText) {
        if (!isValidEmail(to)) {
            throw new IllegalArgumentException("Invalid or missing recipient email: " + to);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("📊 SkillSwap – Weekly Business Review & Recommendations");
            helper.setText("<div style='font-family: Arial, sans-serif;'>" +
                    "<h3>Your Weekly Customer Review Analysis</h3>" +
                    "<p>" + analysisText.replaceAll("\n", "<br>") + "</p>" +
                    "<p>— SkillSwap Team</p>" +
                    "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send weekly analysis email", e);
        }
    }

    /**
     * Sends a contact form submission to a configured admin email (HTML).
     */
    public void sendContactEmail(ContactMessage contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contact message cannot be null");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(contactEmail); // configurable recipient
            helper.setSubject("New Contact Form Submission – SkillSwap");
            helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                "<h3>New Message from Contact Form</h3>" +
                "<p><strong>Name:</strong> " + contact.getName() + "</p>" +
                "<p><strong>Email:</strong> " + contact.getEmail() + "</p>" +
                "<p><strong>Phone:</strong> " + contact.getPhone() + "</p>" +
                "<p><strong>Message:</strong><br>" + contact.getMessage() + "</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send contact email", e);
        }
    }
}