package org.fantom.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.fail;

public class WidgetTest {
    @Test
    public void cannotCreateWidgetWithNegativeWidth() {
        try {
            new Widget<>(0,0,0,0,-10,0, new Date());
            fail("Created widget with invalid width");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void cannotCreateWidgetWithNegativeHeight() {
        try {
            new Widget<>(0,0,0,0,0,-10, new Date());
            fail("Created widget with invalid height");
        } catch (IllegalArgumentException ignored) {
        }
    }
}
