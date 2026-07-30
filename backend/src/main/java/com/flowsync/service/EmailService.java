package com.flowsync.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * The SMTP account — Gmail authenticated sender, always used as From address
     */
    private final String smtpAccount;

    public EmailService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String smtpAccount) {
        this.mailSender = mailSender;
        this.smtpAccount = smtpAccount;
    }

    /**
     * Send an email from admin / user actions.
     * Gmail requires From = authenticated SMTP account.
     * The action-doer's email is shown as the display name + set as Reply-To.
     *
     * @param to          recipient email
     * @param senderEmail the person performing the action (admin email, etc.)
     * @param subject     email subject
     * @param text        plain-text body
     */
    public void sendEmail(String to, String senderEmail, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            if (senderEmail != null && !senderEmail.trim().isEmpty()) {
                // Set the From address directly to the action-doer's email (e.g. Admin 2)
                helper.setFrom(new InternetAddress(senderEmail.trim()));
                helper.setReplyTo(senderEmail.trim());
            } else {
                helper.setFrom(new InternetAddress(smtpAccount, "Sorim System"));
            }

            mailSender.send(mimeMessage);
            log.info("Email sent to {} (on behalf of: {})", to, senderEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send a system-only email (MFA codes, password reset, etc.)
     * Always from the SMTP account with no custom sender.
     */
    public void sendSystemEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            helper.setFrom(new InternetAddress(smtpAccount, "Sorim System"));
            mailSender.send(mimeMessage);
            log.info("System email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send system email to {}: {}", to, e.getMessage());
        }
    }
}
