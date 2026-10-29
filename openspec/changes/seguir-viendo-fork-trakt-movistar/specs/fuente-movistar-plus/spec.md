## Purpose

Obtener de la cuenta de Movistar Plus de la persona sus últimas reproducciones, que la app oficial no publica en Watch Next, para mostrarlas en "Seguir viendo" y abrirlas en la app oficial.

## ADDED Requirements

### Requirement: Conexión con usuario y clave
El sistema SHALL conectar la cuenta de Movistar Plus con el usuario y la clave introducidos una vez en la TV, y SHALL mostrar el error del servicio cuando la conexión falle.

#### Scenario: Conexión correcta
- Given la fuente Movistar Plus sin conectar, when la persona introduce un usuario y una clave válidos, then el add-on muestra la cuenta y el perfil como conectados

#### Scenario: Clave incorrecta
- Given la fuente sin conectar, when la persona introduce una clave incorrecta, then el add-on indica que la clave no es válida y no guarda nada

### Requirement: Sin ocupar plazas de dispositivo sin aviso
El sistema SHALL NOT dar de alta un dispositivo en la cuenta de Movistar Plus sin avisar antes a la persona y obtener su confirmación.

#### Scenario: Alta necesaria
- Given que leer las reproducciones exige un dispositivo registrado, when la persona conecta la fuente, then el add-on explica que ocupará una plaza de dispositivo y solo la ocupa si la persona acepta

### Requirement: Perfil seleccionado
El sistema SHALL leer las reproducciones del perfil de Movistar Plus elegido por la persona, el principal por defecto.

#### Scenario: Cambio de perfil
- Given una cuenta con dos perfiles, when la persona elige el segundo, then la siguiente sincronización publica solo las reproducciones de ese perfil

### Requirement: Últimas reproducciones
El sistema SHALL convertir cada elemento de las últimas reproducciones de Movistar Plus en un elemento "continuar", con su progreso cuando el servicio lo aporte.

#### Scenario: Serie a medias
- Given un episodio visto a medias en la app oficial, when se sincroniza la fuente, then aparece un elemento "continuar" de ese episodio

### Requirement: Sesión caducada
El sistema SHALL renovar la sesión de Movistar Plus sin intervención mientras el servicio lo permita, y SHALL pedir reconectar la cuenta cuando no pueda.

#### Scenario: Clave cambiada
- Given la persona cambia su clave de Movistar Plus, when se sincroniza, then la fuente pasa a "reconexión necesaria" y conserva sus entradas anteriores

### Requirement: Apertura en la app oficial
El sistema SHALL abrir los elementos de Movistar Plus en la app oficial instalada en la TV.

#### Scenario: Abrir elemento
- Given un elemento de Movistar Plus publicado, when la persona lo abre desde el launcher, then se abre la app oficial de Movistar Plus
