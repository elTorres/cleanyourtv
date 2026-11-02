## ADDED Requirements

### Requirement: Aspecto del menú de opciones
El sistema SHALL mostrar el menú de opciones de una entrada con el mismo aspecto que el menú de las tarjetas de apps del launcher: un panel oscuro de esquinas redondeadas, con una fila por opción que se resalta en blanco al recibir el foco, la tipografía del launcher y sin título.

#### Scenario: Menú abierto
- Given una entrada de un episodio, when se abren sus opciones, then el menú muestra solo las filas de las opciones, sin título, con el mismo panel, filas, resaltado y tipografía que el menú de una tarjeta de app

### Requirement: Menú junto a la tarjeta
El sistema SHALL colocar el menú, cuando el launcher indica la posición de la tarjeta, justo debajo de ella y alineado con su borde inicial, o con su borde final cuando alineado con el inicial no cabe, encima de ella cuando debajo no cabe, siempre dentro de la pantalla, y SHALL centrarlo en la pantalla cuando no recibe posición.

#### Scenario: Cabe debajo
- Given una tarjeta con espacio libre debajo, when se abren sus opciones, then el menú aparece justo debajo de la tarjeta, alineado con su borde inicial, desplegándose hacia abajo

#### Scenario: No cabe debajo
- Given una tarjeta sin espacio suficiente debajo para el menú, when se abren sus opciones, then el menú aparece justo encima de la tarjeta, desplegándose hacia arriba

#### Scenario: Tarjeta junto al borde
- Given una tarjeta cerca del borde final de la pantalla, when se abren sus opciones, then el menú se alinea con el borde final de la tarjeta y queda entero dentro de la pantalla

#### Scenario: Sin posición
- Given una petición de opciones que no indica la posición de la tarjeta, when se abren las opciones, then el menú aparece centrado en la pantalla con el mismo aspecto
