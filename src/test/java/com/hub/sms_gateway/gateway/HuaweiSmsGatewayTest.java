package com.hub.sms_gateway.gateway;

import com.hub.sms_gateway.serial.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class HuaweiSmsGatewayTest {

    @Mock
    private SerialPortService serialPortService;

    @InjectMocks
    private HuaweiSmsGateway huaweiSmsGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void mockSuccessfulInitialization(String phone) {

        when(serialPortService.sendCommand("AT","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGS=\"" + phone + "\"",">","ERROR")).thenReturn(">");
    }

    @Test
    void testSendSuccess() {

        String phone = "123456789";

        mockSuccessfulInitialization(phone);

        String modemResponse = "+CMGS: 123\r\n\r\nOK\r\n";

        when(serialPortService.waitForResponse("OK","+CMS ERROR:","ERROR")).thenReturn(modemResponse);

        String response = huaweiSmsGateway.send(phone,"Test message");

        assertEquals(modemResponse, response);

        verify(serialPortService).writeRaw("Test message".getBytes(StandardCharsets.US_ASCII));
        verify(serialPortService).writeRaw(argThat(bytes ->bytes != null&& bytes.length == 1&& bytes[0] == 26));
    }

    @Test
    void testSendFailure() {

        String phone = "123456789";

        when(serialPortService.sendCommand("AT","OK","ERROR")).thenReturn("ERROR");

        assertThrows(ModemException.class,() -> huaweiSmsGateway.send(phone,"Test message"));
    }

    @Test
    void testModemNotEnteringCompositionMode() {

        String phone = "123456789";

        when(serialPortService.sendCommand("AT","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGF=1","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CSCS=\"GSM\"","OK","ERROR")).thenReturn("OK");
        when(serialPortService.sendCommand("AT+CMGS=\"" + phone + "\"",">","ERROR")).thenReturn("UNEXPECTED RESPONSE");

        assertThrows(IllegalStateException.class,() -> huaweiSmsGateway.send(phone,"Test message"));
    }

    @Test
    void testMissingFinalConfirmation() {

        String phone = "123456789";

        mockSuccessfulInitialization(phone);

        when(serialPortService.waitForResponse("OK","+CMS ERROR:","ERROR")).thenReturn("OK");

        IllegalStateException exception = assertThrows(IllegalStateException.class,() -> huaweiSmsGateway.send(phone,"Test message"));

        assertTrue(exception.getMessage().contains("SMS não foi confirmado"));
    }

    @Test
    void testCmsError() {

        String phone = "123456789";

        mockSuccessfulInitialization(phone);

        when(serialPortService.waitForResponse("OK","+CMS ERROR:","ERROR")).thenReturn("+CMS ERROR: 500");

        assertThrows(ModemException.class,() -> huaweiSmsGateway.send(phone,"Test message"));
    }

    @Test
    void shouldNormalizeMessageBeforeSending() {

        String phone = "123456789";

        mockSuccessfulInitialization(phone);

        when(serialPortService.waitForResponse("OK","+CMS ERROR:","ERROR"))
                .thenReturn("+CMGS: 321\r\n\r\nOK\r\n");

        huaweiSmsGateway.send(phone,"Olá João");

        verify(serialPortService).writeRaw(argThat(bytes ->
                                bytes != null && new String(bytes,StandardCharsets.US_ASCII).equals("Ola Joao")));
    }
}