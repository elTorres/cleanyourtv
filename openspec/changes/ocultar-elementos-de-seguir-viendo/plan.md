---
name: "ocultar-elementos-de-seguir-viendo"
description: "Pulsación larga en las tarjetas de Seguir viendo que abre las opciones del add-on para ocultar un episodio, su temporada, la serie o una película, restablecibles desde los ajustes del add-on"
created_at: "2026-11-02T21:30:30Z"

created_by:
  tool: "Claude Code"
  model:
    name: "Claude Opus"
    version: "5.5"
    reasoning_effort: "high"

implemented_by:
  tool: "Claude Code"
  model:
    name: "Claude Opus"
    version: "5.5"
    reasoning_effort: "high"
last_implementation_at: "2026-11-02T22:05:29Z"
---

# Ocultar elementos de Seguir viendo

## Objetivo

Que la persona pueda quitar de la fila "Seguir viendo" lo que no quiere seguir viendo (un episodio, una temporada, una serie o una película) manteniendo pulsado OK sobre la tarjeta, sin tocar Trakt ni Movistar Plus, y deshacerlo desde los ajustes del add-on. El launcher solo abre las opciones de la app que publicó la entrada; la lista de ocultos, el menú y su restablecimiento viven en el add-on.

## Decisiones

- Los ocultos viven en el add-on (elegido por el usuario), en lugar de en el launcher: es el dueño de sus filas de Watch Next y puede borrarlas al momento, filtra lo oculto en cada sincronización y tiene ya una pantalla de ajustes. Solo las entradas del add-on tienen opciones; en este fork son todas, porque `continue_watching_apps` solo lleva el add-on.
- El menú lo dibuja el add-on (elegido por el usuario), en lugar del launcher: el launcher no conoce los alcances ni lleva textos nuevos, y `MainActivity` no cambia.
- El launcher define un protocolo genérico, documentado en `docs/watch-next.md`: con pulsación larga abre una actividad de la app que publicó la entrada, con la acción `com.gathin.tvhome.action.CONTINUE_WATCHING_OPTIONS`, la categoría `DEFAULT`, `setPackage(pkg)` y el `_id` de la fila de Watch Next en el extra `com.gathin.tvhome.extra.WATCH_NEXT_PROGRAM_ID`. Si la app no la declara, la pulsación larga no hace nada, así que nada cambia para las demás apps. La acción se añade a `<queries>` del launcher, aunque el paquete del add-on ya es visible por su `LEANBACK_LAUNCHER`.
- Se pasa el `_id` de la fila, que cualquier lector ve, en lugar de `internal_provider_id`: el add-on busca esa fila entre las suyas, leyendo la tabla entera y filtrando en código como `TvWatchNextStore.ownRows`, porque pasar una selección puede lanzar `SecurityException`.
- El add-on guarda la `showKey`, la temporada y el episodio de cada elemento en `internal_provider_data` de su fila, porque `internal_provider_id` (`fuente:itemKey`) no incluye la serie y las columnas de temporada y episodio solo se publican cuando el elemento tiene las dos (un episodio de Movistar Plus sin temporada no lleva número). `COLUMN_SERIES_ID` se descarta porque exige API 30 y el `minSdk` es 26.
- Las claves de los ocultos son por fuente: `fuente|itemKey` para un episodio o una película, `fuente|showKey|temporada` para una temporada y `fuente|showKey` para una serie (una película tiene `showKey` igual a `itemKey`). Como Trakt y Movistar Plus usan ids distintos, ocultar una serie en una fuente no la oculta en la otra, en lugar de igualar por título.
- Las opciones dependen de la temporada y el episodio del elemento: con episodio, "Ocultar este episodio", "Ocultar la temporada N" (si hay temporada) y "Ocultar la serie"; sin episodio, "No mostrar más" con alcance de serie. El menú no repite "Abrir".
- Los ocultos se guardan en unas `SharedPreferences` propias del add-on (`hidden`), separadas de `settings`, como un único texto con una clave por línea en orden de alta y con un máximo de 200 (se descartan las más antiguas). La desconexión de una fuente no los borra.
- `SyncService` descarta lo oculto antes de pedir los pósters, para no descargar carátulas que no se van a publicar. Si se oculta un episodio a medias de Trakt, no aparece el siguiente de esa serie hasta que Trakt deje de tenerlo a medias, porque `TraktProgress` no propone siguiente episodio para una serie con un elemento a medias.
- La actividad de opciones es exportada y translúcida, para que la fila se vea detrás. Cualquier app puede abrirla, pero solo encuentra filas del add-on y no oculta nada sin una elección de la persona.
- Al cerrar las opciones, el launcher vuelve a leer Watch Next en `onResume`, y `ContinueRow.setItems` recoloca el foco: la regla de foco se extrae a una función pura para probarla.
- Las pruebas son JUnit en la JVM en los dos proyectos, sin pruebas de interfaz. La pulsación larga, Atrás, la pulsación corta y cancelar el diálogo se comprueban en la TV, como pide `AGENTS.md` para los cambios del launcher.
- Los textos nuevos se añaden al add-on en inglés (`values`) y español (`values-es`). El launcher no lleva textos nuevos.
- Fuera del alcance: escribir en Trakt o en Movistar Plus (ocultar en el progreso, borrar pausas); opciones para entradas de otras apps; recuperar ocultos de uno en uno; caducidad automática de los ocultos; cualquier cambio en los ajustes del launcher.

