---
name: "reconectar-mandos-bluetooth"
description: "App del fork que vuelve a vincular y conectar por Bluetooth, al pulsar una tecla, los mandos que la TCL C725 deja de reconocer tras cada apagado"
created_at: "2026-11-09T17:55:00Z"

created_by:
  tool: "Claude Code"
  model:
    name: "Claude Opus"
    version: "5.5"
    reasoning_effort: "low"

implemented_by:
  tool: "Claude Code"
  model:
    name: "Claude Opus"
    version: "5.5"
    reasoning_effort: "low"
last_implementation_at: "2026-11-09T18:19:27Z"
---

# Reconectar mandos Bluetooth

## Objetivo

Una app propia del fork, en `apps/reconectar-mandos/`, que detecta cuándo la persona pulsa una tecla de un mando marcado que la TV ya no conecta (BT_RC833_U1, TCL_RC902V_M1) y en ese momento lo desvincula y lo vuelve a vincular, para que funcione por Bluetooth sin la combinación de emparejamiento. El procedimiento ya se probó a mano en la TV el 2026-11-09 con la sonda `dev.probe.bt`; este cambio lo convierte en algo automático y documentado.

## Decisiones

- App propia con id `io.github.eltorres.reconectarmandos` en `apps/reconectar-mandos/` (elegido por el usuario), fuera de `addons/`: no es una tarjeta de módulo ni se publica en el original, así que no sigue sus reglas de add-ons (captura, fila en `addons/README.md`, sección Permissions) y la suite de Python de add-ons no la recorre. Tampoco va en el launcher, que no debe tener permisos de Bluetooth ni de ubicación.
- Proyecto Gradle copiado de `addons/seguir-viendo/` (AGP 8.5.2, `compileSdk 34`, `minSdk 26`, `targetSdk 34`, Java 17, firma de depuración, R8 en release), con su propio `.gitignore` para `build/` y `local.properties`.
- Permisos: `BLUETOOTH` y `BLUETOOTH_ADMIN` con `maxSdkVersion 30`, `BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT` para API 31 o superior, `ACCESS_FINE_LOCATION`, `RECEIVE_BOOT_COMPLETED` y `FOREGROUND_SERVICE`. Sin `INTERNET`.
- `removeBond()` y `createBond(TRANSPORT_LE)` se llaman por reflexión, como la sonda, porque son API ocultas en Android 11. Las dos funcionan en esta TV desde una app normal (comprobado).
- La espera usa un escaneo BLE con `PendingIntent` y un `ScanFilter` por dirección, que el sistema mantiene sin proceso propio, en lugar de un servicio en primer plano durante minutos tras el arranque. Así se cubre también una pulsación horas después de encender. Modo de escaneo equilibrado.
- La espera empieza con `BOOT_COMPLETED`, con `ACTION_ACL_DISCONNECTED` de un mando marcado (difusión implícita permitida en segundo plano) y al abrir la pantalla, y solo si ningún mando marcado está conectado.
- Se deja de esperar en cuanto cualquier mando marcado está conectado (elegido por el usuario), en lugar de esperar a que lo estén todos: con un mando por Bluetooth basta, y los demás siguen funcionando por infrarrojos. Una reconexión ya empezada termina aunque otro mando se conecte mientras tanto, para no dejar un mando desvinculado a medias.
- La reconexión corre en un servicio en primer plano corto (`ReconnectService`): desvincula, espera el `BOND_STATE_CHANGED` a `BOND_NONE` en lugar de un retardo fijo, vincula y termina al conectar el HID o al fallar. Una reconexión a la vez y como mucho un intento cada 5 s por mando, para no encadenar intentos con la ráfaga de anuncios de una pulsación.
- Un mando marcado está pendiente si está vinculado sin HID conectado, o si no está vinculado porque la app lo desvinculó y la vinculación falló; en ese caso, el siguiente anuncio solo vincula. Un mando que la persona desvincula a mano desde Ajustes no se vuelve a vincular: la app anota qué desvinculó ella.
- El estado conectado se lee del perfil HID (`BluetoothProfile.HID_HOST`, valor 4), no del vínculo.
- La lista de mandos marcados, la marca de "desvinculado por la app" y el último resultado por mando viven en unas `SharedPreferences` propias, con la codificación separada en una clase pura para probarla, como `PrefsHiddenStore` en `seguir-viendo`.
- La pantalla lista los dispositivos LE vinculados con una casilla y el estado de cada uno, y muestra los requisitos que faltan con su solución. El permiso de ubicación se pide desde la pantalla con el diálogo del sistema; `pm grant` queda documentado como alternativa.
- El bloqueo de TCL a los arranques en segundo plano (`APP_AUTO_START`) no se puede comprobar desde la app: se documenta con el comando `appops set ... APP_AUTO_START allow`, igual que en `seguir-viendo`.
- La documentación se escribe únicamente en `examples/tcl-55c725/spain-rc902v-m1.md` (instalación, uso y funcionamiento) y en `examples/tcl-55c725/README.md` (mención breve). El `README.md` de la app, `docs/TROUBLESHOOTING.md` y `FORK.md` solo enlazan a la nota.
- La primera fase incluye un indicador "último anuncio visto" con la pantalla abierta, que comprueba en la TV real que una app normal recibe los anuncios dirigidos del mando mientras sigue vinculado. Si no los recibe, se para y se revisa este cambio antes de la fase 2: la alternativa descartada por ahora es desvincular al arrancar y reintentar la vinculación cada 30 s durante unos minutos.
- Pruebas: solo JUnit en la JVM de la lógica de la app (almacenamiento, estado, pendientes, reconexión y espera) con un puerto de Bluetooth falso, sin pruebas de interfaz, como en `seguir-viendo`. El arranque, la desconexión y la pulsación real se comprueban en la TV.
- Comando de verificación de este cambio: en `apps/reconectar-mandos/`, `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`. La suite de Python del repositorio solo se ejecuta en la fase 3, para confirmar que los enlaces añadidos en `examples/` y `docs/` no la rompen.
- Textos en inglés (`values`) y español (`values-es`).
- Fuera del alcance: captura de pantalla y demás reglas de publicación de add-ons del original; tocar el launcher o enviar algo a upstream; mandos que no sean Bluetooth LE; reparar el firmware de la TV o de los mandos; root; configurar la voz o el micrófono más allá de lo que hace el sistema al conectar; borrar la sonda `dev.probe.bt` de la TV (se desinstala a mano al terminar).

