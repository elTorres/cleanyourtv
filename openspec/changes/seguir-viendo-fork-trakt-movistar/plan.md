---
name: "seguir-viendo-fork-trakt-movistar"
description: "Fork personal del launcher con una fila Seguir viendo leída de Watch Next, alimentada por un add-on con fuentes Trakt y Movistar Plus"
created_at: "2026-10-29T00:00:00Z"

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
last_implementation_at: "2026-10-29T21:30:00Z"
---

# Seguir viendo en Home con Trakt y Movistar Plus

## Objetivo

Ver en Home lo que la persona tiene a medias en Stremio, en Kodi de otro dispositivo (los dos a través de Trakt) y en Movistar Plus, ninguno de los cuales publica en Watch Next. Para ello: un fork propio del launcher que lea Watch Next, partiendo del trabajo de gwiersma, y un add-on con red que publica en Watch Next. El fork conserva los cambios personales que nunca se subirán a Gathin23.

## Decisiones

- Fork de GitHub de `Gathin23/cleanyourtv` en la cuenta del usuario. Remotos: `origin` apunta al fork, `upstream` a Gathin23 y `gwiersma` se usa solo para leer y hacer cherry-pick (en lugar de clonar el fork de gwiersma, que arrastraría su configuración de TV).
- Los cambios personales se pasan al fork así: los commits `dd416ccb` (TCL C725 y tema aurora) y `d5933b95` (dock y `app_keys` de aurora) se rebasan sobre `upstream/main` (ahora `23fd3f23` y `e87c4925`). Los archivos de herramientas de agentes (`.agents/`, `.ai/`, `.claude/`, `.github/copilot-instructions.md`, `.github/hooks/rtk-rewrite.json`, `openspec/config.yaml` y `openspec/schemas/`) se quedan solo en local, ignorados por git; en el fork se suben `CLAUDE.md` y los cambios de `openspec/changes/` (en lugar de un commit con todas las herramientas, que habría publicado configuración personal). Los que llegaron a subirse se quitaron reescribiendo el historial con `--force-with-lease`.
- El fork es público, como todo fork de un repo público. Antes del primer push se revisan los archivos personales en busca de IP, nombres de Wi-Fi, números de serie o `greeting_name`, como pide `AGENTS.md`.
- De gwiersma se integra solo el commit de la funcionalidad (`d723b1f`): el lector de Watch Next, la fila, el interruptor `continue_watching` y el `pm grant` de `READ_TV_LISTINGS` en `launcher/build-and-install.sh`. No se integra su commit de configuración de TV.
- El enganche en `MainActivity.java` se reduce al mínimo y el resto vive en clases propias. `MainActivity` tuvo 311 commits upstream en un mes, así que cada línea que se toca en ese archivo es un conflicto futuro.
- Se mantiene la regla de gwiersma de abrir el `intent_uri` solo si pertenece a quien publicó la fila. El add-on publica intents hacia una actividad trampolín propia, que abre Stremio o Movistar+ (en lugar de relajar la regla en el launcher).
- Un solo add-on, `addons/seguir-viendo/`, con dos fuentes conmutables (en lugar de dos add-ons): una configuración, una sincronización y un trampolín. Las fuentes se pueden separar más adelante porque comparten un mismo puerto `ViewingSource`.
- El add-on vive en `addons/` con su fila en `addons/README.md` para que pasen las pruebas del repo (`tests/test_addons.py` y siguientes). Se acepta el pequeño conflicto de esa tabla en los rebases.
- El add-on se escribe en Java con Gradle Groovy, `minSdk 26` y `targetSdk 34`, como `addons/game-day`, y sin dependencias de terceros: `HttpURLConnection`, `org.json` y Android Keystore (en lugar de `androidx.security`, que está obsoleto).
- `continue_watching` sigue siendo una opción de compilación en `config.xml`, desactivada, y se enciende en la TV con un interruptor en los ajustes del launcher (`LauncherSettingsActivity`), guardado con las demás elecciones de la TV y que no se borra al cambiar de tema (en lugar de una clave de tema, que obligaría a tocar el constructor de `Config`, o de un interruptor en el add-on, que haría depender el launcher de él). La fila se monta siempre que haya dock y se oculta cuando está apagada.
- En la TCL C725 el navegador del firmware (`com.tcl.browser`, 30 filas) y DAZN (60) publican publicidad en Watch Next con fechas recientes que desplazan las filas del add-on. La fila admite una lista `continue_watching_apps` en `config.xml` (vacía: todas las apps), y en este fork solo lleva el add-on (en lugar de una lista de bloqueo, que habría que ampliar con cada app nueva, o de desactivar el navegador, que no resuelve lo de DAZN). Es una opción de compilación, como `continue_watching`.
- Trakt ya no emite secreto para las apps nuevas y declara `client_secret` opcional en todos sus `/oauth`, con PKCE también en el flujo de dispositivo. El add-on genera siempre un par PKCE S256 (`code_challenge` al pedir el código, `code_verifier` al canjearlo) y envía `client_secret` solo si está configurado (en lugar de exigirlo).
- Las credenciales de la app de Trakt (`trakt.clientId`, `trakt.clientSecret` opcional y `trakt.redirectUri`, que debe coincidir con la registrada en Trakt; las apps de tipo Development solo admiten `http://127.0.0.1` o `http://[::1]`) y la clave opcional de TMDB (`tmdb.apiKey`) van en `addons/seguir-viendo/local.properties`, que ya está ignorado por git, y se leen en `BuildConfig`. Sin clave de Trakt, la fuente aparece como no disponible.
- Trakt no da imágenes, así que los pósters se obtienen de TMDB con los ids que devuelve Trakt. Sin `tmdb.apiKey` la fila no lleva póster y el launcher pinta el banner.
- Los pósters se guardan en la caché del add-on y se sirven con un provider exportado de solo lectura que responde únicamente a ids opacos de imagen. No expone títulos ni tokens.
- La sincronización usa `JobScheduler`, con un trabajo periódico de 1 h con red requerida, `BOOT_COMPLETED` y el botón "Sincronizar ahora". No hay servicio en primer plano. El README del add-on lo declara.
- Los elementos de Trakt (que incluyen la actividad de Kodi en otro dispositivo) se abren en Stremio por defecto, o en Kodi (`org.xbmc.kodi`, instalado en la TV) si se elige en "Abrir en". Stremio 1.12.0 en la TV declara `stremio:///detail/{tipo}/{id}` y `stremio:///detail/{tipo}/{id}/{vídeo}`, y abre la ficha con sus fuentes: `stremio:///detail/series/tt…/tt…:T:E` por episodio y `stremio:///detail/movie/tt…` por película, con el id de IMDb que da Trakt. Sin id de IMDb, o con Kodi, se abre la app.
- Las tarjetas de la fila son 16:9 y se recortan al centro, así que de TMDB se toma la imagen de fondo apaisada (`backdrop_path`, a 300 px de ancho) y el póster vertical solo si no hay fondo. La clave admite la API key (va en la URL) o el token de lectura (va en la cabecera `Authorization`).
- Los siguientes episodios salen de las series vistas en los últimos 60 días (como mucho 10, las más recientes) que no tengan ya un elemento a medias, para no pedir a Trakt el progreso de las que se descartarían.
- El id de cada imagen es un SHA-256 truncado de su referencia (`tmdb:tv:<id>`), así que se reutiliza entre sincronizaciones sin volver a descargarla. Los pósters huérfanos se borran solo cuando todas las fuentes conectadas se han actualizado, para no borrar los de una fuente que ha fallado y conserva sus filas.
- Movistar Plus usa la API no oficial descrita en `Paco8/plugin.video.movistarplus` (GPL-2.0). Ese plugin sirve solo como referencia del protocolo: no se copia ni se traduce su código, para que el add-on pueda seguir con licencia MIT.
- Antes de implementar Movistar Plus se hace un spike para responder tres preguntas: si leer `viewings` exige registrar un dispositivo, si hay `refresh_token` y qué deep link acepta la app oficial. La clave solo se guarda (cifrada) si no hay forma de renovar la sesión sin ella.
- Fuera del alcance: el "reproduciendo ahora" por MediaSession o `ContentPlaying`, hacer scrobbling de Movistar a Trakt, leer el canal de preview de Stremio, abrir un issue o PR en Gathin23 o una fila en su `SHOWCASE.md`, workflows de CI de releases y cualquier cambio en el guard de Home o en las reglas de seguridad.

