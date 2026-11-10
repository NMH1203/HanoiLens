package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Architecture test: module discovery, allowed dependencies, no cycles. */
class ModularityTests {

    private static final Set<String> EXPECTED_MODULES =
            Set.of("identity", "profile", "destination", "event", "visit", "media", "recap");

    private final ApplicationModules modules = ApplicationModules.of(HanoiLensApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void hasExactlyTheSevenBusinessModules() {
        Set<String> names = modules.stream()
                .map(m -> m.getName().replace("modules.", ""))
                .collect(Collectors.toSet());
        assertThat(names).isEqualTo(EXPECTED_MODULES);
    }
}