## Fases

### Fase 1: Ocultar desde la fila con las opciones del add-on

Al mantener pulsado OK sobre una tarjeta de "Seguir viendo" se abre el menú del add-on con las opciones de ocultar el episodio, la temporada o la serie (o "No mostrar más" en una película). Al elegir, sus entradas desaparecen de Watch Next y de la fila, y no vuelven en las siguientes sincronizaciones ni tras reiniciar.

#### Contratos

```java
// Servicios de aplicación (launcher, ContinueWatching)
static final String OPTIONS_ACTION = "com.gathin.tvhome.action.CONTINUE_WATCHING_OPTIONS";
static final String EXTRA_PROGRAM_ID = "com.gathin.tvhome.extra.WATCH_NEXT_PROGRAM_ID";
static Intent optionsIntent(Context context, Item item);   // null: la app no ofrece opciones
static int focusAfter(int lastIndex, int count);           // -1: el dock
ContinueEntry(String pkg, String title, String subtitle, String intentUri, String posterUri, boolean browsable,
              long engagedMs, long positionMs, long durationMs, int type, long programId);
Item(String pkg, String title, String subtitle, String intentUri, float progress, String posterUri, long programId);

// Dominio (add-on)
enum HideScope { EPISODE, SEASON, SHOW }
final class PublishedRow { long id(); String sourceId(); String itemKey(); String showKey(); String title(); Integer season(); Integer episode(); }
final class HiddenItems {
    static List<HideScope> scopesFor(Integer season, Integer episode);
    static String key(String sourceId, String itemKey, String showKey, Integer season, HideScope scope);
    static boolean hides(Set<String> keys, ViewingItem item);
    static boolean hides(Set<String> keys, PublishedRow row);
}

// Puertos y repositorios (add-on)
interface HiddenStore { Set<String> keys(); void add(String key); }
final class PrefsHiddenStore implements HiddenStore { static final int MAX_KEYS = 200; static String append(String stored, String key); static Set<String> parse(String stored); }
interface WatchNextStore { void replaceSource(String sourceId, List<ViewingItem> items); void clearSource(String sourceId); List<PublishedRow> published(); void delete(long rowId); }

// Servicios de aplicación (add-on)
final class HideService { PublishedRow row(long rowId); List<HideScope> options(PublishedRow row); void hide(PublishedRow row, HideScope scope); }
public final class OptionsActivity extends Activity { }
```

```xml
<!-- Textos de interfaz de usuario (add-on, res/values-es; en res/values en inglés) -->
<string name="hide_episode">Ocultar este episodio</string>                  <!-- Hide this episode -->
<string name="hide_season">Ocultar la temporada %1$d</string>              <!-- Hide season %1$d -->
<string name="hide_show">Ocultar la serie</string>                         <!-- Hide the series -->
<string name="hide_title">No mostrar más</string>                          <!-- Don\'t show again -->
<string name="entry_gone">Esta entrada ya no está en Seguir viendo</string> <!-- This item is no longer in Continue watching -->
```

