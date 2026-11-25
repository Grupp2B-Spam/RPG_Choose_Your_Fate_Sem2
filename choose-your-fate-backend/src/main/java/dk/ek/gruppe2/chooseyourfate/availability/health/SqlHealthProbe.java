package dk.ek.gruppe2.chooseyourfate.availability.health;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;

public interface SqlHealthProbe {

    boolean isHealthy(DatabaseRole role);
}
