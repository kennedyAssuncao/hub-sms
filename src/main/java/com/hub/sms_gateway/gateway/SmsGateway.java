package com.hub.sms_gateway.gateway;

public interface SmsGateway {

    String send(String phone, String message);

}
