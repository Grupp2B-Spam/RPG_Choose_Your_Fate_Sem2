package dk.ek.gruppe2.recapmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.security")
public class MockCharacterAccessProperties {
    private boolean mockCharacterAccess;
    private Map<Integer, String> mockCharacterOwners = new HashMap<>();

    public boolean isMockCharacterAccess() { return mockCharacterAccess; }
    public void setMockCharacterAccess(boolean mockCharacterAccess) { this.mockCharacterAccess = mockCharacterAccess; }
    public Map<Integer, String> getMockCharacterOwners() { return mockCharacterOwners; }
    public void setMockCharacterOwners(Map<Integer, String> owners) { this.mockCharacterOwners = owners; }
}
