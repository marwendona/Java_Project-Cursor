package org.example.cursor;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

class MouseMoverInteractiveTest {
    private static final String ERROR_MSG = "Test ignored: graphical environment unavailable.";

    @Test
    @EnabledIfSystemProperty(named = "cursor.mover.file.enabled", matches = "true")
    void movesMouseCursorUsingConfigFile() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), ERROR_MSG);
        var configFile = System.getProperty("cursor.mover.config.file", "src/test/resources/cursor-mover.properties");
        var config = MoverConfig.fromPropertiesFile(Path.of(configFile));
        new MouseMover().run(config);
    }

    @Test
    @EnabledIfSystemProperty(named = "cursor.mover.enabled", matches = "true")
    void movesMouseCursorOnConfiguredSchedule() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), ERROR_MSG);
        var config = MoverConfig.fromSystemPropertiesOrEnv();
        new MouseMover().run(config);
    }

    @Test
    @EnabledIfSystemProperty(named = "cursor.mover.example.enabled", matches = "true")
    void movesMouseUsingQuickExample() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), ERROR_MSG);
        var now = ZonedDateTime.now(ZoneId.systemDefault());
        var config = new MoverConfig(
                now.plusSeconds(2),
                now.plusSeconds(12),
                Duration.ofSeconds(1),
                10
        );
        new MouseMover().run(config);
    }
}
