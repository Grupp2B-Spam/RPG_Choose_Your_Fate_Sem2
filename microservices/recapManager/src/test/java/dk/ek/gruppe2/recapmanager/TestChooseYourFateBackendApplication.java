package dk.ek.gruppe2.recapmanager;

import org.springframework.boot.SpringApplication;

import dk.ek.gruppe2.recapmanager.ChooseYourFateBackendApplication;

public class TestChooseYourFateBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChooseYourFateBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