#### Tests

- `launcher/app/src/test/java/com/gathin/tvhome/ContinueWatchingTest`
  - optionsActionIsQueriedInTheManifest (App con opciones): lee `AndroidManifest.xml` y comprueba que `<queries>` declara `OPTIONS_ACTION`
  - focusStaysAtPositionOrPrevious (Quedan tarjetas)
  - focusGoesToDockWhenRowEmpties (Última tarjeta)
- `addons/seguir-viendo/app/src/test/.../HiddenItemsTest`
  - episodeWithSeasonOffersEpisodeSeasonAndShow (Episodio con temporada)
  - episodeWithoutSeasonOffersEpisodeAndShow (Episodio sin temporada)
  - movieOffersOnlyShow (Película)
  - hiddenEpisodeKeepsNextEpisode (Siguiente episodio tras ocultar uno)
  - hiddenSeasonKeepsOtherSeasons (Otra temporada tras ocultar una)
  - hiddenShowHidesEveryEpisode (Serie oculta)
  - sameTitleInAnotherSourceStays (Misma serie en otra fuente)
- `addons/seguir-viendo/app/src/test/.../HideServiceTest`
  - hidingRemovesEveryMatchingRow (Ocultar una serie con dos entradas)
  - unknownRowOffersNothing (Entrada ya sustituida)
  - hidingNeverTouchesTheServices (Pausa en Trakt): lee `HideService.java` y `OptionsActivity.java` y comprueba que no usan `Http`, `TraktAuth` ni `MovistarAuth`
- `addons/seguir-viendo/app/src/test/.../SyncServiceTest`
  - hiddenItemsAreNotPublished (La fuente vuelve a devolver el elemento)
- `addons/seguir-viendo/app/src/test/.../PrefsHiddenStoreTest`
  - storedKeysRoundTripCappedOldestFirst (Reinicio de la TV)
- `addons/seguir-viendo/app/src/test/.../WatchNextMapperTest`
  - storesShowKeyInProviderData (Ocultar una serie con dos entradas)
- Prueba en la TV (sin capa de pruebas de interfaz): App sin opciones, Pulsación corta, Cerrar sin elegir

- [x] Launcher: añadir `programId` (el `_id` de la fila) a `ContinueEntry` e `Item`, leído en `ContinueWatching.load`, y actualizar los constructores de `ContinueWatchingTest`.
- [x] Launcher: añadir `OPTIONS_ACTION`, `EXTRA_PROGRAM_ID` y `optionsIntent` (que resuelve la actividad con `resolveActivity` y añade `FLAG_ACTIVITY_NEW_TASK`, como `intentFor`) a `ContinueWatching`, la acción a `<queries>` de `AndroidManifest.xml`, y en `ContinueRow.card` un `setOnLongClickListener` que la abre o no hace nada, devolviendo siempre `true` para que no se dispare también el clic.
- [x] Launcher: extraer la regla de foco de `ContinueRow.setItems` (`lastIndex` acotado o el dock) a `ContinueWatching.focusAfter` y usarla allí. Añadir los casos a `ContinueWatchingTest`.
- [x] Add-on: crear `HideScope`, `PublishedRow`, `HiddenItems`, `HiddenStore` y `PrefsHiddenStore` (con `getSharedPreferences("hidden")`, como `Settings.java`), con `HiddenItemsTest` y `PrefsHiddenStoreTest`.
- [x] Add-on: guardar la `showKey`, la temporada y el episodio en `COLUMN_INTERNAL_PROVIDER_DATA` en `WatchNextMapper.columns` (con una rama `byte[]` en `TvWatchNextStore.values`), e implementar `published()` y `delete(rowId)` en `TvWatchNextStore` sobre la consulta de `ownRows`, y en `MemoryWatchNextStore`.
- [x] Add-on: filtrar lo oculto en `SyncService.syncOne` antes de `posters.attach`, con el caso en `SyncServiceTest`, y crear `HideService` con `HideServiceTest`. Registrar los dos en `Services`.
- [x] Add-on: crear `OptionsActivity` (exportada, tema translúcido, `intent-filter` con `OPTIONS_ACTION` y `DEFAULT`, `excludeFromRecents` y `taskAffinity=""` como `TrampolineActivity`), que lee el extra, muestra el menú con el patrón de `MainActivity.dialog()`, oculta en el `worker` y se cierra; sin fila, muestra `entry_gone` y se cierra; Atrás la cierra sin cambios. Añadir los textos a `res/values` y `res/values-es`.
- [x] Documentar el protocolo de opciones en la sección "In the launcher" de `docs/watch-next.md` y la pulsación larga junto a **Continue watching** en `docs/launcher.md`.
- [x] Probar en la TCL C725 con `launcher/build-and-install.sh` y el add-on enviado con `tools/b64push.py`. Probado por la persona con el mando real (las seis comprobaciones correctas); por ADB no se puede, porque `input keyevent --longpress` suelta la tecla al momento y `sendevent` no tiene permiso en esta TV. El menú ignora OK hasta una pulsación nueva, para que la tecla que se suelta al abrirlo no elija la primera opción. Comprobaciones: mantener OK sobre un episodio de Trakt, uno de Movistar Plus y una película; Atrás cierra el menú sin cambios; OK corto sigue abriendo; ocultar episodio, temporada y serie y comprobar que la tarjeta desaparece y el foco queda en la fila o en el dock; "Sincronizar ahora" y volver a Home sin que reaparezca; reiniciar la TV y comprobarlo de nuevo; D-pad desde los cuatro bordes de la fila. Comprobar también que la fila se ve detrás del menú. Capturas con `tools/tv.py --screenshot shots/ocultar-1.png`.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 2: Restablecer los ocultos desde los ajustes del add-on

