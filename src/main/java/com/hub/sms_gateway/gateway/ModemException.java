package com.hub.sms_gateway.gateway;

public class ModemException extends RuntimeException {

    private final String errorCode;

    public ModemException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static ModemException fromResponse(String response) {
        if (response.contains("+CMS ERROR:")) {
            int startIndex = response.indexOf("+CMS ERROR:") + "+CMS ERROR:".length();
            String errorCode = response.substring(startIndex).trim();
            return new ModemException("O modem retornou um erro CMS: " + errorCode, errorCode);
        }
        return new ModemException("Erro desconhecido do modem: " + response, "-1");
    }
}
