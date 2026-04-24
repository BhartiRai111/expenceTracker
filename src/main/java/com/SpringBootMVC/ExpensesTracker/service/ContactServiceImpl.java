package com.SpringBootMVC.ExpensesTracker.service;

import com.SpringBootMVC.ExpensesTracker.DTO.ContactInfoDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.ContactConfig;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.repository.ContactConfigRepository;
import com.SpringBootMVC.ExpensesTracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactConfigRepository contactConfigRepository;

    @Override
    public void updateContact(String username, ContactInfoDTO dto) {

        User user = userRepository.findByUserName(username);

        ContactConfig config = contactConfigRepository.findByUserId(user.getId());

        if (config == null) {
            config = new ContactConfig();
            config.setUser(user);
        }

        config.setEmail(dto.getEmail());
        config.setPhone(dto.getPhone());
        config.setWhatsappNumber(dto.getWhatsappNumber());

        contactConfigRepository.save(config);
    }
}