En la pantalla del add-on aparece un botón con el número de elementos ocultos de "Seguir viendo" que, tras confirmar, los vuelve a mostrar todos y sincroniza.

#### Contratos

```java
// Puertos y repositorios (add-on)
interface HiddenStore { void clear(); }

// Servicios de aplicación (add-on)
final class HideService { int hiddenCount(); void restoreAll(); }
```

```xml
<!-- Textos de interfaz de usuario (add-on, res/values-es; en res/values en inglés) -->
<string name="hidden_count">Ocultos en Seguir viendo: %1$d</string>                     <!-- Hidden from Continue watching: %1$d -->
<string name="hidden_none">Nada oculto en Seguir viendo</string>                        <!-- Nothing hidden from Continue watching -->
<string name="hidden_reset">¿Volver a mostrar los %1$d elementos ocultos?</string>      <!-- Show the %1$d hidden items again? -->
<string name="hidden_show">Volver a mostrar</string>                                   <!-- Show again -->
```

#### Tests

- `addons/seguir-viendo/app/src/test/.../HideServiceTest`
  - restoreAllEmptiesTheList (Restablecer)
  - countIsZeroWithoutHiddenItems (Ninguno oculto)
- Prueba en la TV (un diálogo de confirmación, sin capa de pruebas de interfaz): Cancelar

- [ ] Añadir `clear` a `HiddenStore` y `PrefsHiddenStore`, y `hiddenCount` y `restoreAll` a `HideService`, con sus casos en `HideServiceTest`.
- [ ] Añadir el botón `hidden` a `activity_main.xml` del add-on, debajo de las dos fuentes, con su texto en `render()` (`hidden_count` o `hidden_none`) y, al pulsarlo con ocultos, una confirmación con `dialog()` que llama a `restoreAll` y lanza `runSync(services.sync::syncAll)`, como el botón "Sincronizar ahora"; sin ocultos no hace nada. Añadir los textos a `res/values` y `res/values-es`.
- [ ] Documentar en el `README.md` del add-on la pulsación larga, qué guarda (`hidden`), la actividad exportada y el botón de restablecer, y añadir la diferencia a la tabla de `FORK.md`.
- [ ] Probar en la TV: con dos elementos ocultos, el botón muestra 2; cancelar los deja ocultos; confirmar sincroniza y, al volver a Home, aparecen; sin ocultos el botón dice "Nada oculto" y no abre el diálogo.
- [ ] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

## Siguiente paso

La Fase 1 está implementada y probada en la TV. Ejecuta `/openspec-apply-change` para implementar la Fase 2.
