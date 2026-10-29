package com.SpringBootMVC.ExpensesTracker.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Value("${twilio.sid:}")
    private String sid;

    @Value("${twilio.token:}")
    private String token;

    @Value("${twilio.phone:}")
    private String from;

    @Override
    public void sendSms(String to, String message) {
        if (sid.isBlank() || token.isBlank() || from.isBlank()) {
            log.info("Twilio is not configured; SMS not sent");
            return;
        }
        Twilio.init(sid, token);

        Message.creator(
                new com.twilio.type.PhoneNumber(to),
                new com.twilio.type.PhoneNumber(from),
                message
        ).create();
    }
}