## Fases

### Fase 1: Pantalla de mandos y detección de anuncios

La app se instala, lista los mandos LE vinculados, deja marcar cuáles reconectar, muestra si cada uno está conectado y qué requisito falta, y con la pantalla abierta indica cuándo vio el último anuncio de cada mando marcado. Todavía no desvincula nada.

#### Contratos

```java
// Dominio
enum RemoteState { CONNECTED, WAITING, RECONNECTING, NOT_BONDED }
enum Missing { LOCATION_PERMISSION, LOCATION_OFF, BLUETOOTH_OFF }
final class Remote { String address(); String name(); }
final class Attempt { long atMs(); boolean connected(); String detail(); }

// Puertos y repositorios
interface BluetoothPort {
    boolean enabled();
    List<Remote> bondedLe();
    boolean bonded(String address);
    boolean inputConnected(String address);
}
interface RemoteStore {
    Map<String, String> marked();                 // dirección -> nombre
    void mark(Remote remote);
    void unmark(String address);
    Attempt lastAttempt(String address);          // null si nunca
}
final class PrefsRemoteStore implements RemoteStore {
    static String encode(Map<String, String> marked);
    static Map<String, String> decode(String stored);
}

// Servicios de aplicación
final class RemoteStatus {
    static RemoteState of(String address, BluetoothPort bluetooth, boolean reconnecting);
    static List<Missing> missing(boolean locationPermission, boolean locationOn, boolean bluetoothOn);
}

// Textos de interfaz de usuario (values y values-es)
// app_name, remotes_title, remote_connected, remote_waiting, remote_reconnecting, remote_not_bonded,
// last_advert_seen, last_attempt_ok, last_attempt_failed, missing_location_permission, missing_location_off,
// missing_bluetooth_off, no_le_devices
```

