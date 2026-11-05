package com.finanzapp.lifehub.modules.finance.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:user_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/realms/test"
})
@ActiveProfiles("test")
class UserApplicationTests {

	@Test
	void contextLoads() {
	}

}
