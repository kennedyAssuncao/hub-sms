package com.hub.sms_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.hub.sms_gateway.serial.SerialPortService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:context-test")
class SmsGatewayApplicationTests {

	@MockitoBean
	SerialPortService serialPortService;

	@Test
	void contextLoads() {
	}

}
