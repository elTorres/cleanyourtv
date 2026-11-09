# reconexion-de-mandos Specification

## Purpose

Que los mandos Bluetooth que la TV deja de reconocer tras cada apagado (los que entregan una clave de privacidad de ejemplo y se anuncian con su dirección pública, como el BT_RC833_U1 y el TCL_RC902V_M1 en la TCL C725) vuelvan a funcionar por Bluetooth en cuanto la persona los usa, sin repetir la combinación de emparejamiento. Con un escaneo activo la propia TV a veces los vuelve a conectar; si no, la app los desvincula y los vuelve a vincular (probado en la TCL C725 el 2026-11-09).

## Requirements

### Requirement: Mandos elegidos por la persona
El sistema SHALL actuar solo sobre los mandos que la persona haya marcado en la pantalla de la app, elegidos entre los dispositivos Bluetooth de baja energía vinculados a la TV, y SHALL recordar esa lista aunque un mando deje de estar vinculado.

#### Scenario: Marcar un mando
- Given el BT_RC833_U1 vinculado a la TV, when la persona lo marca en la pantalla de la app, then el mando aparece en la lista de mandos a reconectar con su nombre y su dirección

#### Scenario: Mando no marcado
- Given un mando vinculado que la persona no ha marcado, when se desconecta, then la app no lo desvincula ni lo vuelve a vincular

### Requirement: Reconexión al usar el mando
El sistema SHALL, mientras espera, mantener la TV escuchando los anuncios de los mandos marcados para que el mando quede conectado poco después de que la persona pulse una tecla, sin pedir la combinación de emparejamiento.

#### Scenario: Primera pulsación tras encender
- Given la TV recién encendida con el RC833 marcado, vinculado y sin conexión y la app cerrada, when la persona pulsa una tecla del RC833, then en menos de 30 s el mando queda conectado y responde por Bluetooth

#### Scenario: Mando sin usar
- Given un mando marcado y sin conexión que nadie pulsa, when pasa el tiempo, then la app no lo desvincula

### Requirement: Respaldo con nueva vinculación
El sistema SHALL, si un mando marcado vinculado y sin conexión sigue anunciándose 10 s después de su primer anuncio sin que la TV lo conecte, desvincularlo y volver a vincularlo en ese momento, sin pedir la combinación de emparejamiento.

#### Scenario: La TV no lo conecta
- Given el RC902 marcado, vinculado y sin conexión, when la persona sigue pulsando sus teclas y pasan 10 s desde su primer anuncio sin conexión, then la app lo desvincula y lo vuelve a vincular y el mando queda conectado

#### Scenario: La TV lo conecta antes
- Given el RC902 marcado y anunciándose, when la TV lo conecta antes de 10 s, then la app no lo desvincula

### Requirement: Momentos en que se vigila
El sistema SHALL empezar a esperar a los mandos marcados al encender la TV, cuando un mando marcado se desconecte y al abrir la app, siempre que ningún mando marcado esté conectado, y SHALL dejar de esperar en cuanto cualquiera de los mandos marcados esté conectado.

#### Scenario: Uno conectado
- Given el RC902 y el RC833 marcados y sin conexión, when el RC833 queda conectado, then la app deja de esperar anuncios, también los del RC902

#### Scenario: Desconexión con la TV encendida
- Given el RC833 marcado y conectado y ningún otro mando marcado conectado, when el RC833 se desconecta sin que la TV se apague, then la app vuelve a esperar los anuncios de los mandos marcados

#### Scenario: Otro mando marcado sigue conectado
- Given el RC902 y el RC833 marcados y conectados, when el RC833 se desconecta, then la app no empieza a esperar

### Requirement: Espera limitada
El sistema SHALL dejar de esperar 10 minutos después de empezar una espera si ningún mando marcado se ha conectado, y SHALL empezar una espera nueva de 10 minutos cada vez que la persona abre la app.

#### Scenario: Mando sin pilas
- Given la TV recién encendida con el RC833 marcado, vinculado y sin pilas, when pasan 10 minutos sin que se conecte ningún mando marcado, then la app deja de escuchar

#### Scenario: Volver a esperar
- Given la espera terminada sin conexión y el RC833 otra vez con pilas, when la persona abre la app y pulsa una tecla del RC833, then el mando queda conectado en unos segundos

### Requirement: Sin proceso permanente
El sistema SHALL esperar los anuncios con un escaneo que conserva el propio sistema, sin mantener un servicio propio en marcha ni una notificación permanente, y SHALL usar un servicio en primer plano solo mientras dura una reconexión de respaldo.

#### Scenario: Espera tras el arranque
- Given la TV encendida y un mando marcado sin conexión, when nadie lo pulsa, then la app no tiene ningún servicio en marcha mientras espera

### Requirement: Reintento sin dejar el mando suelto
El sistema SHALL, si la vinculación de respaldo falla después de desvincular un mando, volver a intentarla con el siguiente anuncio de ese mando mientras siga esperando, y SHALL incluir ese mando en la siguiente espera si se deja de esperar antes de que quede conectado.

#### Scenario: Vinculación fallida
- Given la app ha desvinculado el RC902 y la vinculación falla porque el mando dejó de anunciarse, when la persona vuelve a pulsar una tecla del RC902, then la app lo vincula sin volver a desvincularlo

#### Scenario: Otro mando conectado antes
- Given la app ha desvinculado el RC902 y la vinculación ha fallado, when el RC833 queda conectado y más tarde empieza una nueva espera, then la app vincula el RC902 con su primer anuncio de esa espera

### Requirement: Estado visible
El sistema SHALL mostrar en su pantalla, para cada mando marcado, si está conectado, esperando una pulsación o reconectándose, la hora de su último anuncio visto y el resultado y la hora de su última reconexión de respaldo.

#### Scenario: Consultar el estado
- Given el RC902 se reconectó a las 18:41, when la persona abre la app, then ve el RC902 como conectado con la reconexión de las 18:41 correcta, aunque la pantalla estuviera cerrada al reconectarse

### Requirement: Requisitos que faltan
El sistema SHALL indicar en su pantalla qué le impide esperar a los mandos (permiso de ubicación sin conceder, ubicación desactivada o Bluetooth apagado) y cómo resolverlo, y SHALL no desvincular ningún mando mientras falte alguno. El sistema SHALL indicar también si falta el permiso de ubicación todo el tiempo, sin el que solo puede reconectar con la pantalla abierta.

#### Scenario: Ubicación solo con la app abierta
- Given el permiso de ubicación concedido solo mientras se usa la app, when la persona abre la app, then la pantalla pide permitir la ubicación todo el tiempo y explica que sin ella no reconecta tras encender la TV con la app cerrada

#### Scenario: Ubicación desactivada
- Given la ubicación de la TV desactivada, when la persona abre la app, then la pantalla explica que hace falta activarla para detectar los mandos, no se escucha ningún anuncio y no se desvincula ninguno

### Requirement: Sin red
El sistema SHALL funcionar sin acceso a la red y SHALL no enviar datos fuera de la TV.

#### Scenario: Instalación
- Given la app instalada, when se revisan sus permisos, then no incluye acceso a Internet
