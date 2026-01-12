package ru.binarysimple.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class NotificationContact {
    @Email
    @NotBlank
    @Column(name = "email", nullable = false)
    String email;
    
    @Column(name = "phone")
    String phone;
    
    @Column(name = "telegram_username")
    String telegramUsername;
    
    @Column(name = "whatsapp_username")
    String whatsappUsername;
}