package io.github.eltorres.reconectarmandos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.eltorres.reconectarmandos.Reconnector.Step;

public class ReconnectorTest {
    private static final String RC902 = "00:00:00:70:A2:7D";
    private static final String RC833 = "00:00:00:73:01:EE";
    private static final String RC802 = "41:42:EC:03:F2:6B";
    private static final long T = 1_000_000;

    private final FakeBluetooth bluetooth = new FakeBluetooth().bond(RC902, "TCL_RC902V_M1").bond(RC833, "BT_RC833_U1");
    private final FakeRemoteStore store = new FakeRemoteStore().with(RC902).with(RC833);
    private boolean ready = true;
    private final Reconnector reconnector = new Reconnector(store, bluetooth, () -> ready);

    @Test
    public void firstAdvertOfBurstWaitsForTheTv() {
        assertEquals(Step.IGNORE, reconnector.onAdvert(RC833, T));
        assertEquals(Step.IGNORE, reconnector.onAdvert(RC833, T + 3_000));
        bluetooth.connect(RC833);

        assertEquals(Step.IGNORE, reconnector.onAdvert(RC833, T + 12_000));
        assertTrue(bluetooth.bonded(RC833));
        assertFalse(store.unbondedByUs().contains(RC833));
    }

    @Test
    public void advertTenSecondsIntoBurstUnbonds() {
        reconnector.onAdvert(RC902, T);
        reconnector.onAdvert(RC902, T + 5_000);

        assertEquals(Step.UNBOND, reconnector.onAdvert(RC902, T + 10_000));
        assertTrue(reconnector.reconnecting(RC902));
        assertTrue(store.unbondedByUs().contains(RC902));
    }

    @Test
    public void unbondedStepBonds() {
        startUnbond(RC902);
        bluetooth.removeBond(RC902);

        assertEquals(Step.BOND, reconnector.onUnbonded(RC902));
        assertEquals(Step.IGNORE, reconnector.onUnbonded(RC833));

        bluetooth.createBondLe(RC902);
        bluetooth.connect(RC902);
        reconnector.onFinished(RC902, true, "", T + 20_000);
        assertFalse(reconnector.reconnecting(RC902));
        assertFalse(store.unbondedByUs().contains(RC902));
        assertTrue(store.lastAttempt(RC902).connected());
    }

    @Test
    public void advertAfterLongGapStartsNewBurst() {
        reconnector.onAdvert(RC902, T);
        reconnector.onAdvert(RC902, T + 5_000);

        assertEquals(Step.IGNORE, reconnector.onAdvert(RC902, T + 5_000 + Reconnector.BURST_GAP_MS + 1));
        assertEquals(Long.valueOf(T + 5_000 + Reconnector.BURST_GAP_MS + 1), store.burstStart(RC902));
    }

    @Test
    public void unmarkedRemoteIsIgnored() {
        bluetooth.bond(RC802, "TCL_RC802V_MA");

        for (long at = T; at <= T + 20_000; at += 1_000) assertEquals(Step.IGNORE, reconnector.onAdvert(RC802, at));
        assertTrue(bluetooth.bonded(RC802));
    }

    @Test
    public void advertAfterFailedBondOnlyBonds() {
        startUnbond(RC902);
        bluetooth.removeBond(RC902);
        reconnector.onUnbonded(RC902);
        reconnector.onFinished(RC902, false, "bond failed", T + 15_000);

        assertEquals(Step.BOND, reconnector.onAdvert(RC902, T + 60_000));
        assertFalse(store.lastAttempt(RC902).connected());
    }

    @Test
    public void secondAttemptWithinFiveSecondsIsIgnored() {
        startUnbond(RC902);
        assertEquals(Step.IGNORE, reconnector.onAdvert(RC833, T + 10_500));
        bluetooth.removeBond(RC902);
        reconnector.onFinished(RC902, false, "unbond refused", T + 11_000);

        assertEquals(Step.IGNORE, reconnector.onAdvert(RC902, T + 12_000));
        assertEquals(Step.BOND, reconnector.onAdvert(RC902, T + 16_000));
    }

    @Test
    public void nothingHappensWhileRequirementMissing() {
        ready = false;

        for (long at = T; at <= T + 20_000; at += 1_000) assertEquals(Step.IGNORE, reconnector.onAdvert(RC902, at));
        assertTrue(bluetooth.bonded(RC902));
    }

    private void startUnbond(String address) {
        reconnector.onAdvert(address, T);
        assertEquals(Step.UNBOND, reconnector.onAdvert(address, T + 10_000));
    }
}
