package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.ModemStatusResponse;
import com.hub.sms_gateway.serial.SerialPortService;
import org.springframework.stereotype.Service;

@Service
public class ModemService {

    private final SerialPortService serial;

    public ModemService(SerialPortService serial) {
        this.serial = serial;
    }

    public synchronized ModemStatusResponse getStatus() {

        String ati = serial.sendCommand("ATI");
        String cpin = serial.sendCommand("AT+CPIN?");
        String csq = serial.sendCommand("AT+CSQ");
        String cops = serial.sendCommand("AT+COPS?");
        String creg = serial.sendCommand("AT+CREG?");

        return new ModemStatusResponse(
                true,
                serial.getPortName(),
                extractManufacturer(ati),
                extractModel(ati),
                extractOperator(cops),
                extractSignal(csq),
                cpin.contains("+CPIN: READY"),
                isRegistered(creg)
        );
    }

    private String extractManufacturer(String response) {
        return extractLineValue(response, "Manufacturer:");
    }

    private String extractModel(String response) {
        return extractLineValue(response, "Model:");
    }

    private String extractOperator(String response) {

        int firstQuote = response.indexOf('"');
        int secondQuote = response.indexOf('"', firstQuote + 1);

        if (firstQuote >= 0 && secondQuote > firstQuote) {
            return response.substring(firstQuote + 1, secondQuote);
        }

        return null;
    }

    private Integer extractSignal(String response) {

        int index = response.indexOf("+CSQ:");

        if (index < 0) {
            return null;
        }

        String value = response
                .substring(index + 5)
                .trim()
                .split(",")[0]
                .trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isRegistered(String response) {
        return response.contains("+CREG: 0,1")
                || response.contains("+CREG: 0,5");
    }

    private String extractLineValue(
            String response,
            String prefix
    ) {

        return response.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .findFirst()
                .orElse(null);
    }
}