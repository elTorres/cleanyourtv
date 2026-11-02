## MODIFIED Requirements

### Requirement: Opciones de la app con pulsación larga
El sistema SHALL abrir, cuando la persona mantiene pulsado OK sobre una tarjeta de "Seguir viendo", las opciones que ofrece para esa entrada la app que la publicó, indicándole la posición en pantalla de la tarjeta tal como se ve enfocada, y SHALL NOT hacer nada cuando esa app no ofrece opciones.

#### Scenario: App con opciones
- Given una tarjeta publicada por una app que ofrece opciones para sus entradas, when la persona mantiene pulsado OK sobre ella, then se abren las opciones de esa app para esa entrada y la app recibe la posición en pantalla de la tarjeta enfocada

#### Scenario: App sin opciones
- Given una tarjeta publicada por una app que no ofrece opciones, when la persona mantiene pulsado OK sobre ella, then no se abre nada, la tarjeta sigue en la fila y conserva el foco

#### Scenario: Pulsación corta
- Given el foco en una tarjeta de una app que ofrece opciones, when la persona pulsa OK sin mantenerlo, then se abre el destino de la entrada como hasta ahora y no se abren las opciones
