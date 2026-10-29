package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AIController {

    @Autowired
    private AIService aiService;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        return Map.of("reply", aiService.getChatResponse(request.get("message")));
    }

    @PostMapping("/suggest-category")
    public Map<String, String> suggestCategory(@RequestBody Map<String, String> request) {
        return Map.of("category", aiService.predictCategory(request.get("description")));
    }
}
