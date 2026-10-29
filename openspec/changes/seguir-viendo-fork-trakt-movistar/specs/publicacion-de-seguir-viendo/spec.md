## Purpose

Que un add-on con acceso a la red convierta la actividad de visionado de servicios externos en entradas de Continue watching (Watch Next) del sistema, de modo que cualquier launcher que lea esa tabla las muestre, con su póster y abriendo la app adecuada.

## ADDED Requirements

### Requirement: Publicación como entradas propias
El sistema SHALL publicar cada elemento de visionado como una entrada propia de Watch Next marcada como visible para otras apps, con título, temporada, episodio, posición, duración, última actividad y tipo (continuar o siguiente) cuando la fuente los aporte.

#### Scenario: Episodio a medias
- Given una fuente conectada devuelve un episodio a medias, when termina la sincronización, then existe una entrada "continuar" con su posición y duración que un launcher con permiso de lectura puede ver

### Requirement: Sustitución por fuente
El sistema SHALL sustituir en cada sincronización todas las entradas de una fuente por las que esa fuente devuelve, sin tocar las entradas de otras fuentes ni las de otras apps.

#### Scenario: Elemento terminado en otro dispositivo
- Given una entrada publicada de la fuente Trakt, when la siguiente sincronización de Trakt ya no la devuelve, then la entrada desaparece de Watch Next

#### Scenario: Fallo de una fuente
- Given dos fuentes conectadas, when una falla por red, then se conservan sus entradas anteriores y las de la otra fuente se actualizan

### Requirement: Pósters disponibles sin red
El sistema SHALL ofrecer cada póster como una imagen local legible por el launcher sin acceso a la red, y SHALL publicar la entrada sin póster cuando no pueda obtenerlo.

#### Scenario: Póster descargado
- Given un elemento con imagen disponible, when se publica su entrada, then el póster de la entrada es una dirección `content://` del add-on que devuelve esa imagen

#### Scenario: Sin imagen
- Given un elemento sin imagen disponible, when se publica su entrada, then la entrada se publica sin póster

### Requirement: Apertura a través del add-on
El sistema SHALL hacer que cada entrada abra el add-on, y que este abra el contenido en la app configurada para esa fuente, o esa app en su pantalla principal cuando no sepa abrir el contenido concreto.

#### Scenario: Contenido abrible
- Given una entrada de Trakt de una serie que Stremio sabe abrir, when la persona la abre desde el launcher, then Stremio se abre en ese contenido

#### Scenario: App de destino no instalada
- Given la app de destino de la fuente no está instalada, when la persona abre la entrada, then el add-on muestra un mensaje que lo indica y no se cierra sin más

### Requirement: Sincronización periódica y bajo demanda
El sistema SHALL sincronizar las fuentes conectadas al arrancar la TV, periódicamente solo con red disponible, y cuando la persona lo pida desde el add-on.

#### Scenario: Sincronizar ahora
- Given una fuente conectada, when la persona elige "Sincronizar ahora", then las entradas de esa fuente se actualizan y el add-on muestra la hora de la última sincronización

#### Scenario: Sin red
- Given la TV sin red, when llega la sincronización periódica, then no se ejecuta hasta que haya red

### Requirement: Credenciales protegidas
El sistema SHALL guardar los tokens y credenciales solo en el almacenamiento privado del add-on, cifrados con una clave del sistema, y SHALL NOT exponerlos a otras apps ni escribirlos en los registros.

#### Scenario: Registros del sistema
- Given una fuente conectada, when se sincroniza, then ningún token, usuario ni clave aparece en logcat

### Requirement: Desconexión de una fuente
El sistema SHALL permitir desconectar cada fuente, borrando sus credenciales y todas sus entradas publicadas.

#### Scenario: Desconectar
- Given la fuente Trakt conectada con entradas publicadas, when la persona elige desconectarla, then sus credenciales se borran y sus entradas desaparecen de Watch Next
