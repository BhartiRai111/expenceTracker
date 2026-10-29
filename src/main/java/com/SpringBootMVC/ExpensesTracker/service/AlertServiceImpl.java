package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.ContactConfig;
import com.SpringBootMVC.ExpensesTracker.repository.ContactConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AlertServiceImpl implements AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Autowired
    private ContactConfigRepository contactRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SmsService smsService;

    /**
     * Notifies the client over every channel they configured, falling back to the details given at
     * registration. A failing channel is logged and never breaks the expense operation that triggered it.
     */
    @Override
    public void sendAlert(Client client, int totalExpense) {
        ContactConfig config = contactRepo.findByUserClientId(client.getId());
        String email = firstNonBlank(config == null ? null : config.getEmail(), client.getEmail());
        String phone = firstNonBlank(config == null ? null : config.getPhone(), client.getPhone());
        String whatsapp = firstNonBlank(config == null ? null : config.getWhatsappNumber(), client.getWhatsappNumber());

        String message = "Budget exceeded! This month's expenses: " + totalExpense
                + " (budget: " + client.getBudget() + ")";

        if (email != null) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(email);
                mail.setSubject("Budget Alert");
                mail.setText(message);
                mailSender.send(mail);
            } catch (Exception e) {
                log.warn("Could not send budget alert email for client {}", client.getId(), e);
            }
        }
        if (phone != null) {
            try {
                smsService.sendSms(phone, message);
            } catch (Exception e) {
                log.warn("Could not send budget alert SMS for client {}", client.getId(), e);
            }
        }
        if (whatsapp != null) {
            log.info("WhatsApp alerts are not implemented; skipped for client {}", client.getId());
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }
}
