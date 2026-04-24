package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.ContactConfig;
import com.SpringBootMVC.ExpensesTracker.repository.ContactConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private ContactConfigRepository contactRepo;

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendAlert(int clientId, int totalExpense) {
        ContactConfig config = contactRepo.findByUserId(clientId);
        if (config == null) return;
        String message = "⚠ Budget exceeded! Total expense: " + totalExpense;

        if (config.getEmail() != null) {
            sendEmail(config.getEmail(), message);
        }
        if (config.getPhone() != null) {
            sendSMS(config.getPhone(), message);
        }
        if (config.getWhatsappNumber() != null) {
            sendWhatsApp(config.getWhatsappNumber(), message);
        }
    }

    private void sendSMS(String phone, String msg) {
        System.out.println("📱 SMS → " + phone + " : " + msg);
    }

    private void sendWhatsApp(String number, String msg) {
        System.out.println("💬 WhatsApp → " + number + " : " + msg);
    }


    private void sendEmail(String to, String msg) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject("Budget Alert 🚨");
        mail.setText(msg);

        mailSender.send(mail);
    }
}
