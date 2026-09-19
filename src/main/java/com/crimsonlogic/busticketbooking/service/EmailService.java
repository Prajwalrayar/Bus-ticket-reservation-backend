package com.crimsonlogic.busticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(@Autowired(required = false) JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Send an email notifying the passenger that an agent has replied to their ticket.
     */
    public void sendTicketReplyEmail(String toEmail, String ticketId, String passengerName) {
        String subject = "Update on your Support Ticket #" + ticketId;
        String text = "Dear " + passengerName + ",\n\n" +
                "A support agent has replied to your ticket (Ticket ID: " + ticketId + ").\n" +
                "Please log in to your account and check the support dashboard to view the message.\n\n" +
                "Best regards,\n" +
                "Bus Ticket Booking Support Team";
        
        sendSimpleMessage(toEmail, subject, text);
    }

    /**
     * Send an email notifying the passenger that their ticket is resolved.
     */
    public void sendTicketResolvedEmail(String toEmail, String ticketId, String passengerName) {
        String subject = "Support Ticket Resolved #" + ticketId;
        String text = "Dear " + passengerName + ",\n\n" +
                "Your support ticket (Ticket ID: " + ticketId + ") has been marked as RESOLVED.\n" +
                "If you need further assistance, please feel free to create a new ticket or reply to the existing one.\n\n" +
                "Best regards,\n" +
                "Bus Ticket Booking Support Team";
        
        sendSimpleMessage(toEmail, subject, text);
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        if (emailSender == null) {
            log.warn("JavaMailSender is not configured. Simulating email to: {}. Subject: {}", to, subject);
            log.info("Email Content:\n{}", text);
            return;
        }
        
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("spring.mail.username is not configured. Simulating email to: {}. Subject: {}", to, subject);
            log.info("Email Content:\n{}", text);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to: {}. Reason: {}", to, e.getMessage());
            log.info("Failed Email Content:\n{}", text);
        }
    }
}
