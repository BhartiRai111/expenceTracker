package com.SpringBootMVC.ExpensesTracker.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.sid}")
    private String sid;

    @Value("${twilio.token}")
    private String token;

    @Value("${twilio.phone}")
    private String from;

    @Override
    public void sendSms(String to, String message) {

        Twilio.init(sid, token);

        Message.creator(
                new com.twilio.type.PhoneNumber(to),
                new com.twilio.type.PhoneNumber(from),
                message
        ).create();
    }
}
