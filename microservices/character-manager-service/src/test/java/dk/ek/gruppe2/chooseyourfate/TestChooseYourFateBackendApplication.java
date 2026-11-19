package dk.ek.gruppe2.chooseyourfate;

import org.springframework.boot.SpringApplication;

public class TestChooseYourFateBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChooseYourFateBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
