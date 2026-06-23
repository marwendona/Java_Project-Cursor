package org.example.cursor;

import java.awt.*;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

public class MouseMover {
    private final Robot robot;
    private final Clock clock;

    public MouseMover() throws AWTException {
        this(new Robot(), Clock.systemDefaultZone());
    }

    MouseMover(Robot robot, Clock clock) {
        this.robot = Objects.requireNonNull(robot, "robot");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.robot.setAutoDelay(0);
    }

    public void run(MoverConfig config) throws InterruptedException {
        Objects.requireNonNull(config, "config").validate();

        var now = now();
        if (now.isBefore(config.startAt())) {
            sleepUntil(config.startAt());
        }

        boolean moveRight = true;
        while (now().isBefore(config.endAt())) {
            moveBy(moveRight ? config.movePixels() : -config.movePixels(), 0);
            moveRight = !moveRight;
            sleepUntil(min(now().plus(config.interval()), config.endAt()));
        }
    }

    private void moveBy(int deltaX, int deltaY) {
        var pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            throw new IllegalStateException("Unable to retrieve the current mouse position.");
        }

        var current = pointerInfo.getLocation();
        robot.mouseMove(current.x + deltaX, current.y + deltaY);
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock);
    }

    private void sleepUntil(ZonedDateTime target) throws InterruptedException {
        var sleepTime = Duration.between(now(), target);
        if (sleepTime.isPositive()) {
            Thread.sleep(sleepTime.toMillis());
        }
    }

    private static ZonedDateTime min(ZonedDateTime first, ZonedDateTime second) {
        return first.isBefore(second) ? first : second;
    }
}
