package com.projectlos.gw_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.config.import-check.enabled=false"
})
class GwServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
