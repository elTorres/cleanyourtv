## ADDED Requirements

### Requirement: Opciones de la app con pulsación larga
El sistema SHALL abrir, cuando la persona mantiene pulsado OK sobre una tarjeta de "Seguir viendo", las opciones que ofrece para esa entrada la app que la publicó, y SHALL NOT hacer nada cuando esa app no ofrece opciones.

#### Scenario: App con opciones
- Given una tarjeta publicada por una app que ofrece opciones para sus entradas, when la persona mantiene pulsado OK sobre ella, then se abren las opciones de esa app para esa entrada

#### Scenario: App sin opciones
- Given una tarjeta publicada por una app que no ofrece opciones, when la persona mantiene pulsado OK sobre ella, then no se abre nada, la tarjeta sigue en la fila y conserva el foco

#### Scenario: Pulsación corta
- Given el foco en una tarjeta de una app que ofrece opciones, when la persona pulsa OK sin mantenerlo, then se abre el destino de la entrada como hasta ahora y no se abren las opciones

### Requirement: Foco cuando desaparece la tarjeta enfocada
El sistema SHALL dejar el foco, cuando la tarjeta enfocada ya no está al volver a Home, en la tarjeta que ocupa su posición o en la anterior si era la última, y SHALL pasarlo al dock si la fila queda vacía.

#### Scenario: Quedan tarjetas
- Given una fila con tres tarjetas y el foco en la segunda, when su entrada deja de estar publicada y la persona vuelve a Home, then la fila muestra las otras dos y el foco queda en una de ellas

#### Scenario: Última tarjeta
- Given una fila con una sola tarjeta enfocada, when su entrada deja de estar publicada y la persona vuelve a Home, then la fila desaparece y el foco pasa al dock
