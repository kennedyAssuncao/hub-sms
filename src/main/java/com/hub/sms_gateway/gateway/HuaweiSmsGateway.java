package com.hub.sms_gateway.gateway;

import com.hub.sms_gateway.serial.SerialPortService;
import com.hub.sms_gateway.util.SmsTextNormalizer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class HuaweiSmsGateway implements SmsGateway {

    private final SerialPortService serial;

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

            String normalizedMessage = SmsTextNormalizer.normalize(message);

            serial.writeRaw(normalizedMessage.getBytes(StandardCharsets.US_ASCII));
            serial.writeRaw(new byte[]{26});

            String finalResponse = serial.waitForResponse("OK", "+CMS ERROR:", "ERROR");

            validateResponse(finalResponse);

            if (!finalResponse.contains("+CMGS:")) {
                throw new IllegalStateException(
                        "SMS não foi confirmado pelo modem: " + finalResponse
                );
            }

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

        if (response.contains("+CMS ERROR:")) {
            throw ModemException.fromResponse(response);
        }

        if (response.contains("ERROR")) {
            throw new ModemException("O modem retornou erro: " + response,"-1");
        }
    }
}
