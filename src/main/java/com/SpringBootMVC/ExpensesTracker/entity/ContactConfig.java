package com.SpringBootMVC.ExpensesTracker.entity;

import jakarta.persistence.*;

@Entity
public class ContactConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String email;
    private String phone;
    private String whatsappNumber;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

//    public ContactConfig(int id, String email, String phone, String whatsappNumber, User user) {
//        this.id = id;
//        this.email = email;
//        this.phone = phone;
//        this.whatsappNumber = whatsappNumber;
//        this.user = user;
//    }

    @Override
    public String toString() {
        return "ContactConfig{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", whatsappNumber='" + whatsappNumber + '\'' +
                ", user=" + user +
                '}';
    }

    // getters setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}