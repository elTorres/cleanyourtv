# TCL C725 (España) con el mando TCL_RC902V_M1

Notas de una TCL C725 configurada con el launcher de CleanYourTV como Home, recogidas el 2026-10-29. Complementan
el `README.md` de esta carpeta, que describe la misma plataforma.

## Equipo

| | |
| --- | --- |
| TV | TCL C725, plataforma R51MT05 (BeyondTV4, G03) |
| Sistema | Android TV 11, build `RP1A.200622.001` |
| Mando | Bluetooth, se registra como `TCL_RC902V_M1` (no es el que venía con la TV) |
| Home | `launcher/` de CleanYourTV con `HOME_KEY_SERVICE=1` |
| Debloat | `debloat.txt` de esta carpeta, salvo `com.tcl.keyhelp`, que se volvió a activar (ver más abajo) |

## Botones del mando

Keycodes leídos con `adb shell "logcat -c; logcat | grep handleKeyBeforeQueueing"`. TCL los registra como
`TclPhoneWindowManager: handleKeyBeforeQueueing, key = NNNN`.

| Keycode | Botón | Función asignada | Cómo |
| --- | --- | --- | --- |
| 4062 | Netflix | Movistar Plus+ | `app_keys` |
| 4099 | Prime Video | Stremio | `app_keys` |
| 4140 | Guard | NordVPN | `app_keys` |
| 4057 | Media | Cambiar a HDMI 2 | `app_keys`, destino `input:` |
| 4065 | YouTube | SmartTube | `app_keys` |
| 4020 | TCL Channel | Guía EPG (vista estándar) | `app_keys`, destino `guide:` |
| 4077 | Ajustes (engranaje) | Ajustes | `settings_key_keycodes` |
| 4001 | LiveTV | Pulsar: TDT. Mantener: guía EPG de TCL | `app_keys` + `hold:` |
| 4056 | Teclado numérico | Mando virtual de TCL | Lo gestiona el firmware con `com.tcl.keyhelp` activado |
| 4137 | (sin asignar) | Se quería enviar el botón rojo (183) | No es posible solo con el launcher (ver Limitaciones) |

El botón LiveTV genera dos eventos: `4001` desde el mando (`from device 6`) y, unos 100 ms después de soltarlo,
`172` (`KEYCODE_GUIDE`) desde `device -1`. Ese `172` lo inyecta el propio firmware, iba dirigido al launcher de
Google y ahora no hace nada. Hay que remapear el `4001`.

## Paquetes e entradas de esta TV

| Qué | Id |
| --- | --- |
| Movistar Plus+ | `com.movistarplus.androidtv` |
| Stremio | `com.stremio.one` |
| NordVPN | `com.nordvpn.android` |
| SmartTube | `org.smarttube.stable` |
| Live TV de TCL | `com.tcl.tv` |
| Sintonizador TDT | `com.tcl.tvinput/.TDTVInputService/HW1413742848` |
| AV | `com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413743104` |
| HDMI 1 | `com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128` |
| HDMI 2 | `com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744384` |
| HDMI 3 | `com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744640` |
| HDMI 4 | hardware `1413745664` (`hdmi_port=4`), sin entrada publicada en esta unidad |

Ids sacados de `adb shell dumpsys tv_input | grep -E "HW[0-9]+|hdmi_port"`.

Pantallas de EPG de `com.tcl.tv`, probadas con la TDT en pantalla:

| Componente o intent | Resultado |
| --- | --- |
| `com.tcl.tv/com.tcl.dtv.manage.newguide.activity.EpgActivity` | Guía de TCL |
| `com.tcl.tv/com.tcl.dtv.manage.guide.EpgMainActivity` | La misma guía |
| `com.tcl.tv/com.tcl.epg.EpgProActivity` | Error, no se abre |
| `VIEW` `vnd.android.cursor.dir/program` en `com.tcl.tv` (lo que envía `guide:`) | Otra guía, distinta |

## `config.json` del tema

