package com.hub.sms_gateway.messaging;

import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.gateway.SmsGateway;
import com.hub.sms_gateway.messaging.dto.SmsSendMessage;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsQueueConsumerTest {

    private static final Long SMS_ID = 1L;
    private static final String PHONE = "+5512999999999";
    private static final String MESSAGE = "Teste RabbitMQ";

    @Mock
    private SmsMessageRepository repository;

    @Mock
    private SmsGateway smsGateway;

    @Mock
    private SmsQueuePublisher publisher;

    @InjectMocks
    private SmsQueueConsumer consumer;

    private SmsMessage sms;

    @BeforeEach
    void setUp() {
        sms = new SmsMessage(PHONE, MESSAGE);
    }

    @Test
    void shouldSendPendingSmsAndMarkAsSent() {

        var queueMessage = new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        when(smsGateway.send(PHONE, MESSAGE))
                .thenReturn("+CMGS: 38\r\n\r\nOK\r\n");

        consumer.consume(queueMessage);

        assertThat(sms.getStatus())
                .isEqualTo(SmsStatus.SENT);

        assertThat(sms.getSentAt())
                .isNotNull();

        assertThat(sms.getModemResponse())
                .contains("+CMGS: 38")
                .contains("OK");

        assertThat(sms.getErrorMessage())
                .isNull();

        verify(smsGateway)
                .send(PHONE, MESSAGE);

        verify(repository, times(2))
                .save(sms);
    }

    @Test
    void shouldScheduleRetryWhenGatewayFails() {

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        when(smsGateway.send(PHONE, MESSAGE))
                .thenThrow(
                        new RuntimeException(
                                "Falha temporaria"
                        )
                );

        consumer.consume(queueMessage);

        assertThat(sms.getStatus())
                .isEqualTo(SmsStatus.RETRYING);

        assertThat(sms.getRetryCount())
                .isEqualTo(1);

        assertThat(sms.getErrorMessage())
                .isEqualTo("Falha temporaria");

        verify(publisher)
                .publishRetry(SMS_ID);

        verify(repository, times(2))
                .save(sms);
    }

    @Test
    void shouldProcessSmsInRetryingStatus() {

        sms.markAsRetrying(
                "Falha anterior"
        );

        sms.incrementRetryCount();

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        when(smsGateway.send(PHONE, MESSAGE))
                .thenReturn(
                        "+CMGS: 38\r\n\r\nOK\r\n"
                );

        consumer.consume(queueMessage);

        assertThat(sms.getStatus())
                .isEqualTo(SmsStatus.SENT);

        assertThat(sms.getRetryCount())
                .isEqualTo(1);

        verify(smsGateway)
                .send(PHONE, MESSAGE);

        verify(publisher, never())
                .publishRetry(anyLong());
    }

    @Test
    void shouldMarkAsFailedWhenRetriesAreExhausted() {

        sms.incrementRetryCount();
        sms.incrementRetryCount();
        sms.incrementRetryCount();

        sms.markAsRetrying(
                "Falha anterior"
        );

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        when(smsGateway.send(PHONE, MESSAGE))
                .thenThrow(
                        new RuntimeException(
                                "Modem indisponivel"
                        )
                );

        assertThatThrownBy(() ->
                consumer.consume(queueMessage)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Modem indisponivel");

        assertThat(sms.getStatus())
                .isEqualTo(SmsStatus.FAILED);

        assertThat(sms.getRetryCount())
                .isEqualTo(4);

        verify(publisher, never())
                .publishRetry(anyLong());
    }

    @Test
    void shouldNotProcessAlreadySentSms() {

        sms.markAsSent(
                "+CMGS: 38\r\n\r\nOK\r\n"
        );

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        consumer.consume(queueMessage);

        verifyNoInteractions(smsGateway);

        verify(repository, never())
                .save(any(SmsMessage.class));
    }

    @Test
    void shouldNotProcessFailedSms() {

        sms.markAsFailed("Falha anterior");

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        consumer.consume(queueMessage);

        verifyNoInteractions(smsGateway);

        verify(repository, never())
                .save(any(SmsMessage.class));
    }

    @Test
    void shouldThrowExceptionWhenSmsDoesNotExist() {

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                consumer.consume(queueMessage)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "SMS não encontrado"
                );

        verifyNoInteractions(smsGateway);

        verify(repository, never())
                .save(any(SmsMessage.class));
    }

    @Test
    void shouldMarkSmsAsProcessingBeforeSending() {

        var queueMessage =
                new SmsSendMessage(SMS_ID);

        when(repository.findById(SMS_ID))
                .thenReturn(Optional.of(sms));

        when(smsGateway.send(PHONE, MESSAGE))
                .thenAnswer(invocation -> {

                    assertThat(sms.getStatus())
                            .isEqualTo(
                                    SmsStatus.PROCESSING
                            );

                    return "+CMGS: 38\r\n\r\nOK\r\n";
                });

        consumer.consume(queueMessage);

        assertThat(sms.getStatus())
                .isEqualTo(SmsStatus.SENT);

        verify(repository, times(2))
                .save(sms);
    }
}