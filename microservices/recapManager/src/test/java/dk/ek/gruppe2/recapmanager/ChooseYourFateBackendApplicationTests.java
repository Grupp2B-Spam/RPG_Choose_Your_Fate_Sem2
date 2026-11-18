package dk.ek.gruppe2.recapmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import dk.ek.gruppe2.recapmanager.ChooseYourFateBackendApplication;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = ChooseYourFateBackendApplication.class)
class ChooseYourFateBackendApplicationTests {
	@Test
	void contextLoads() {
	}
}
