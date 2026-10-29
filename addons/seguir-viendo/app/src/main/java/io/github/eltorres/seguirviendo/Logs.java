package io.github.eltorres.seguirviendo;

import android.util.Log;

final class Logs {
    static final String TAG = "SeguirViendo";

    private Logs() {
    }

    static void w(SecretStore secrets, String message, Throwable error) {
        String detail = error == null ? "" : ": " + error;
        Log.w(TAG, secrets.scrub(message + detail));
    }
}
