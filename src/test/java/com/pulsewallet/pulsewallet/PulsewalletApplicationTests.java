package com.pulsewallet.pulsewallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "pulsewallet.security.jwt.secret=test-only-secret-key-at-least-32-bytes-long")
class PulsewalletApplicationTests {

	@Test
	void contextLoads() {
	}

}