```json
{
  "dock_apps": [
    "com.tcl.tv",
    "com.movistarplus.androidtv",
    "com.stremio.one",
    "com.fctv77.tv",
    "com.dazn",
    "com.bone.android.a4v.oficial",
    "org.smarttube.stable"
  ],
  "columns": 6,
  "rows_below_dock": 0,
  "background_color": "#070A14",
  "wallpaper": true,
  "live_tv_package": "com.tcl.tv",
  "live_tv_input_id": "com.tcl.tvinput/.TDTVInputService/HW1413742848",
  "settings_key_keycodes": [4077],
  "consume_volume_keys": true,
  "app_keys": [
    "4062:com.movistarplus.androidtv",
    "4099:com.stremio.one",
    "4140:com.nordvpn.android",
    "4057:input:com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744384",
    "4065:org.smarttube.stable",
    "4020:guide:com.tcl.tv",
    "4001:input:com.tcl.tvinput/.TDTVInputService/HW1413742848",
    "hold:4001:com.tcl.tv/com.tcl.dtv.manage.newguide.activity.EpgActivity",
    "4137"
  ]
}
```

Se aplica con `TV_HOST=<ip> tools/theme.sh <carpeta-del-tema>`.

## Lo que se aprendió por el camino

- **Volumen de dos en dos.** Con `HomeKeyService` vinculado, un toque de volumen subía o bajaba dos pasos.
  `"consume_volume_keys": true` lo arregla: el servicio se queda con la tecla y repite él la pulsación mantenida.
- **`"4001:com.tcl.tv"` no lleva a la TDT.** La app Live TV de TCL vuelve a la última fuente usada. Para ir
  siempre a la TDT, el destino tiene que ser la entrada del sintonizador (`input:...TDTVInputService...`). Se
  comprobó antes con:
  `adb shell am start -a android.intent.action.VIEW -t vnd.android.cursor.item/channel -d "content://android.media.tv/passthrough/com.tcl.tvinput%2F.TDTVInputService%2FHW1413742848"`.
- **`front:` no sirve con destinos `input:`.** `HomeKeyService.inFront` solo aplica `front:` si el destino normal es
  una app o una actividad. Además, la TDT y los HDMI se ven dentro de `com.tcl.tv`, así que el launcher no podría
  distinguirlos. Por eso la EPG va en la pulsación larga (`hold:`).
- **El mando virtual es `com.tcl.keyhelp/.virtualkey.KeysService`.** `com.tcl.keyhelp` ya venía desactivado (la
  lista lo incluye). Con `adb shell pm enable com.tcl.keyhelp`, el firmware vuelve a abrir el mando virtual con el
  botón 4056 sin tocar `app_keys`. Es un servicio, no una actividad, así que el launcher no podría lanzarlo.
- **tvQuickActions se desinstaló.** Mientras estuvo instalado, sus remapeos dejaban de funcionar con el tiempo. El
  motivo probable es que TclAppBoot no deja re-vincular el servicio de accesibilidad de una app sin `AUTO_START`
  (`cmd appops set <paquete> AUTO_START allow; cmd appops set <paquete> APP_AUTO_START allow; appops write-settings`),
  y Safety Guard (`com.tcl.guard`), desde donde se concede ese permiso, está desactivado por la lista. Además, dos
  servicios filtrando las mismas teclas se pisan.

## Limitaciones

- **Enviar un keycode (botón rojo 183 desde el 4137) no es posible con el launcher.** `app_keys` solo abre apps,
  actividades o entradas, o hace sus acciones fijas (`back`, `home`, `recents`, `mute`, `sleep`, `screensaver`,
  `settings`, `wifi`, `ignore`). Inyectar teclas requiere `INJECT_EVENTS`, un permiso de nivel *signature* que
  `pm grant` no puede conceder y que solo tienen el firmware y el shell de adb. Las alternativas son tvQuickActions
  (que se conecta por adb a la propia TV) para ese único botón, o root.

## Comprobaciones útiles

```
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys accessibility | grep -i "Bound services"
adb shell appops get com.gathin.tvhome | grep -i AUTO_START
```

Si `HomeKeyService` aparece en la lista pero no en `Bound services` (por ejemplo, tras reiniciar), hay que vaciar la
lista y volver a escribirla:

```
adb shell 'S=$(settings get secure enabled_accessibility_services); settings put secure enabled_accessibility_services ""; settings put secure accessibility_enabled 0; sleep 1; settings put secure enabled_accessibility_services "$S"; settings put secure accessibility_enabled 1'
```

## Mando Bluetooth: no se reconecta tras apagar la TV

Con este `TCL_RC902V_M1` el mando sigue funcionando por infrarrojos tras apagar y encender la TV, pero en
*Mandos y accesorios* aparece como desconectado. Si se vuelve a emparejar funciona, hasta el siguiente apagado. Ya
pasaba antes de aplicar CleanYourTV y no pasaba con el mando original.

**Diagnóstico.** Ejecuta estos comandos en este orden:

1. Con el mando conectado, antes de apagar, comprueba que está emparejado:

    ```
    adb shell "dumpsys bluetooth_manager | grep -iA6 'Bonded devices'"
    ```

2. Apaga y enciende la TV, pulsa unos botones del mando, espera unos 10 segundos y comprueba si el emparejamiento
   se ha conservado:

    ```
    adb shell "dumpsys bluetooth_manager | grep -iA6 'Bonded devices'"
    ```

3. Comprueba que los servicios de mandos de TCL están activos. Este comando no debe devolver nada:

    ```
    adb shell "pm list packages -d | grep -iE 'rcu|autopair|bluetooth'"
    ```

4. Mira qué hicieron los servicios de TCL al encender:

    ```
    adb shell "logcat -d | grep -iE 'rcu|autopair|BondState|RC902' | tail -40"
    ```

5. Mira el intento de reconexión del Bluetooth:

    ```
    adb shell "logcat -d | grep -iE 'HidHost|bt_hh|smp|encrypt|TCL_RC902V|disconnect' | tail -40"
    ```

**Resultado en esta unidad (2026-10-29):**

- El mando sigue en `Bonded devices` tras el apagado (`00:00:00:xx:xx:xx [ LE ] TCL_RC902V_M1`), así que el
  emparejamiento no se pierde.
- `com.tcl.tcl_bt_rcu_service` y `com.tcl.autopair` están activos y arrancan al encender
  (`STR_RESUME_COMPLETED`, `START_AUTOPAIR`). Ningún paquete de la lista de debloat interviene.
- Al encender, el Bluetooth se reinicia (`MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED`), vuelve a cargar el mando
  emparejado y lanza la reconexión (`GATT_Connect ... address=00:00:00:xx:xx:xx`, `attempting auto connection`).
  El mando nunca responde: después solo aparecen consultas de `getConnectionState`.
- Emparejarlo desde cero (*Desvincular*, quitar las pilas 30 segundos y emparejar otra vez) no lo arregla.

