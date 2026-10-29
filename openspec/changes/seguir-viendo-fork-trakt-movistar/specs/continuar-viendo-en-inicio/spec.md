## Purpose

Mostrar en la pantalla de inicio del launcher una fila "Seguir viendo" con lo que la persona ha dejado a medias o tiene pendiente, leída de la tabla Continue watching (Watch Next) del sistema, sin que el launcher acceda a la red.

## ADDED Requirements

### Requirement: Fila desactivada por defecto
El sistema SHALL tomar de `continue_watching` en `config.xml` el valor por defecto de la fila "Seguir viendo", desactivado, y SHALL comportarse como el launcher original mientras esté desactivada.

#### Scenario: Interruptor desactivado
- Given `continue_watching` desactivado en `config.xml` y ninguna elección en la TV, when la persona vuelve a Home, then no aparece ninguna fila nueva y el dock y el reloj ocupan su sitio habitual

### Requirement: Interruptor en los ajustes del launcher
El sistema SHALL ofrecer en los ajustes del launcher un interruptor "Seguir viendo" que active o desactive la fila en la TV sin reinstalar, cuya elección prevalece sobre `config.xml` y se conserva al cambiar de tema.

#### Scenario: Activar desde los ajustes
- Given la fila desactivada y filas publicadas en Watch Next, when la persona activa "Seguir viendo" en los ajustes del launcher y vuelve a Home, then aparece la fila "Seguir viendo" entre el reloj y el dock

#### Scenario: Desactivar desde los ajustes
- Given la fila visible, when la persona desactiva "Seguir viendo" en los ajustes del launcher y vuelve a Home, then la fila desaparece

#### Scenario: Cambio de tema
- Given "Seguir viendo" activado en los ajustes, when se envía otro tema a la TV, then la fila sigue activada

### Requirement: Lectura al volver a Home
El sistema SHALL leer la tabla Watch Next cada vez que Home vuelve a primer plano y SHALL NOT consultarla con temporizadores ni servicios en segundo plano.

#### Scenario: Nuevas filas tras ver un episodio
- Given una app ha publicado una fila nueva mientras la persona estaba en otra app, when la persona pulsa Home, then la fila nueva aparece en "Seguir viendo"

### Requirement: Fila oculta sin permiso o sin contenido
El sistema SHALL ocultar la fila cuando no tenga concedido el permiso de lectura de Watch Next o cuando no quede ninguna entrada tras filtrar.

#### Scenario: Permiso no concedido
- Given `continue_watching` activado y el permiso de lectura no concedido, when la persona vuelve a Home, then la fila no aparece y Home funciona con normalidad

#### Scenario: Tabla vacía
- Given ninguna app ha publicado filas visibles, when la persona vuelve a Home, then la fila no aparece

### Requirement: Apps permitidas
El sistema SHALL mostrar solo las entradas publicadas por las apps de la lista `continue_watching_apps` de `config.xml` cuando la lista no esté vacía, y las de todas las apps cuando esté vacía.

#### Scenario: Publicidad de otra app
- Given `continue_watching_apps` con solo el add-on y una app del firmware que publica entradas más recientes, when se construye la fila, then solo aparecen las entradas del add-on

#### Scenario: Lista vacía
- Given `continue_watching_apps` vacía, when se construye la fila, then aparecen las entradas de todas las apps

### Requirement: Filtrado y orden de entradas
El sistema SHALL mostrar como máximo diez entradas, una por serie o película, ordenadas de la más reciente a la más antigua, y SHALL descartar las entradas sin actividad en los últimos 60 días y las de tipo "continuar" con más del 95 % visto.

#### Scenario: Entrada antigua
- Given una entrada con última actividad hace 90 días, when se construye la fila, then esa entrada no aparece

#### Scenario: Episodio casi terminado
- Given una entrada "continuar" al 99 % de su duración, when se construye la fila, then esa entrada no aparece

#### Scenario: Varios episodios de la misma serie
- Given dos entradas de la misma serie, when se construye la fila, then solo aparece la más reciente

### Requirement: Contenido de cada tarjeta
El sistema SHALL mostrar en cada tarjeta el póster cuando sea una imagen local, o el banner de la app que publicó la entrada en su lugar, junto con el título, la temporada y el episodio cuando existan, y una barra de progreso cuando la entrada tenga posición y duración.

#### Scenario: Póster local
- Given una entrada con póster `content://`, when se dibuja la tarjeta, then se muestra ese póster

#### Scenario: Póster remoto
- Given una entrada con póster `https://`, when se dibuja la tarjeta, then se muestra el banner de la app que la publicó y el launcher no accede a la red

### Requirement: Apertura de una entrada
El sistema SHALL abrir el destino de la entrada solo si pertenece a la app que la publicó, y SHALL abrir esa app en su pantalla principal en caso contrario.

#### Scenario: Destino de la propia app
- Given una entrada cuyo destino pertenece a la app que la publicó, when la persona pulsa OK sobre la tarjeta, then se abre ese destino

#### Scenario: Destino de otra app
- Given una entrada cuyo destino pertenece a una app distinta de la que la publicó, when la persona pulsa OK sobre la tarjeta, then se abre la app que la publicó

### Requirement: Navegación con el mando
El sistema SHALL permitir llegar a la fila y salir de ella con el D-pad desde el reloj y desde el dock, sin dejar el foco atrapado.

#### Scenario: Del dock a la fila y vuelta
- Given la fila visible y el foco en el dock, when la persona pulsa Arriba y después Abajo, then el foco pasa a la fila y vuelve al dock
