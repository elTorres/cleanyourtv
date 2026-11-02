package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MenuPlacementTest {

    @Test
    public void fitsBelowAlignsToCardStart() {
        MenuPlacement ltr = MenuPlacement.nextTo(110, 250, 445, 440, 420, 270, 1920, 1080, 20, false);
        assertEquals(110, ltr.x());
        assertEquals(460, ltr.y());
        assertFalse(ltr.above());

        MenuPlacement rtl = MenuPlacement.nextTo(1500, 250, 1835, 440, 420, 270, 1920, 1080, 20, true);
        assertEquals(1415, rtl.x());
    }

    @Test
    public void opensAboveWhenBelowDoesNotFit() {
        MenuPlacement placement = MenuPlacement.nextTo(690, 775, 950, 925, 490, 270, 1920, 1080, 20, false);

        assertEquals(690, placement.x());
        assertEquals(485, placement.y());
        assertTrue(placement.above());
    }

    @Test
    public void alignsToCardEndNearTheEndEdge() {
        MenuPlacement ltr = MenuPlacement.nextTo(1523, 250, 1867, 440, 425, 270, 1920, 1080, 20, false);
        assertEquals(1442, ltr.x());
        assertEquals(460, ltr.y());

        MenuPlacement rtl = MenuPlacement.nextTo(53, 250, 397, 440, 425, 270, 1920, 1080, 20, true);
        assertEquals(53, rtl.x());

        MenuPlacement offScreen = MenuPlacement.nextTo(1890, 250, 2225, 440, 420, 270, 1920, 1080, 20, false);
        assertEquals(1500, offScreen.x());
    }

    @Test
    public void centersWithoutBounds() {
        MenuPlacement placement = MenuPlacement.centered(400, 300, 1920, 1080);

        assertEquals(760, placement.x());
        assertEquals(390, placement.y());
        assertFalse(placement.above());
    }
}
