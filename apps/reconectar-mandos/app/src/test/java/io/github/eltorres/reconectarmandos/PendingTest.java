package io.github.eltorres.reconectarmandos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

public class PendingTest {
    private static final String RC902 = "00:00:00:70:A2:7D";
    private static final String RC833 = "00:00:00:73:01:EE";

    @Test
    public void bondedDisconnectedMarkedRemoteIsPending() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC833, "BT_RC833_U1");

        assertEquals(Set.of(RC833), Pending.of(new FakeRemoteStore().with(RC833).marked(), Set.of(), bluetooth));
    }

    @Test
    public void anyConnectedMeansNothingPending() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1").bond(RC833, "BT_RC833_U1")
                .connect(RC833);

        assertTrue(Pending.of(new FakeRemoteStore().with(RC902).with(RC833).marked(), Set.of(), bluetooth).isEmpty());
    }

    @Test
    public void otherMarkedRemoteConnectedMeansNothingPending() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1").bond(RC833, "BT_RC833_U1")
                .connect(RC902);

        assertTrue(Pending.of(new FakeRemoteStore().with(RC902).with(RC833).marked(), Set.of(), bluetooth).isEmpty());
    }

    @Test
    public void unbondedByUsStaysPendingForNextWait() {
        FakeBluetooth bluetooth = new FakeBluetooth().bond(RC833, "BT_RC833_U1");

        assertEquals(Set.of(RC902, RC833),
                Pending.of(new FakeRemoteStore().with(RC902).with(RC833).marked(), Set.of(RC902), bluetooth));
    }

    @Test
    public void remoteUnbondedByPersonIsNotPending() {
        assertTrue(Pending.of(new FakeRemoteStore().with(RC902).marked(), Set.of(), new FakeBluetooth()).isEmpty());
    }
}
