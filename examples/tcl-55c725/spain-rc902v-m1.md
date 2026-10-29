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
    adb shell "logcat -d | grep -iE 'HidHost|bt_hh|smp|encrypt|A2:7D|disconnect' | tail -40"
    ```

**Resultado en esta unidad (2026-10-29):**

- El mando sigue en `Bonded devices` tras el apagado (`00:00:00:70:A2:7D [ LE ] TCL_RC902V_M1`), así que el
  emparejamiento no se pierde.
- `com.tcl.tcl_bt_rcu_service` y `com.tcl.autopair` están activos y arrancan al encender
  (`STR_RESUME_COMPLETED`, `START_AUTOPAIR`). Ningún paquete de la lista de debloat interviene.
- Al encender, el Bluetooth se reinicia (`MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED`), vuelve a cargar el mando
  emparejado y lanza la reconexión (`GATT_Connect ... address=00:00:00:70:a2:7d`, `attempting auto connection`).
  El mando nunca responde: después solo aparecen consultas de `getConnectionState`.
- Emparejarlo desde cero (*Desvincular*, quitar las pilas 30 segundos y emparejar otra vez) no lo arregla.

**Conclusión.** Lo más probable es que sea un defecto de este mando, que no es original. Una dirección que empieza
por `00:00:00` no es una dirección de fabricante válida: el mando entrega una identidad falsa al emparejarse y
luego se anuncia con otra, así que la TV no lo encuentra al reconectar. Desde la TV no tiene arreglo.

Alternativas:

- Usarlo por infrarrojos: los botones remapeados llegan con el mismo keycode. El micrófono no funciona por
  infrarrojos.
- Volver a emparejarlo cuando haga falta la voz.
- Cambiarlo por un TCL RC902V original.

Actualizar el firmware del mando tampoco es una opción. TCL solo actualiza sus mandos originales, a través de
`com.tcl.tcl_bt_rcu_service`, y forzar un firmware de TCL en una réplica podría inutilizarla.
