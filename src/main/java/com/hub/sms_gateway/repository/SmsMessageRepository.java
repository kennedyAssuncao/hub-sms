package com.hub.sms_gateway.repository;

import com.hub.sms_gateway.entity.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {
}