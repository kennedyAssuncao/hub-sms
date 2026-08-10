package com.hub.sms_gateway.serial;

import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class SerialPortService {

    private final SerialPort serialPort;
    private static final int RESPONSE_TIMEOUT_MS = 10000; // 10 seconds for commands to respond

    public SerialPortService(
            @Value("${sms.modem.port}") String portName,
            @Value("${sms.modem.baud-rate}") int baudRate
    ) {
        this.serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        // We will manage timeouts manually in our read loop
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
    }

    @PostConstruct
    public void diagnosticarPortas() {
        System.out.println("==================================");
        System.out.println("PORTAS SERIAIS ENCONTRADAS");
        System.out.println("==================================");
        Arrays.stream(SerialPort.getCommPorts())
                .forEach(port -> {
                    System.out.println("Porta: " + port.getSystemPortName());
                    System.out.println("Descrição: " + port.getDescriptivePortName());
                    System.out.println("Fabricante: " + port.getManufacturer());
                    System.out.println("----------------------------------");
                });
    }

    public synchronized void open() {
        if (serialPort.isOpen()) {
            return;
        }
        if (!serialPort.openPort()) {
            throw new IllegalStateException(
                    "Não foi possível abrir a porta " + serialPort.getSystemPortName()
            );
        }
    }

    public synchronized void close() {
        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public synchronized String sendCommand(String command, String... expectedTerminators) {
        open();
        clearInputBuffer();

        try {
            serialPort.getOutputStream().write((command + "\r").getBytes(StandardCharsets.US_ASCII));
            serialPort.getOutputStream().flush();
            return waitForResponse(expectedTerminators);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao enviar comando AT: " + command, e);
        }
    }

    public String waitForResponse(String... terminators) {
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < RESPONSE_TIMEOUT_MS) {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                int bytesRead = serialPort.readBytes(buffer, buffer.length);
                if (bytesRead > 0) {
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
                    response.append(chunk);

                    String currentResponse = response.toString();

                    for (String terminator : terminators) {
                        if (currentResponse.contains(terminator)) {
                            return currentResponse;
                        }
                    }
                }
            }
            try {
                Thread.sleep(100); // Prevent busy-waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Aguardando resposta do modem, mas a thread foi interrompida.", e);
            }
        }
        throw new IllegalStateException("Timeout esperando pela resposta do modem. Resposta recebida: " + response);
    }

    public synchronized void writeRaw(byte[] data) {
        open();
        try {
            serialPort.getOutputStream().write(data);
            serialPort.getOutputStream().flush();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao escrever dados brutos na porta serial", e);
        }
    }

    private void clearInputBuffer() {
        if (serialPort.bytesAvailable() > 0) {
            byte[] buffer = new byte[serialPort.bytesAvailable()];
            serialPort.readBytes(buffer, buffer.length);
        }
    }
}