## Fases

### Fase 0: Fork y migración de los cambios personales

Repositorio fork operativo, al día con upstream y con los cambios personales encima, con las pruebas del repo pasando.

- [x] Crear el fork de `Gathin23/cleanyourtv` en GitHub y reconfigurar los remotos del clon local: `origin` al fork, `upstream` a Gathin23 y añadir `gwiersma` (`https://github.com/gwiersma/cleanyourtv`).
- [x] Rebasar `dd416ccb` y `d5933b95` sobre `upstream/main`, resolviendo conflictos en `themes/aurora/config.json`.
- [x] Revisar `examples/tcl-55c725/spain-rc902v-m1.md`, `themes/aurora/config.json`, `.vscode/settings.json` y los archivos de herramientas en busca de identificadores (IP, Wi-Fi, serie, `greeting_name`, rutas personales, tokens) y eliminarlos o generalizarlos.
- [x] Añadir a `.gitignore` los archivos locales de herramientas que no deben subirse (como `.claude/settings.local.json`) y crear un commit con `.agents/`, `.ai/`, `.claude/`, `.github/copilot-instructions.md`, `.github/hooks/`, `openspec/` y `CLAUDE.md`, que debe seguir importando `@AGENTS.md` (`tests/test_agents_md.py`). Por decisión del usuario, `.ai/` y `.agents/` quedan ignorados y solo en local, y `.claude/` (un enlace a `.ai/skills`) tampoco se sube; después se sacaron también del historial `.github/copilot-instructions.md`, `.github/hooks/rtk-rewrite.json`, `openspec/config.yaml` y `openspec/schemas/`.
- [x] Añadir `FORK.md` con la procedencia (Gathin23 y gwiersma), el procedimiento de rebase sobre `upstream/main` y la lista de diferencias, siguiendo la forma del `FORK.md` de gwiersma.
- [x] Hacer push de `main` al fork.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 1: Fila Seguir viendo con lo que está a medias en Trakt

