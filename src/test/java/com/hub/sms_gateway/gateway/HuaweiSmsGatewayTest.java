package com.hub.sms_gateway.gateway;

import com.hub.sms_gateway.serial.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HuaweiSmsGatewayTest {

    @Mock
    private SerialPortService serialPortService;

    @InjectMocks
    private HuaweiSmsGateway huaweiSmsGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendSuccess() {
        when(serialPortService.sendCommand("AT", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGS=\"123456789\"",">","ERROR")).thenReturn(">");
        when(serialPortService.waitForResponse("OK","+CMS ERROR:","ERROR")).thenReturn("+CMGS: 123\r\nOK\r\n");

        String response = huaweiSmsGateway.send("123456789", "Test message");

        assertEquals("+CMGS: 123", response);
    }

    @Test
    void testSendFailure() {
        when(serialPortService.sendCommand("AT", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGS=\"123456789\"",">","ERROR")).thenReturn("ERROR");

        assertThrows(ModemException.class, () -> {
            huaweiSmsGateway.send("123456789", "Test message");
        });
    }

    @Test
    void testModemNotEnteringCompositionMode() {
        when(serialPortService.sendCommand("AT", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGS=\"123456789\"",">","ERROR")).thenReturn("UNEXPECTED RESPONSE");

        assertThrows(IllegalStateException.class, () -> {
            huaweiSmsGateway.send("123456789", "Test message");
        });
    }

    @Test
    void testMissingFinalConfirmation() {
        when(serialPortService.sendCommand("AT", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand(anyString(), anyString(), anyString())).thenReturn(">");
        when(serialPortService.waitForResponse("OK", "+CMS ERROR:", "ERROR")).thenReturn("OK");

        assertThrows(IllegalStateException.class, () -> {
            huaweiSmsGateway.send("123456789", "Test message");
        });
    }

    @Test
    void testCmsError() {
        when(serialPortService.sendCommand("AT", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"", "OK", "ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand(anyString(), anyString(), anyString())).thenReturn(">");
        when(serialPortService.waitForResponse("OK", "+CMS ERROR:", "ERROR")).thenReturn("+CMS ERROR: 500");

        assertThrows(ModemException.class, () -> {
            huaweiSmsGateway.send("123456789", "Test message");
        });
    }

    @Test
    void shouldNormalizeMessageBeforeSending() {

        // stubs de sucesso...

        huaweiSmsGateway.send("123456789","Olá João");

        verify(serialPortService)
                .writeRaw(argThat(bytes ->
                        new String(bytes,StandardCharsets.US_ASCII).equals("Ola Joao")));
    }
}