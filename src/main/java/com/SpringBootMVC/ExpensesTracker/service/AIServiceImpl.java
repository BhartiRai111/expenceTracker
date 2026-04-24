package com.SpringBootMVC.ExpensesTracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {

    @Value("${hf.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .build();

    @Override
    public String predictCategory(String description) {

        try {

            String prompt = "Classify this expense into one category: " +
                    "Groceries, Utilities(bills), Transportation, Dining, Entertainment. " +
                    "Expense: " + description +
                    ". Only return category name.";

            Map response = webClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "model", "gpt-4o-mini",
                            "messages", new Object[]{
                                    Map.of("role", "user", "content", prompt)
                            }
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // extract response
            Map choice = (Map) ((java.util.List) response.get("choices")).get(0);
            Map message = (Map) choice.get("message");

            return message.get("content").toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Other"; // fallback
        }
    }
    @Override
    public String getChatResponse(String message) {

        try {

            WebClient client = WebClient.builder()
                    .baseUrl("https://api-inference.huggingface.co/models/distilbert-base-uncased")
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", message);

            Object response = client.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

}