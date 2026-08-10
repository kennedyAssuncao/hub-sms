package com.hub.sms_gateway.gateway;

import com.hub.sms_gateway.serial.SerialPortService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HuaweiSmsGateway implements SmsGateway {

    private final SerialPortService serial;
    private static final Pattern CMS_ERROR_PATTERN = Pattern.compile("\\+CMS ERROR: (\\d+)");

    public HuaweiSmsGateway(SerialPortService serial) {
        this.serial = serial;
    }

    @Override
    public synchronized String send(String phone, String message) {
        try {
            // Configure modem for SMS sending
            validateResponse(serial.sendCommand("AT", "OK", "ERROR"));
            validateResponse(serial.sendCommand("AT+CMGF=1", "OK", "ERROR")); // Text mode
            validateResponse(serial.sendCommand("AT+CSCS=\"GSM\"", "OK", "ERROR")); // Character set

            // Send the SMS command and wait for the prompt
            String promptResponse = serial.sendCommand("AT+CMGS=\"" + phone + "\"", ">", "ERROR");
            validateResponse(promptResponse);

            if (!promptResponse.contains(">")) {
                throw new IllegalStateException("Modem não entrou em modo de composição: " + promptResponse);
            }

            // Send the message content and CTRL+Z
            serial.writeRaw(message.getBytes(StandardCharsets.US_ASCII));
            serial.writeRaw(new byte[]{26}); // CTRL+Z to send

            // Wait for the final response (+CMGS or ERROR)
            String finalResponse = serial.waitForResponse("+CMGS", "ERROR", "OK");
            validateResponse(finalResponse);

            return finalResponse;

        } catch (IllegalStateException e) {
            // Re-throw internal exceptions
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions
            throw new IllegalStateException("Ocorreu um erro inesperado durante o envio do SMS.", e);
        }
    }

    private void validateResponse(String response) {
        if (response.contains("ERROR")) {
            Matcher matcher = CMS_ERROR_PATTERN.matcher(response);
            if (matcher.find()) {
                String errorCode = matcher.group(1);
                throw new ModemException("O modem retornou um erro.", errorCode);
            }
            throw new IllegalStateException("O modem retornou um erro desconhecido: " + response);
        }
    }
}
