package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.gateway.ModemException;
import com.hub.sms_gateway.gateway.SmsGateway;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import com.hub.sms_gateway.serial.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:sms-persistence-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SmsPersistenceIntegrationTest {

    private static final SmsRequest REQUEST = new SmsRequest("+5511999999999", "Teste de persistencia");

    @Autowired
    SmsService service;

    @Autowired
    SmsMessageRepository repository;

    @MockitoBean
    SmsGateway gateway;

    @MockitoBean
    SerialPortService serial;

    @LocalServerPort
    int port;

    @BeforeEach
    void clearHistory() {
        repository.deleteAll();
    }

    @Test
    void persistsPendingBeforeSendingAndSentAfterConfirmation() {
        String modemResponse = "+CMGS: 123\r\nOK\r\n";
        when(gateway.send(REQUEST.phone(), REQUEST.message())).thenAnswer(invocation -> {
            assertPendingIsPersisted();
            return modemResponse;
        });

        var response = service.send(REQUEST);

        SmsMessage saved = repository.findById(response.id()).orElseThrow();
        assertThat(saved.getPhone()).isEqualTo(REQUEST.phone());
        assertThat(saved.getMessage()).isEqualTo(REQUEST.message());
        assertThat(saved.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(saved.getSentAt()).isNotNull();
        assertThat(saved.getModemResponse()).isEqualTo(modemResponse);
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(response.status()).isEqualTo("SENT");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void persistsFailedEvenWhenSendThrows() {
        ModemException failure = new ModemException("Falha temporaria", "500");
        when(gateway.send(REQUEST.phone(), REQUEST.message())).thenAnswer(invocation -> {
            assertPendingIsPersisted();
            throw failure;
        });

        assertThatThrownBy(() -> service.send(REQUEST)).isSameAs(failure);

        assertThat(repository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getStatus()).isEqualTo(SmsStatus.FAILED);
            assertThat(saved.getErrorMessage()).isEqualTo(failure.getMessage());
            assertThat(saved.getSentAt()).isNull();
            assertThat(saved.getModemResponse()).isNull();
        });
    }

    @Test
    void servesH2ConsoleOverHttp() throws Exception {
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/h2-console")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("<title>H2 Console</title>");
        }
    }

    private void assertPendingIsPersisted() {
        assertThat(repository.findAll()).singleElement().satisfies(saved -> {
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStatus()).isEqualTo(SmsStatus.PENDING);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getSentAt()).isNull();
        });
    }
}
