package com.hub.sms_gateway.exception;

import com.hub.sms_gateway.gateway.ModemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ModemException.class)
    public ResponseEntity<Object> handleModemException(ModemException ex) {
        // Log the error for debugging purposes
        System.err.println("Modem Error: " + ex.getMessage() + " (Code: " + ex.getErrorCode() + ")");

        String userMessage = "Falha no envio do SMS. ";
        switch (ex.getErrorCode()) {
            case "305":
                userMessage += "Erro de formato de mensagem (provavelmente muito longa).";
                break;
            case "500":
                userMessage += "O modem não pôde enviar a mensagem. Verifique o sinal e o serviço.";
                break;
            default:
                userMessage += "Ocorreu um erro com o modem (Código: " + ex.getErrorCode() + ").";
                break;
        }

        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", userMessage,
                "modem_error_code", ex.getErrorCode()
        );

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        // Log the error for debugging purposes
        System.err.println("Internal Error: " + ex.getMessage());

        Map<String, Object> body = Map.of(
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal Server Error",
                "message", "Ocorreu um erro interno no servidor: " + ex.getMessage()
        );

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
