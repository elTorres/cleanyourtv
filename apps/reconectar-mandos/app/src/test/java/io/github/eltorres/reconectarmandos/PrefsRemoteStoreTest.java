package io.github.eltorres.reconectarmandos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrefsRemoteStoreTest {
    private static final String RC902 = "00:00:00:70:A2:7D";
    private static final String RC833 = "00:00:00:73:01:EE";

    @Test
    public void markedRemotesSurviveEncodeAndDecode() {
        Map<String, String> marked = new LinkedHashMap<>();
        marked.put(RC833, "BT_RC833_U1");
        marked.put(RC902, "TCL_RC902V_M1");

        Map<String, String> decoded = PrefsRemoteStore.decode(PrefsRemoteStore.encode(marked));

        assertEquals(marked, decoded);
        assertEquals(List.of(RC833, RC902), List.copyOf(decoded.keySet()));
    }

    @Test
    public void markedRemoteStaysWhenNoLongerBonded() {
        Map<String, String> marked = PrefsRemoteStore.decode(
                PrefsRemoteStore.encode(Collections.singletonMap(RC902, "TCL_RC902V_M1")));

        List<Remote> listed = RemoteStatus.listed(List.of(), marked);

        assertEquals(List.of(new Remote(RC902, "TCL_RC902V_M1")), listed);
    }

    @Test
    public void namesWithSeparatorsDoNotBreakTheList() {
        Map<String, String> marked = new LinkedHashMap<>();
        marked.put(RC902, "Mando\tdel\nsalón");

        Map<String, String> decoded = PrefsRemoteStore.decode(PrefsRemoteStore.encode(marked));

        assertEquals("Mando del salón", decoded.get(RC902));
    }

    @Test
    public void attemptSurvivesEncodeAndDecode() {
        Attempt attempt = Attempt.decode(new Attempt(1_762_713_660_000L, false, "bond failed\nreason 9").encode());

        assertEquals(1_762_713_660_000L, attempt.atMs());
        assertFalse(attempt.connected());
        assertEquals("bond failed reason 9", attempt.detail());
    }

    @Test
    public void emptyStoreHasNoRemotes() {
        assertTrue(PrefsRemoteStore.decode("").isEmpty());
        assertTrue(PrefsRemoteStore.decode(null).isEmpty());
    }
}