En Home aparece la fila "Seguir viendo" con los episodios y películas pausados en Trakt (desde Stremio o Kodi). Al pulsar OK se abre Stremio. Se conecta con el código de dispositivo y, de momento, sin pósters (se ve el banner del add-on).

#### Contratos

```java
// Dominio (add-on)
enum ViewingKind { CONTINUE, NEXT }
final class ViewingItem {
    String sourceId(); String itemKey(); String showKey(); ViewingKind kind();
    String title(); String episodeTitle(); Integer season(); Integer episode();
    long positionMs(); long durationMs(); long lastEngagedMs();
    String posterKey(); OpenTarget target();
}
final class OpenTarget { String targetPackage(); String deepLink(); }

// Puertos (add-on)
interface ViewingSource { String id(); SourceState state(); List<ViewingItem> fetch() throws IOException, AuthException; }
interface WatchNextStore { void replaceSource(String sourceId, List<ViewingItem> items); void clearSource(String sourceId); }
interface SecretStore { void put(String key, String value); String get(String key); void remove(String key); }
enum SourceState { UNAVAILABLE, DISCONNECTED, CONNECTED, RECONNECT_NEEDED }

// Servicios de aplicación (add-on)
final class SyncService { SyncReport syncAll(); SyncReport sync(String sourceId); }
final class TraktAuth { DeviceCode requestCode() throws IOException; PollResult poll(DeviceCode code) throws IOException; void refreshIfNeeded() throws IOException, AuthException; }

// Launcher (adaptado de gwiersma)
final class ContinueWatching { static List<ContinueEntry> filter(List<ContinueEntry> rows, long nowMs, Set<String> allowedApps); static boolean enabled(boolean configured, Boolean chosenOnTv); }
```

```xml
<!-- Textos de interfaz de usuario (add-on, res/values-es) -->
<string name="trakt_connect">Conectar Trakt</string>
<string name="trakt_code">Entra en %1$s e introduce %2$s</string>
<string name="trakt_expired">El código ha caducado. Pide uno nuevo.</string>
<string name="trakt_unavailable">Falta trakt.clientId en local.properties; añádelo y vuelve a compilar</string>
<string name="sync_now">Sincronizar ahora</string>
<string name="last_sync">Última sincronización: %1$s</string>
<string name="target_missing">%1$s no está instalada</string>
```

#### Tests

