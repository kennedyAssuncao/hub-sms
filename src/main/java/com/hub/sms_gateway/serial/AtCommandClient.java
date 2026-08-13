package com.hub.sms_gateway.serial;

public interface AtCommandClient {

    String sendCommand(String command, String... expectedTerminators);

    String getPortName();
}