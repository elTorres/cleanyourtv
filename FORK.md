# Fork personal de CleanYourTV

Fork de [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) para una TCL 55C725 (Android TV 11) en España.
Sigue el `main` de upstream y añade encima la configuración de esta TV, las herramientas de agentes con OpenSpec y,
en adelante, una fila "Seguir viendo" en Home alimentada por un add-on con Trakt y Movistar Plus.

Nada de lo que está aquí se envía a upstream salvo que se diga lo contrario en un PR aparte.

## Procedencia

| Origen | Qué se toma |
|---|---|
| [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) | Todo: es la base, en el remoto `upstream` |
| [gwiersma/cleanyourtv](https://github.com/gwiersma/cleanyourtv/blob/main/FORK.md) | Solo el commit de la fila Continue watching (lector de Watch Next, fila, interruptor `continue_watching` y `pm grant` de `READ_TV_LISTINGS`), en el remoto `gwiersma`. Su configuración de TV no se toma |

## Diferencias con upstream

| Cambio | Archivos | Estado |
|---|---|---|
| Notas de la TCL 55C725 con el mando RC902V M1 | `examples/tcl-55c725/spain-rc902v-m1.md` | Hecho |
| Tema aurora ajustado a esta TV: dock, etiquetas, `app_keys`, entrada de TDT | `themes/aurora/config.json` | Hecho |
| Herramientas de agentes y OpenSpec | `.github/copilot-instructions.md`, `.github/hooks/`, `openspec/`, `CLAUDE.md` | Hecho |
| Skills y comandos de agentes fuera de git, solo en local | `.gitignore` (`.ai/`, `.agents/`); `.claude/skills` enlaza a `.ai/skills` y tampoco se sube | Hecho |
| Archivos locales de editor fuera de git | `.gitignore` (`.vscode/`, `.claude/settings.local.json`) | Hecho |
| Fila "Seguir viendo" en Home, desactivada por defecto | `launcher/` | Pendiente |
| Add-on con fuentes Trakt y Movistar Plus que publica en Watch Next | `addons/seguir-viendo/` | Pendiente |

El plan completo está en `openspec/changes/seguir-viendo-fork-trakt-movistar/`.

## Remotos

```bash
git remote -v
# origin    https://github.com/elTorres/cleanyourtv.git
# upstream  https://github.com/Gathin23/cleanyourtv.git
# gwiersma  https://github.com/gwiersma/cleanyourtv.git
```

## Ponerse al día con upstream

Upstream cambia a diario y `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` es el archivo que más
conflictos da, así que conviene hacerlo a menudo.

```bash
git fetch upstream
git rebase upstream/main
tools/venv/bin/python -m unittest discover -s tests -v
cd launcher && ./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
git push --force-with-lease origin main
```

Si hay conflicto en `themes/aurora/config.json` o en la tabla de `addons/README.md`, se conservan las dos partes: la de
upstream y la de este fork.

## Lo que no se sube nunca

Además de lo que ya excluye `.gitignore` (backups, capturas, notas, la clave ADB), quedan fuera
`addons/seguir-viendo/local.properties` con las claves de Trakt y TMDB, y cualquier dato de la cuenta de Movistar Plus.
