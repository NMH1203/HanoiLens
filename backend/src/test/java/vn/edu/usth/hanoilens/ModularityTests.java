package vn.edu.usth.hanoilens;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Architecture guard (docs/PROJECT_STRUCTURE_AND_NAMING.md): no cycles, no access to another module's internals. */
class ModularityTests {

    @Test
    void modulesAreWellFormed() {
        ApplicationModules.of(HanoiLensApplication.class).verify();
    }
}
