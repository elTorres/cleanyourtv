---
name: "menu-de-seguir-viendo-como-el-de-las-tiles"
description: "El menú de opciones de Seguir viendo que dibuja el add-on pasa a tener el aspecto del menú de las tarjetas de apps del launcher, sin título, y se abre junto a la tarjeta"
created_at: "2026-11-02T23:02:56Z"

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
last_implementation_at: "2026-11-02T23:27:13Z"
---

# Menú de Seguir viendo como el de las tiles

## Objetivo

Que mantener pulsado OK sobre una tarjeta de "Seguir viendo" abra un menú igual que el de las tarjetas de apps: el mismo panel, las mismas filas y la misma tipografía, sin título y junto a la tarjeta. Hoy es un `AlertDialog` del sistema centrado en pantalla. El menú lo sigue dibujando el add-on y el launcher solo le indica dónde está la tarjeta.

## Decisiones

- El menú lo sigue dibujando el add-on (elegido por el usuario), en lugar de que lo pinte el launcher con `glassPopup`. El protocolo de opciones no cambia: solo gana la posición de la tarjeta, y el aspecto del launcher se copia en el add-on.
- La posición viaja en `Intent.setSourceBounds`, el campo estándar de Android para indicar dónde está en pantalla lo que lanzó el intent, en lugar de un extra propio. Una app que no lo lea sigue funcionando como hasta ahora.
- El rectángulo es el visual de la tarjeta enfocada: la esquina de `getLocationOnScreen`, que ya aplica la escala, y el ancho y el alto multiplicados por `getScaleX` y `getScaleY`. Así el panel no pisa el anillo de foco de la escala `FOCUS_SCALE`.
- La colocación copia la de `MainActivity.onTileLongPress`: el panel va debajo de la tarjeta, alineado con su borde inicial y con 10dp de separación; si no cabe va encima, y se ajusta para quedar dentro de la pantalla. El ancho es de 200dp como mínimo o el que mida el contenido. Sin posición, el panel sale centrado.
- El panel es una vista dentro de la propia `OptionsActivity`, que ya es translúcida y ocupa toda la pantalla, en lugar de un `PopupWindow`. Se coloca con coordenadas absolutas, restando la posición en pantalla de la vista raíz.
- Del launcher se copian `glass_panel`, `input_row_bg`, `pill_text`, la fila de 42dp sin icono ni estado de `item_input.xml` (la variante de `addSettingsMenuRow`), el relleno de 6dp de `popup_inputs.xml` y Outfit Medium (`outfit.ttf` con `outfit_medium.xml`, unos 108 KB que solo pesan en el APK del add-on). Junto a la fuente se incluye `FONT-LICENSE-OFL.txt`, como en `addons/prayer-times`.
- Una prueba del add-on compara sus drawables del menú con los del launcher, para que la copia no se desvíe sin que nadie se entere. Esto acopla el add-on a la ruta del launcher dentro del repo, cosa que se acepta en este fork.
- La animación es la de `dropIn` (aparece y se despliega desde el borde de la tarjeta en 220ms). El add-on no ve el ajuste "reducir movimiento" del launcher; `ViewPropertyAnimator` ya sigue la escala de animaciones del sistema, y eso basta.
- El menú no tiene título, igual que el de las tiles. La protección del OK al soltar pasa del diálogo a las filas, con el patrón de `addSettingsMenuRow`.
- Fuera del alcance: los demás menús del launcher (entradas, ajustes, biblioteca), el toast `entry_gone`, el ajuste "reducir movimiento" del launcher dentro del add-on, opciones para entradas de otras apps y compartir una librería de estilos entre el launcher y el add-on.

## Fases

### Fase 1: El menú del add-on con el aspecto del de las tiles

El menú de opciones se ve igual que el de las tarjetas de apps, sin título, y de momento aparece centrado, porque el launcher todavía no envía la posición. Ya se puede usar en la TV con el launcher actual.

#### Contratos

```java
// Dominio (add-on)
final class MenuPlacement {
    static MenuPlacement centered(int menuWidth, int menuHeight, int screenWidth, int screenHeight);
    int x();
    int y();
    boolean above();
}
```

```xml
<!-- Recursos de interfaz de usuario (add-on) -->
<!-- res/drawable/menu_panel.xml       copia de launcher res/drawable/glass_panel.xml -->
<!-- res/drawable/menu_row.xml         copia de launcher res/drawable/input_row_bg.xml -->
<!-- res/color/menu_row_text.xml       copia de launcher res/drawable/pill_text.xml -->
<!-- res/font/outfit.ttf, res/font/outfit_medium.xml   copias del launcher -->
<!-- res/layout/options_menu.xml       panel de popup_inputs.xml, a pantalla completa y transparente -->
<!-- res/layout/options_row.xml        fila de item_input.xml sin icono, punto ni estado -->
```

#### Tests

- `addons/seguir-viendo/app/src/test/java/io/github/eltorres/seguirviendo/MenuStyleTest`
  - menuDrawablesMatchTheLauncher (Menú abierto): lee `menu_panel.xml`, `menu_row.xml` y `menu_row_text.xml` y comprueba que sus elementos coinciden con `glass_panel.xml`, `input_row_bg.xml` y `pill_text.xml` de `launcher/app/src/main/res/drawable`
- `addons/seguir-viendo/app/src/test/java/io/github/eltorres/seguirviendo/MenuPlacementTest`
  - centersWithoutBounds (Sin posición)
- Prueba en la TV (sin capa de pruebas de interfaz): Menú abierto, sin título y con el mismo aspecto que el de una tarjeta de app