- `launcher/app/src/test/java/com/gathin/tvhome/ContinueWatchingTest`
  - dropsRowsOlderThanSixtyDays (Entrada antigua)
  - dropsContinueRowsPastNinetyFivePercent (Episodio casi terminado)
  - keepsNewestRowPerShow (Varios episodios de la misma serie)
  - capsAtTenNewestFirst (Filtrado y orden de entradas)
  - allowedAppsOnlyWhenListed (Publicidad de otra app)
  - emptyAllowListShowsEveryApp (Lista vacía)
  - emptyInputHidesRow (Tabla vacía)
  - followsIntentOnlyFromPublisher (Destino de la propia app, Destino de otra app)
- `launcher/app/src/test/java/com/gathin/tvhome/ContinueWatchingConfigTest`
  - offByDefault (Interruptor desactivado)
  - choiceOnTvWinsOverConfig (Activar desde los ajustes, Desactivar desde los ajustes)
  - themeSyncKeepsTheChoice (Cambio de tema)
- `addons/seguir-viendo/app/src/test/.../WatchNextMapperTest`
  - mapsContinueItemWithPositionAndDuration (Episodio a medias)
  - marksRowsSearchable (Episodio a medias)
  - targetsOwnTrampoline (Apertura a través del add-on)
- `addons/seguir-viendo/app/src/test/.../SyncServiceTest`
  - replacesOnlyTheSyncedSource (Elemento terminado en otro dispositivo)
  - keepsPreviousRowsWhenSourceFails (Fallo de una fuente)
  - authFailureMarksReconnectNeededAndKeepsRows (Token rechazado)
- `addons/seguir-viendo/app/src/test/.../TraktAuthTest`
  - pollReturnsTokenAfterActivation (Activación completada)
  - pollReportsExpiredCode (Código caducado)
  - missingClientIdMakesSourceUnavailable (Compilación sin credenciales)
  - worksWithoutClientSecretUsingPkce (Credenciales de la aplicación en la compilación)
- `addons/seguir-viendo/app/src/test/.../TraktPlaybackParserTest`
  - pausedEpisodeBecomesContinueItem (Episodio pausado en Kodi)
- `addons/seguir-viendo/app/src/test/.../TrampolineTest`
  - opensStremioByDefault (Destino por defecto)
  - reportsMissingTargetApp (App de destino no instalada)
- `addons/seguir-viendo/app/src/test/.../SecretStoreTest`
  - neverLogsSecrets (Registros del sistema)

- [x] Cherry-pick de `gwiersma/d723b1f` sobre `main`, resolviendo conflictos en `MainActivity.java`, `config.xml` y `build-and-install.sh` sin tocar el guard de Home. Dejar `continue_watching` desactivado en `config.xml` y añadir el interruptor "Seguir viendo" en los ajustes del launcher, siguiendo el patrón de `animationsChoice` en `LauncherSettingsActivity.java`.
- [x] Añadir `continue_watching_apps` (string-array en `config.xml`, vacía por defecto en upstream y con el add-on en este fork) y aplicarla en `ContinueWatching.filter`, documentada en `docs/watch-next.md` y exenta en `tests/test_theme_keys_config.py` como `continue_watching`.
- [x] Extraer el filtrado de `ContinueWatching` a una función pura comprobable y añadir `ContinueWatchingTest` y `ContinueWatchingConfigTest`.
- [x] Crear el proyecto `addons/seguir-viendo/` copiando la estructura de `addons/game-day/` (Gradle, `.gitignore` con `local.properties`, `README.md`; sin `install.sh`, que game-day no tiene: la receta de instalación va en el README), con `INTERNET` y `com.android.providers.tv.permission.WRITE_EPG_DATA`, y la lectura de `local.properties` en `BuildConfig`.
- [x] Implementar `SecretStore` con una clave AES de Android Keystore, siguiendo el patrón de almacenamiento de `addons/game-day/.../CalendarStore.java`.
- [x] Implementar `TraktAuth` (`/oauth/device/code`, `/oauth/device/token`, `/oauth/token` para renovar) y su pantalla de conexión, con el código, la URL y el estado.
- [x] Implementar `TraktSource` para elementos `CONTINUE` a partir de `/sync/playback`.
- [x] Implementar `WatchNextStore` sobre `TvContract.WatchNextPrograms`, con `searchable=1`, `internal_provider_id` igual a `sourceId:itemKey` y `intent_uri` hacia el trampolín.
- [x] Implementar la actividad trampolín, que abre el paquete de destino o muestra `target_missing`.
- [x] Implementar `SyncService` y el botón "Sincronizar ahora" con la hora de la última sincronización.
- [x] Añadir la fila del add-on en `addons/README.md` y su `README.md` con los permisos, la configuración de `local.properties` y el trabajo en segundo plano.
- [x] Probar en la TCL C725: instalar el fork con `launcher/build-and-install.sh`, conectar Trakt, pausar un episodio en Stremio, sincronizar, pulsar Home y comprobar la fila, el D-pad desde los cuatro bordes y la apertura de Stremio, con capturas `tools/tv.py --screenshot shots/seguir-viendo-1.png`. En la prueba, Abajo desde la barra superior saltaba la fila: `pillKeys` de upstream, posterior al commit de gwiersma, enviaba el foco al dock; ahora va a la fila cuando está en pantalla, y sobra el `nextFocusDownId` de `updateTopBarDown`.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 2: Siguiente episodio, pósters, deep link y sincronización automática

