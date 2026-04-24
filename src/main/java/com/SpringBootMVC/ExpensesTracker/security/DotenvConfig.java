package com.SpringBootMVC.ExpensesTracker.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {

    @PostConstruct
    public void init() {
        Dotenv dotenv = Dotenv.load();

        System.setProperty("DB_URL", dotenv.get("DB_URL"));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

        System.setProperty("MAIL_USERNAME", dotenv.get("MAIL_USERNAME"));
        System.setProperty("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD"));

        System.setProperty("TWILIO_SID", dotenv.get("TWILIO_SID"));
        System.setProperty("TWILIO_TOKEN", dotenv.get("TWILIO_TOKEN"));
        System.setProperty("TWILIO_PHONE", dotenv.get("TWILIO_PHONE"));

        System.setProperty("HF_TOKEN", dotenv.get("HF_TOKEN"));
    }
}