**Traza HCI (2026-11-09).** Se sacó del bloque `BTSNOOP_LOG_SUMMARY` de `dumpsys bluetooth_manager`, sin root;
el método está en
[TROUBLESHOOTING](../../docs/TROUBLESHOOTING.md#a-bluetooth-remote-works-only-over-ir-after-every-power-off-tcl-android-11).
Corrige la hipótesis anterior: el mando no cambia de dirección.

- Al emparejarse, el mando rechaza Secure Connections y entrega como identidad su dirección *pública*
  `00:00:00:xx:xx:xx` y la IRK `000102030405060708090a0b0c0d0e0f`, la clave de ejemplo de los kits de
  desarrollo.
- Al encender, la TV lo añade a la lista de resolución con esa IRK y a la lista de aceptación, y espera la
  reconexión.
- Al pulsar botones, el mando envía anuncios dirigidos a la TV (`ADV_DIRECT_IND`) desde esa misma dirección
  pública: 710 en unos 10 segundos. La controladora nunca llega a conectar.

**Conclusión.** Con una IRK distinta de cero, la controladora aplica el modo de privacidad de red y descarta los
paquetes que llegan desde la dirección de identidad. La TV tendría que pasar ese mando al modo de privacidad de
dispositivo (`LE Set Privacy Mode`), y este Android 11 no lo hace nunca. Al emparejar de nuevo funciona porque esa
conexión se abre antes de cargar la IRK. Con el `BT_RC833_U1` pasa lo mismo (misma dirección `00:00:00` y mismo
síntoma); el `TCL_RC802V_MA` sí reconecta. Ningún ajuste de la TV lo arregla; lo que funciona es la app
[Reconectar mandos](#app-reconectar-mandos), que lo vuelve a conectar al pulsar una tecla.

Sin la app:

- Usarlo por infrarrojos: los botones remapeados llegan con el mismo keycode. Desvincúlalo para que no aparezca
  como desconectado. El micrófono no funciona por infrarrojos.
- Volver a emparejarlo cuando haga falta la voz.
- Cambiarlo por un mando que reconecte en esta TV, como el `TCL_RC802V_MA`.

Actualizar el firmware del mando tampoco es una opción. TCL solo actualiza sus mandos originales, a través de
`com.tcl.tcl_bt_rcu_service`, y forzar un firmware de TCL en una réplica podría inutilizarla.

### App Reconectar mandos

El mando no deja de intentarlo: tras el apagado sigue enviando anuncios dirigidos a la TV cada vez que se pulsa una
tecla. Si en ese momento se desvincula y se vuelve a vincular por su dirección, el mando acepta el emparejamiento sin
la combinación de teclas y la TV lo conecta en unos segundos. Se comprobó a mano con los dos mandos el 2026-11-09.
La app de este fork, en [`apps/reconectar-mandos/`](../../apps/reconectar-mandos/), lo automatiza.

**Cómo funciona.**

- **Cuándo escucha:** al encender la TV, cuando se desconecta un mando marcado y al abrir la app, siempre que no haya
  ya un mando marcado conectado. Escucha con un escaneo Bluetooth que mantiene el propio sistema, sin servicio ni
  notificación. TCL retrasa unos 40 s el aviso de arranque a las apps, así que la escucha empieza poco después de ver
  la pantalla de inicio.
- **Si la TV lo conecta sola:** con el escaneo en marcha, la controladora a veces capta el anuncio y la TV conecta el
  mando por su cuenta, sin desvincularlo. Pasó al quitar y poner las pilas y al abrir la app, pero no en los
  arranques.
- **Respaldo:** si el mando sigue anunciándose 10 s sin conectarse, la app lo desvincula y lo vuelve a vincular.
  Durante unos segundos muestra una notificación de "Reconectando un mando". Si la vinculación falla, la repite con
  la siguiente pulsación. En tres reinicios seguidos, el RC833 quedó conectado de 15 a 30 s después de empezar a
  escuchar, con la app cerrada.
- **Cuándo deja de escuchar:** en cuanto cualquiera de los mandos marcados está conectado, o a los 10 minutos.
  Si pasado ese tiempo no se ha conectado, lo normal es que el mando no tenga pilas: pónselas y abre la app, que
  vuelve a escuchar otros 10 minutos, o vuelve a emparejarlo con su combinación de teclas.
- **Si lo desvinculas tú desde Ajustes:** la app no lo vuelve a vincular. Solo vuelve a vincular los mandos que ha
  desvinculado ella.

La pantalla muestra, para cada mando marcado, si está conectado, la hora de su último anuncio y el resultado de la
última reconexión.

**Compilar e instalar.** Hace falta el JDK 17 o posterior y el SDK de Android (`sdk.dir` en
`apps/reconectar-mandos/local.properties`, que git ignora). Desde la raíz del repositorio:

```bash
(cd apps/reconectar-mandos && ./gradlew --no-daemon assembleRelease)
TV_HOST=<ip> tools/venv/bin/python -I tools/b64push.py \
  apps/reconectar-mandos/app/build/outputs/apk/release/app-release.apk /data/local/tmp/rm.apk
TV_HOST=<ip> tools/venv/bin/python -I tools/tv.py 'pm install -r /data/local/tmp/rm.apk; rm -f /data/local/tmp/rm.apk'
```

**Permisos que hay que dar una vez.**

- Ubicación, también en segundo plano: Android 11 solo entrega los anuncios Bluetooth a una app con permiso de
  ubicación, y además la ubicación de la TV tiene que estar activada. Si el permiso es solo "mientras se usa la app",
  el escaneo que empieza al encender la TV no recibe nada y el mando solo se reconecta con la app abierta. La app
  pide los dos permisos con el botón *Permitir ubicación*. El de segundo plano se da más fácil por ADB:

  ```bash
  TV_HOST=<ip> tools/venv/bin/python -I tools/tv.py 'pm grant io.github.eltorres.reconectarmandos android.permission.ACCESS_COARSE_LOCATION; pm grant io.github.eltorres.reconectarmandos android.permission.ACCESS_FINE_LOCATION; pm grant io.github.eltorres.reconectarmandos android.permission.ACCESS_BACKGROUND_LOCATION'
  ```

- Arranque en segundo plano: el firmware de TCL no deja arrancar apps al encender sin `APP_AUTO_START`. La app no
  puede comprobarlo, así que conviene darlo al instalar:

  ```bash
  TV_HOST=<ip> tools/venv/bin/python -I tools/tv.py 'appops set io.github.eltorres.reconectarmandos APP_AUTO_START allow; appops write-settings'
  ```

**Uso.** Abre *Reconectar mandos* desde la lista de apps y pulsa OK sobre cada mando que deba reconectarse: queda
marcado con ✓. A partir de ahí no hace falta volver a abrirla: tras encender la TV, pulsa teclas del mando unos
segundos hasta que responda por Bluetooth. La app nunca toca un mando sin marcar. No pide acceso a Internet.
