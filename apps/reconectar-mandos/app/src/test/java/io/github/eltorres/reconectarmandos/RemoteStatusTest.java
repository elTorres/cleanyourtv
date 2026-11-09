package io.github.eltorres.reconectarmandos;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class RemoteStatusTest {
    private static final String RC902 = "00:00:00:70:A2:7D";
    private static final String RC833 = "00:00:00:73:01:EE";

    @Test
    public void bondedWithInputIsConnected() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1").connect(RC902);

        assertEquals(RemoteState.CONNECTED, RemoteStatus.of(RC902, bluetooth, false));
    }

    @Test
    public void bondedWithoutInputIsWaiting() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1");

        assertEquals(RemoteState.WAITING, RemoteStatus.of(RC902, bluetooth, false));
    }

    @Test
    public void reconnectingWinsOverWaiting() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1");

        assertEquals(RemoteState.RECONNECTING, RemoteStatus.of(RC902, bluetooth, true));
    }

    @Test
    public void unbondedRemoteIsNotBonded() {
        assertEquals(RemoteState.NOT_BONDED, RemoteStatus.of(RC902, new FakeBluetooth(), false));
    }

    @Test
    public void locationOffIsReported() {
        assertEquals(List.of(Missing.LOCATION_OFF), RemoteStatus.missing(true, false, true));
    }

    @Test
    public void everyMissingRequirementIsReported() {
        assertEquals(List.of(Missing.BLUETOOTH_OFF, Missing.LOCATION_PERMISSION, Missing.LOCATION_OFF),
                RemoteStatus.missing(false, false, false));
    }

    @Test
    public void markedRemotesComeFirstAndBondedOnesAreNotRepeated() {
        List<Remote> bonded = List.of(new Remote(RC833, "BT_RC833_U1"), new Remote(RC902, "TCL_RC902V_M1"));

        List<Remote> listed = RemoteStatus.listed(bonded, Collections.singletonMap(RC902, "TCL_RC902V_M1"));

        assertEquals(List.of(new Remote(RC902, "TCL_RC902V_M1"), new Remote(RC833, "BT_RC833_U1")), listed);
    }
}
