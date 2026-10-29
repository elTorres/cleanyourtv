package io.github.eltorres.seguirviendo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SyncSchedulerTest {

    @Test
    public void periodicJobRequiresNetwork() {
        SyncScheduler.Spec periodic = SyncScheduler.periodic();
        assertTrue(periodic.requiresNetwork);
        assertTrue(periodic.persisted);
        assertEquals(60L * 60 * 1000, periodic.periodMs);
        SyncScheduler.Spec soon = SyncScheduler.soon();
        assertTrue(soon.requiresNetwork);
        assertFalse(soon.persisted);
        assertEquals(0, soon.periodMs);
        assertNotEquals(periodic.id, soon.id);
    }
}
