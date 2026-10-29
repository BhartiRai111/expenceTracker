package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.entity.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);
    private static final String FALLBACK_CATEGORY = "Other";

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Autowired
    private CategoryService categoryService;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .build();

    @Override
    public String predictCategory(String description) {
        if (description == null || description.isBlank() || apiKey.isBlank()) {
            return FALLBACK_CATEGORY;
        }
        try {
            List<String> names = categoryService.findAll().stream().map(Category::getName).toList();
            String prompt = "Classify this expense into exactly one of these categories: "
                    + String.join(", ", names) + ". Expense: " + description
                    + ". Only return the category name.";
            String answer = complete(List.of(Map.of("role", "user", "content", prompt))).trim();
            return names.stream().filter(n -> n.equalsIgnoreCase(answer)).findFirst().orElse(FALLBACK_CATEGORY);
        } catch (Exception e) {
            log.warn("Category prediction failed", e);
            return FALLBACK_CATEGORY;
        }
    }

    @Override
    public String getChatResponse(String message) {
        if (message == null || message.isBlank()) {
            return "Please type a question.";
        }
        if (apiKey.isBlank()) {
            return "The AI assistant is not configured.";
        }
        try {
            return complete(List.of(
                    Map.of("role", "system", "content",
                            "You are a concise assistant inside a personal expense tracker app. "
                                    + "Help with budgeting and expense questions."),
                    Map.of("role", "user", "content", message)));
        } catch (Exception e) {
            log.warn("Chat request failed", e);
            return "Sorry, the assistant is unavailable right now.";
        }
    }

    @SuppressWarnings("unchecked")
    private String complete(List<Map<String, String>> messages) {
        Map<String, Object> response = webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("model", model, "messages", messages))
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(30));
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString().trim();
    }
}