#### Tests

- `PrefsRemoteStoreTest`
  - markedRemotesSurviveEncodeAndDecode (Marcar un mando)
  - markedRemoteStaysWhenNoLongerBonded (Mandos elegidos por la persona: recordar la lista aunque deje de estar vinculado)
- `RemoteStatusTest`
  - bondedWithInputIsConnected (Consultar el estado)
  - bondedWithoutInputIsWaiting (Consultar el estado)
  - reconnectingWinsOverWaiting (Consultar el estado)
  - locationOffIsReported (Ubicación desactivada)

- [x] Crea `apps/reconectar-mandos/` con el esqueleto Gradle de `addons/seguir-viendo/` (`settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `.gitignore`, `app/build.gradle` sin `buildConfigField` ni dependencias de red) y el manifiesto con los permisos de las Decisiones, `LEANBACK_LAUNCHER` y banner.
- [x] Implementa el dominio, `BluetoothPort` sobre `BluetoothAdapter` y el perfil HID (`getProfileProxy` con el valor 4, como `Probe.connectInput` de la sonda), `PrefsRemoteStore` y `RemoteStatus`, con sus pruebas.
- [x] Implementa `MainActivity`: lista de dispositivos LE vinculados con casilla y estado, bloque de requisitos que faltan con petición del permiso de ubicación, y un escaneo en primer plano mientras está abierta que muestra la hora del último anuncio de cada mando marcado. Toma el estilo de pantalla de `addons/seguir-viendo/app/src/main/java/io/github/eltorres/seguirviendo/MainActivity.java` y sus `res/`.
- [x] Añade a `examples/tcl-55c725/spain-rc902v-m1.md`, en la sección del mando Bluetooth, un apartado sobre la app con su compilación, la instalación con `tools/b64push.py` y `pm install`, `APP_AUTO_START`, el permiso de ubicación y el uso de la pantalla; y crea `apps/reconectar-mandos/README.md` con una línea que enlaza a ese apartado.
- [x] Instala la app en la TV, marca el RC902 y el RC833, y comprueba con un mando vinculado y desconectado que la pantalla muestra anuncios al pulsar sus teclas. Si no aparecen, para y revisa este cambio antes de la fase 2.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 2: Reconexión automática al pulsar el mando

Tras encender la TV, o cuando un mando marcado se desconecta y no queda otro conectado, la app espera los anuncios sin servicio en marcha y, en cuanto la persona pulsa una tecla, desvincula y vuelve a vincular ese mando. Deja de esperar con el primer mando marcado conectado. La pantalla muestra el resultado y la hora de la última reconexión.

#### Contratos

```java
// Puertos y repositorios
interface BluetoothPort {                          // se añaden a los de la fase 1
    boolean removeBond(String address);
    boolean createBondLe(String address);
}
interface AdvertWatch { void watch(Set<String> addresses); void stop(); }
interface RemoteStore {                            // se añaden a los de la fase 1
    Set<String> unbondedByUs();
    void setUnbondedByUs(String address, boolean value);
    void saveAttempt(String address, Attempt attempt);
}

// Servicios de aplicación
final class Pending { static Set<String> of(Map<String, String> marked, Set<String> unbondedByUs, BluetoothPort bluetooth); }
final class Watcher { void refresh(); }           // arranca o para AdvertWatch según Pending
final class Reconnector {
    enum Step { IGNORE, UNBOND, BOND }
    Step onAdvert(String address, long nowMs);
    Step onUnbonded(String address);
    void onFinished(String address, boolean connected, long nowMs);
}

// Componentes Android
public final class EventReceiver extends BroadcastReceiver { }   // BOOT_COMPLETED, ACL_DISCONNECTED
public final class AdvertReceiver extends BroadcastReceiver { }  // resultados del escaneo con PendingIntent
public final class ReconnectService extends Service { }          // primer plano solo durante una reconexión

// Textos de interfaz de usuario
// reconnect_notification_title, reconnect_notification_text
```

#### Tests

- `ReconnectorTest`
  - advertFromBondedDisconnectedRemoteUnbondsThenBonds (Primera pulsación tras encender)
  - unmarkedRemoteIsIgnored (Mando no marcado)
  - advertAfterFailedBondOnlyBonds (Vinculación fallida)
  - secondAdvertWithinFiveSecondsIsIgnored (Reintento sin dejar el mando suelto: una reconexión a la vez)
  - nothingHappensWhileRequirementMissing (Ubicación desactivada)
- `PendingTest`
  - bondedDisconnectedMarkedRemoteIsPending (Desconexión con la TV encendida)
  - anyConnectedMeansNothingPending (Uno conectado)
  - otherMarkedRemoteConnectedMeansNothingPending (Otro mando marcado sigue conectado)
  - unbondedByUsStaysPendingForNextWait (Otro mando conectado antes)
  - remoteUnbondedByPersonIsNotPending (Mando no marcado: no se vuelve a vincular lo que la persona desvinculó)
- `WatcherTest`
  - watchStartsWithPendingRemotes (Espera tras el arranque)
  - watchStopsWhenAnyMarkedConnected (Uno conectado)

- [ ] Amplía `BluetoothPort` con `removeBond` y `createBondLe` por reflexión (como `Probe.onCreate` de la sonda) y `RemoteStore` con la marca de desvinculado y el último intento.
- [ ] Implementa `Pending`, `Reconnector` y `Watcher` con sus pruebas, y `AdvertWatch` sobre `BluetoothLeScanner.startScan(filters, settings, PendingIntent)`.
- [ ] Implementa `EventReceiver`, `AdvertReceiver` y `ReconnectService` (notificación mínima mientras reconecta, `BOND_STATE_CHANGED` y estado HID para avanzar, final por conexión o por fallo con un límite de 35 s, y `Watcher.refresh()` al terminar), y conecta la pantalla al último intento. Toma el patrón de `BootReceiver` de `addons/seguir-viendo/`.
- [ ] Prueba en la TV: reinicia con los dos mandos marcados, pulsa el RC902 y comprueba que responde por Bluetooth con el LED de infrarrojos tapado y que la app deja de esperar aunque el RC833 siga sin conexión; repite con una pulsación pasados varios minutos, y con una desvinculación a mano desde Ajustes para comprobar que no se vuelve a vincular.
- [ ] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 3: Documentación en la carpeta de la TV

La nota de la TV explica la app como solución, y el resto de documentos solo enlazan a ella.

#### Contratos

Sin cambios de contratos.

#### Tests

- Sin pruebas: el cambio es solo documentación.

- [ ] Completa el apartado de la app en `examples/tcl-55c725/spain-rc902v-m1.md` con el funcionamiento automático (cuándo espera, que para con el primer mando conectado, qué pasa si se desvincula a mano) y cambia su conclusión "Desde la TV no tiene arreglo" para enlazar ese apartado.
- [ ] Añade a `examples/tcl-55c725/README.md`, en la línea sobre los mandos Bluetooth de *Firmware quirks*, la mención de la app con enlace al apartado de la nota.
- [ ] Sustituye en `docs/TROUBLESHOOTING.md` la frase "Nothing on the TV fixes it" por un enlace al apartado de la nota, sin repetir su contenido, y añade a la tabla de diferencias de `FORK.md` una fila de la app que enlaza a la nota.
- [ ] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

## Siguiente paso

Fase 1 completada. En su prueba en la TV, con la app abierta y escaneando en baja latencia, el RC833 vinculado y sin conexión se reconectó solo al pulsar una tecla, mediante la conexión en segundo plano de la propia TV y sin desvincularlo; en las pruebas anteriores, sin escaneo activo, nunca lo hizo. Antes de la fase 2 conviene repetir esa prueba y, si se confirma, revisar el diseño con `/openspec-update-change` (mantener un escaneo mientras haya mandos marcados sin conexión, con desvincular y vincular como respaldo). Después, ejecuta `/openspec-apply-change` para implementar la Fase 2.