La fila muestra también el siguiente episodio de cada serie, con carátulas reales servidas en local, abre Stremio en el contenido concreto y se actualiza sola. La fuente se puede desconectar.

#### Contratos

```java
// Puertos (add-on)
interface PosterStore { String uriFor(String posterKey); String put(String posterKey, byte[] image) throws IOException; void retainOnly(Set<String> posterKeys); }
interface ArtworkLookup { byte[] poster(String tmdbId, String type) throws IOException; }
interface Targets { String targetPackage(String sourceId); void setTargetPackage(String sourceId, String packageName); }

// Servicios de aplicación (add-on)
final class Posters { List<ViewingItem> attach(List<ViewingItem> items); void retainOnly(Collection<ViewingItem> published); }
final class SyncScheduler { static void schedule(Context context); static void syncSoon(Context context); static void cancel(Context context); }
final class SourcesService { void disconnect(String sourceId); void setTargetPackage(String sourceId, String packageName); }
```

```xml
<!-- Textos de interfaz de usuario (add-on, res/values-es) -->
<string name="disconnect">Desconectar</string>
<string name="target_app">Abrir en: %1$s</string>
<string name="posters_on">Carátulas de TMDB</string>
<string name="posters_off">Sin carátulas: falta tmdb.apiKey en local.properties</string>
<string name="reconnect_needed">Vuelve a conectar la cuenta</string>
```

#### Tests

- `addons/seguir-viendo/app/src/test/.../TraktProgressParserTest`
  - finishedEpisodeYieldsNextEpisode (Episodio terminado)
  - continueItemSuppressesNextForSameShow (Serie con episodio a medias)
- `addons/seguir-viendo/app/src/test/.../PosterStoreTest`
  - storedPosterIsServedAsContentUri (Póster descargado)
  - missingArtworkPublishesWithoutPoster (Sin imagen)
  - orphanPostersAreRemoved (Sustitución por fuente)
- `addons/seguir-viendo/app/src/test/.../SourcesServiceTest`
  - disconnectRemovesSecretsAndRows (Desconectar)
  - targetChoiceIsKeptPerSource (App de destino configurable)
- `addons/seguir-viendo/app/src/test/.../SyncSchedulerTest`
  - periodicJobRequiresNetwork (Sin red)
- `addons/seguir-viendo/app/src/test/.../StremioLinkTest`
  - buildsEpisodeDeepLink (Contenido abrible)
  - linksOnlyForStremio (Destino por defecto)
- `addons/seguir-viendo/app/src/test/.../TmdbArtworkTest`
  - prefersTheWideBackdropForTheRowCard (Póster descargado)
  - readAccessTokenGoesInTheHeaderNotTheUrl (Credenciales protegidas)
  - unknownShowHasNoArtwork (Sin imagen)
- `addons/seguir-viendo/app/src/test/.../TrampolineTest`
  - cyclesThroughInstalledTargets (App de destino configurable)