- [x] Copiar al add-on los drawables, el color de texto, `outfit.ttf`, `outfit_medium.xml` y `launcher/FONT-LICENSE-OFL.txt` con los nombres de los contratos, y crear `options_menu.xml` y `options_row.xml` a partir de `popup_inputs.xml` e `item_input.xml`.
- [x] Crear `MenuPlacement` con `centered` y `MenuPlacementTest`, y `MenuStyleTest`.
- [x] En `OptionsActivity.show`, cambiar el `AlertDialog` por `options_menu.xml`: una `options_row.xml` por alcance con el texto de `label`, sin título, con el foco en la primera fila y la tipografía de `outfit_medium`. Cada fila oculta con `hide(row, scope)` tras una pulsación nueva de OK, como `MainActivity.addSettingsMenuRow`; esto sustituye a `heldFromTheLauncher`. Atrás sigue cerrando sin cambios. Colocar el panel con `MenuPlacement.centered` y animarlo como `MainActivity.dropIn`.
- [x] Probar en la TCL C725 con el add-on enviado con `tools/b64push.py` y el launcher actual. La persona mantiene OK con el mando real sobre un episodio con temporada, otro sin temporada y una película, y compara el menú con el de una tarjeta de app. Comprobar también que Atrás cierra sin cambios y que al elegir se oculta como antes. Capturas con `tools/tv.py --screenshot shots/menu-1.png`. Probado por la persona con el mando real: Blue Lights (T1·E4) y Lupin (T3·E4) abren sus tres filas sin título, centradas, con la primera enfocada, y el menú sigue abierto al soltar OK; Atrás los cierra y las tarjetas siguen en la fila; la película abre su única opción y elegir una opción la oculta. El episodio sin temporada no se mencionó aparte; sus opciones las cubre `HiddenItemsTest` y el menú las pinta con el mismo bucle. Por ADB no se pueden abrir, porque el shell de esta TV no ve las filas de Watch Next. Las capturas `shots/menu-1.png` y `shots/menu-tiles.png` (menú de Stremio en el dock) coinciden en panel, margen interior (~11 px), alto de fila (~84 px, 42dp), resaltado, margen del texto (~55 px) y tipografía; solo cambia la posición, que es la Fase 2.
- [x] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

### Fase 2: El menú junto a la tarjeta

El launcher envía la posición visual de la tarjeta enfocada y el add-on coloca el menú debajo de ella, o encima si no cabe, dentro de la pantalla.

#### Contratos

```java
// Servicios de aplicación (launcher, ContinueWatching)
static Intent optionsIntent(Context context, Item item, Rect cardBounds);
static int[] visualBounds(int screenLeft, int screenTop, int width, int height, float scaleX, float scaleY);   // {left, top, right, bottom}

// Dominio (add-on)
final class MenuPlacement {
    static MenuPlacement nextTo(int cardLeft, int cardTop, int cardRight, int cardBottom, int menuWidth, int menuHeight,
                                int screenWidth, int screenHeight, int gap, boolean rtl);
}
```

#### Tests

- `launcher/app/src/test/java/com/gathin/tvhome/ContinueWatchingTest`
  - visualBoundsIncludeTheFocusScale (App con opciones)
- `addons/seguir-viendo/app/src/test/java/io/github/eltorres/seguirviendo/MenuPlacementTest`
  - fitsBelowAlignsToCardStart (Cabe debajo)
  - opensAboveWhenBelowDoesNotFit (No cabe debajo)
  - staysInsideTheScreenNearTheEndEdge (Tarjeta junto al borde)
- Prueba en la TV: App con opciones (el menú sale junto a la tarjeta), App sin opciones y Pulsación corta, que no cambian

- [ ] Launcher: añadir `visualBounds` a `ContinueWatching` con su caso en `ContinueWatchingTest`, y pasar a `optionsIntent` el `Rect` que `ContinueRow.openOptions` calcula con `getLocationOnScreen`, `getScaleX` y `getScaleY` de la tarjeta, que lo pone con `setSourceBounds`.
- [ ] Add-on: añadir `MenuPlacement.nextTo` con la regla de `MainActivity.onTileLongPress` (debajo con 10dp de separación, encima si no cabe, alineado con el borde inicial según `rtl` y ajustado a la pantalla) y sus casos en `MenuPlacementTest`. En `OptionsActivity`, usarlo cuando `getIntent().getSourceBounds()` no sea nulo y `centered` cuando lo sea. Encima, desplegar desde abajo, como la rama `showAtLocation` de `onTileLongPress`.
- [ ] Documentar `setSourceBounds` en el protocolo de opciones de `docs/watch-next.md`, el aspecto y la posición del menú en el `README.md` del add-on, y añadir a la tabla de `FORK.md` la diferencia del launcher.
- [ ] Probar en la TCL C725 con `launcher/build-and-install.sh` y el add-on enviado con `tools/b64push.py`. La persona mantiene OK con el mando real sobre la primera y la última tarjeta de la fila y sobre una del medio, y comprueba que el menú sale pegado debajo o encima, sin pisar el anillo de foco y entero dentro de la pantalla. Comprobar que OK corto sigue abriendo y que una app sin opciones no abre nada. Capturas con `tools/tv.py --screenshot shots/menu-2.png`.
- [ ] Verifica los cambios en cuanto a comprobación de tipos, linting y pruebas utilizando el comando de verificación del proyecto. Busca el comando en el archivo AGENTS.md o en la configuración del proyecto. Corrige cualquier problema que aparezca.

## Siguiente paso

La Fase 1 está implementada y probada en la TV. Ejecuta `/openspec-apply-change` para implementar la Fase 2, el menú junto a la tarjeta.
