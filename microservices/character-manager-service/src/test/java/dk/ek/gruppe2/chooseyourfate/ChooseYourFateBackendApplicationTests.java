package dk.ek.gruppe2.chooseyourfate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = ChooseYourFateBackendApplication.class)
class ChooseYourFateBackendApplicationTests {
	@Test
	void contextLoads() {
	}
}
