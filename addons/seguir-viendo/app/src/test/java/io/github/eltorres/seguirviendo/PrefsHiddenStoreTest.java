package io.github.eltorres.seguirviendo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrefsHiddenStoreTest {

    @Test
    public void storedKeysRoundTripCappedOldestFirst() {
        String stored = "";
        for (int i = 0; i < PrefsHiddenStore.MAX_KEYS + 5; i++) stored = PrefsHiddenStore.append(stored, "trakt|show:" + i);
        stored = PrefsHiddenStore.append(stored, "trakt|show:10");
        Set<String> keys = PrefsHiddenStore.parse(stored);
        assertEquals(PrefsHiddenStore.MAX_KEYS, keys.size());
        assertFalse(keys.contains("trakt|show:0"));
        assertTrue(keys.contains("trakt|show:10"));
        List<String> ordered = new ArrayList<>(keys);
        assertEquals("trakt|show:10", ordered.get(ordered.size() - 1));
    }

    @Test
    public void emptyStoreHidesNothing() {
        assertTrue(PrefsHiddenStore.parse("").isEmpty());
        assertTrue(PrefsHiddenStore.parse(null).isEmpty());
    }
}