- [x] Ampliar `TraktSource` con elementos `NEXT` a partir de `/sync/watched/shows` y `/shows/{id}/progress/watched`, limitado a las series con actividad en los últimos 60 días.
- [x] Implementar `ArtworkLookup` con TMDB y `PosterStore` con un provider exportado de solo lectura por id opaco. Limpiar los pósters huérfanos en cada sincronización.
- [x] Verificar en la TV el formato del deep link de Stremio por episodio y por película, e implementar `StremioLink` con vuelta a abrir la app si falla. Verificado con Stremio 1.12.0 (Sherlock T1·E1 y una película): los dos abren la ficha.
- [x] Implementar `SyncScheduler` (trabajo periódico de 1 h con red, receptor de `BOOT_COMPLETED`) y la desconexión, junto con la elección de la app de destino en la configuración (Stremio o Kodi). Añade los permisos `RECEIVE_BOOT_COMPLETED` y `ACCESS_NETWORK_STATE`, declarados en el README del add-on.
- [x] Probar en la TV: terminar un episodio en Stremio y ver el siguiente en la fila con póster. Pausar en Kodi del otro dispositivo y ver la entrada tras la sincronización automática. Desconectar y comprobar que la fila queda vacía. Hecho en la TCL C725: los siguientes episodios salen del historial real de Trakt (Family Guy, Lupin, MobLand, Tulsa King) con carátula, OK abre el episodio en Stremio, un episodio recién pausado (Person of Interest T1·E1) llega con el trabajo programado (forzado con `cmd jobscheduler run`) y Desconectar deja la fila vacía y cancela el trabajo; al reconectar vuelven las filas y el trabajo. En la prueba los botones del add-on no mostraban el foco con el mando; ahora el enfocado se ve en blanco.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 3: Fuente Movistar Plus

La fila incluye las últimas reproducciones de Movistar Plus del perfil elegido y las abre en la app oficial.

#### Contratos

```java
// Servicios de aplicación (add-on)
final class MovistarAuth { LoginResult login(String user, String password) throws IOException; List<Profile> profiles() throws IOException, AuthException; void selectProfile(String profileId); boolean deviceRegistrationRequired(); void registerDevice() throws IOException; void refreshIfNeeded() throws IOException, AuthException; }
final class MovistarSource implements ViewingSource { }
```

```xml
<!-- Textos de interfaz de usuario (add-on, res/values-es) -->
<string name="movistar_connect">Conectar Movistar Plus</string>
<string name="movistar_bad_password">Usuario o clave no válidos</string>
<string name="movistar_device_warning">Movistar Plus contará este add-on como un dispositivo más de tu cuenta. ¿Continuar?</string>
<string name="movistar_profile">Perfil</string>
```

#### Tests

- `addons/seguir-viendo/app/src/test/.../MovistarAuthTest`
  - validLoginConnects (Conexión correcta)
  - badPasswordStoresNothing (Clave incorrecta)
  - registrationAsksBeforeTakingADeviceSlot (Alta necesaria)
  - rejectedRefreshMarksReconnectNeeded (Clave cambiada)
- `addons/seguir-viendo/app/src/test/.../MovistarViewingsParserTest`
  - partlyWatchedEpisodeBecomesContinueItem (Serie a medias)
  - readsSelectedProfileOnly (Cambio de perfil)
- `addons/seguir-viendo/app/src/test/.../TrampolineTest`
  - opensOfficialMovistarApp (Abrir elemento)

- [ ] Spike en la TV con la cuenta real: comprobar si `ultimasreproducciones` responde sin registrar dispositivo, si el token tiene `refresh_token` o caducidad renovable, y qué paquete y deep link usa la app oficial (`dumpsys package` de la app instalada). Anotar los resultados en `addons/seguir-viendo/docs/movistar.md`, sin datos de la cuenta.
- [ ] Implementar `MovistarAuth` según el spike (la clave cifrada en `SecretStore` solo si no hay renovación sin ella) y la pantalla de conexión con la selección de perfil y el aviso de dispositivo.
- [ ] Implementar `MovistarSource` sobre `viewings` con `container=trackedseries`, con fixtures JSON anonimizados en `app/src/test/resources`.
- [ ] Añadir al `README.md` del add-on el aviso de API no oficial, la nota de procedencia del protocolo y lo que guarda en la TV.
- [ ] Probar en la TV: ver a medias un episodio en la app oficial, sincronizar, comprobar la entrada en la fila y que se abre Movistar Plus.
- [ ] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

## Siguiente paso

Ejecuta `/openspec-apply-change` para implementar la Fase 3: la fuente Movistar Plus, empezando por el spike en la TV con la cuenta real (registro de dispositivo, renovación del token y deep link de la app oficial).
