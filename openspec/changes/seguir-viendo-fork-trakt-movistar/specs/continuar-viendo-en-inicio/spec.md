## Purpose

Mostrar en la pantalla de inicio del launcher una fila "Seguir viendo" con lo que la persona ha dejado a medias o tiene pendiente, leída de la tabla Continue watching (Watch Next) del sistema, sin que el launcher acceda a la red.

## ADDED Requirements

### Requirement: Fila desactivada por defecto
El sistema SHALL mostrar la fila "Seguir viendo" solo cuando el interruptor `continue_watching` esté activado en `config.xml` o en el tema, y SHALL comportarse como el launcher original cuando esté desactivado.

#### Scenario: Interruptor desactivado
- Given `continue_watching` desactivado, when la persona vuelve a Home, then no aparece ninguna fila nueva y el dock y el reloj ocupan su sitio habitual

#### Scenario: Interruptor activado
- Given `continue_watching` activado y filas publicadas en Watch Next, when la persona vuelve a Home, then aparece la fila "Seguir viendo" entre el reloj y el dock

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
