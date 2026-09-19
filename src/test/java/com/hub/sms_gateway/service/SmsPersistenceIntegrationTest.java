package com.hub.sms_gateway.service;

import com.hub.sms_gateway.dto.SmsRequest;
import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.messaging.SmsQueuePublisher;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import com.hub.sms_gateway.serial.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sms-persistence-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class SmsPersistenceIntegrationTest {

    private static final SmsRequest REQUEST =
            new SmsRequest(
                    "+5512999999999",
                    "Teste de persistencia"
            );

    @Autowired
    private SmsService smsService;

    @Autowired
    private SmsMessageRepository repository;

    @MockitoBean
    private SmsQueuePublisher publisher;

    @MockitoBean
    private SerialPortService serialPortService;

    @BeforeEach
    void setUp() {

        repository.deleteAll();

        reset(publisher);
    }

    @Test
    void shouldPersistSmsAsPendingAndPublishToQueue() {

        var response = smsService.send(REQUEST);

        List<SmsMessage> messages =
                repository.findAll();

        assertThat(messages)
                .hasSize(1);

        SmsMessage saved =
                messages.getFirst();

        assertThat(saved.getId())
                .isNotNull();

        assertThat(saved.getPhone())
                .isEqualTo(REQUEST.phone());

        assertThat(saved.getMessage())
                .isEqualTo(REQUEST.message());

        assertThat(saved.getStatus())
                .isEqualTo(SmsStatus.PENDING);

        assertThat(saved.getCreatedAt())
                .isNotNull();

        assertThat(saved.getSentAt())
                .isNull();

        assertThat(saved.getErrorMessage())
                .isNull();

        assertThat(saved.getModemResponse())
                .isNull();

        assertThat(response.id())
                .isEqualTo(saved.getId());

        assertThat(response.status())
                .isEqualTo(SmsStatus.PENDING.name());

        verify(publisher)
                .publish(saved.getId());
    }

    @Test
    void shouldFindPersistedSmsById() {

        var response =
                smsService.send(REQUEST);

        var result =
                smsService.findById(
                        response.id()
                );

        assertThat(result.id())
                .isEqualTo(response.id());

        assertThat(result.phone())
                .isEqualTo(REQUEST.phone());

        assertThat(result.message())
                .isEqualTo(REQUEST.message());

        assertThat(result.status())
                .isEqualTo(SmsStatus.PENDING);
    }

    @Test
    void shouldFindSmsHistory() {

        smsService.send(
                new SmsRequest(
                        "+5512999999991",
                        "Mensagem 1"
                )
        );

        smsService.send(
                new SmsRequest(
                        "+5512999999992",
                        "Mensagem 2"
                )
        );

        var result =
                smsService.findAll(
                        0,
                        20,
                        null
                );

        assertThat(result.content())
                .hasSize(2);

        assertThat(result.totalElements())
                .isEqualTo(2);

        assertThat(result.page())
                .isZero();

        assertThat(result.size())
                .isEqualTo(20);
    }

    @Test
    void shouldFilterSmsHistoryByStatus() {

        smsService.send(REQUEST);

        var result =
                smsService.findAll(
                        0,
                        20,
                        SmsStatus.PENDING
                );

        assertThat(result.content())
                .hasSize(1);

        assertThat(result.content().getFirst().status())
                .isEqualTo(SmsStatus.PENDING);
    }
}
