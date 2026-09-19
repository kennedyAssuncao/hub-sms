package com.hub.sms_gateway.controller;

import com.hub.sms_gateway.entity.SmsMessage;
import com.hub.sms_gateway.entity.SmsStatus;
import com.hub.sms_gateway.repository.SmsMessageRepository;
import com.hub.sms_gateway.serial.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sms-history-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SmsHistoryIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    SmsMessageRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    SerialPortService serial;

    @BeforeEach
    void clearHistory() {
        repository.deleteAll();
    }

    @Test
    void returnsEmptyHistoryWithDefaultPagination() throws Exception {
        mvc.perform(get("/api/v1/sms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
        verifyNoInteractions(serial);
    }

    @Test
    void paginatesByCreationTimeThenIdDescending() throws Exception {
        var first = save(SmsStatus.SENT);
        var second = save(SmsStatus.PENDING);
        var oldest = save(SmsStatus.FAILED);
        jdbc.update("UPDATE sms_message SET created_at = TIMESTAMP '2026-01-02 12:00:00'");
        jdbc.update("UPDATE sms_message SET created_at = TIMESTAMP '2026-01-01 12:00:00' WHERE id = ?", oldest.getId());

        mvc.perform(get("/api/v1/sms").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[1].id").value(first.getId()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/sms").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(oldest.getId()))
                .andExpect(jsonPath("$.page").value(1));
        mvc.perform(get("/api/v1/sms").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3));
        verifyNoInteractions(serial);
    }

    @ParameterizedTest
    @EnumSource(SmsStatus.class)
    void filtersBeforePaginating(SmsStatus filter) throws Exception {
        for (SmsStatus status : SmsStatus.values()) {
            save(status);
            save(status);
        }
        mvc.perform(get("/api/v1/sms").param("status", filter.name()).param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value(filter.name()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void returnsEmptyPageWhenFilterHasNoMatches() throws Exception {
        save(SmsStatus.SENT);
        mvc.perform(get("/api/v1/sms").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @EnumSource(SmsStatus.class)
    void returnsDetailsById(SmsStatus smsStatus) throws Exception {
        var sms = save(smsStatus);
        var result = mvc.perform(get("/api/v1/sms/{id}", sms.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sms.getId()))
                .andExpect(jsonPath("$.phone").value(sms.getPhone()))
                .andExpect(jsonPath("$.message").value(sms.getMessage()))
                .andExpect(jsonPath("$.status").value(smsStatus.name()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        if (smsStatus == SmsStatus.SENT) {
            result.andExpect(jsonPath("$.sentAt").isNotEmpty())
                    .andExpect(jsonPath("$.modemResponse").value("+CMGS: 123\r\nOK"))
                    .andExpect(jsonPath("$.errorMessage").value(nullValue()));
        } else {
            result.andExpect(jsonPath("$.sentAt").value(nullValue()))
                    .andExpect(jsonPath("$.modemResponse").value(nullValue()));
            if (smsStatus == SmsStatus.FAILED) {
                result.andExpect(jsonPath("$.errorMessage").value("Falha temporaria"));
            }
        }
        verifyNoInteractions(serial);
    }

    @Test
    void returnsStructured404ForMissingId() throws Exception {
        mvc.perform(get("/api/v1/sms/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("SMS_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/sms/9223372036854775807"));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,abc", "page,2147483648", "size,0", "size,-1", "size,101", "size,abc", "status,INVALID"})
    void rejectsInvalidQueryParameters(String parameter, String value) throws Exception {
        mvc.perform(get("/api/v1/sms").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields." + parameter).isNotEmpty());
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        mvc.perform(get("/api/v1/sms/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.id").isNotEmpty());
    }

    private SmsMessage save(SmsStatus status) {
        SmsMessage sms = new SmsMessage("+5511999999999", "Mensagem de teste");
        if (status == SmsStatus.SENT) {
            sms.markAsSent("+CMGS: 123\r\nOK");
        } else if (status == SmsStatus.FAILED) {
            sms.markAsFailed("Falha temporaria");
        }
        return repository.save(sms);
    }
}
