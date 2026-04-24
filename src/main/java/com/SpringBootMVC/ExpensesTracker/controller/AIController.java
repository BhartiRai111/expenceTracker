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

        String message = request.get("message");

        String reply = aiService.getChatResponse(message);

        return Map.of("reply", reply);
    }
}