package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.ModemStatusResponse;
import com.hub.sms_gateway.serial.AtCommandClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModemServiceTest {

    private AtCommandClient serial;
    private ModemService modemService;

    @BeforeEach
    void setUp() {
        serial = mock(AtCommandClient.class);
        modemService = new ModemService(serial);
    }

    @Test
    void shouldReturnModemStatus() {

        when(serial.getPortName())
                .thenReturn("COM5");

        when(serial.sendCommand("ATI"))
                .thenReturn("""
                    ATI
                    Manufacturer: huawei
                    Model: E3276
                    Revision: 21.260.05.00.149
                    OK
                    """);

        when(serial.sendCommand("AT+CPIN?"))
                .thenReturn("""
                    +CPIN: READY
                    OK
                    """);

        when(serial.sendCommand("AT+CSQ"))
                .thenReturn("""
                    +CSQ: 17,99
                    OK
                    """);

        when(serial.sendCommand("AT+COPS?"))
                .thenReturn("""
                    +COPS: 0,0,"TIM",7
                    OK
                    """);

        when(serial.sendCommand("AT+CREG?"))
                .thenReturn("""
                    +CREG: 0,1
                    OK
                    """);

        ModemStatusResponse result =
                modemService.getStatus();

        assertTrue(result.connected());
        assertEquals("COM5", result.port());
        assertEquals("huawei", result.manufacturer());
        assertEquals("E3276", result.model());
        assertEquals("TIM", result.operator());
        assertEquals(17, result.signal());
        assertTrue(result.simReady());
        assertTrue(result.networkRegistered());
    }

    @Test
    void shouldReturnSimNotReadyWhenPinIsRequired() {

        mockDefaultResponses();

        when(serial.sendCommand("AT+CPIN?"))
                .thenReturn("+CPIN: SIM PIN\r\nOK\r\n");

        ModemStatusResponse result =
                modemService.getStatus();

        assertFalse(result.simReady());
    }

    @Test
    void shouldReturnNetworkNotRegistered() {

        mockDefaultResponses();

        when(serial.sendCommand("AT+CREG?"))
                .thenReturn("+CREG: 0,0\r\nOK\r\n");

        ModemStatusResponse result =
                modemService.getStatus();

        assertFalse(result.networkRegistered());
    }

    @Test
    void shouldReturnNetworkRegisteredWhenRoaming() {

        mockDefaultResponses();

        when(serial.sendCommand("AT+CREG?"))
                .thenReturn("+CREG: 0,5\r\nOK\r\n");

        ModemStatusResponse result =
                modemService.getStatus();

        assertTrue(result.networkRegistered());
    }

    private void mockDefaultResponses() {

        when(serial.getPortName())
                .thenReturn("COM5");

        when(serial.sendCommand("ATI"))
                .thenReturn("""
                    Manufacturer: huawei
                    Model: E3276
                    OK
                    """);

        when(serial.sendCommand("AT+CPIN?"))
                .thenReturn("+CPIN: READY\r\nOK\r\n");

        when(serial.sendCommand("AT+CSQ"))
                .thenReturn("+CSQ: 17,99\r\nOK\r\n");

        when(serial.sendCommand("AT+COPS?"))
                .thenReturn("+COPS: 0,0,\"TIM\",7\r\nOK\r\n");

        when(serial.sendCommand("AT+CREG?"))
                .thenReturn("+CREG: 0,1\r\nOK\r\n");
    }
}