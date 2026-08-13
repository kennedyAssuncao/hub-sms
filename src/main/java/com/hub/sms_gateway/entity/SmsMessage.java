package com.hub.sms_gateway.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_message")
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 2000)
    private String modemResponse;

    protected SmsMessage() {
    }

    public SmsMessage(String phone, String message) {
        this.phone = phone;
        this.message = message;
        this.status = SmsStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsSent(String modemResponse) {
        this.status = SmsStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.modemResponse = modemResponse;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = SmsStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getMessage() {
        return message;
    }

    public SmsStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getModemResponse() {
        return modemResponse;
    }
}