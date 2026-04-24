package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.service.ClientService;
import com.SpringBootMVC.ExpensesTracker.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class CustomeAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    UserService userService;
    ClientService clientService;

    @Autowired
    public CustomeAuthenticationSuccessHandler(UserService userService, ClientService clientService) {
        this.userService = userService;
        this.clientService = clientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response
            , Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        // ✅ User fetch
        User user = userService.findUserByUserName(username);
        if (user == null) {
            response.sendRedirect("/showLoginPage?error");
            return;
        }
        Client client = clientService.findClientById(user.getId());
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        // ✅ session attribute set
        session.setAttribute("client", client);
        session.setAttribute("username", username);
        // ✅ optional: session timeout
        session.setMaxInactiveInterval(30 * 60); // 30 min
        // ✅ redirect
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
