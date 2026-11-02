package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MenuPlacementTest {

    @Test
    public void centersWithoutBounds() {
        MenuPlacement placement = MenuPlacement.centered(400, 300, 1920, 1080);

        assertEquals(760, placement.x());
        assertEquals(390, placement.y());
        assertFalse(placement.above());
    }
}
