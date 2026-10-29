## Purpose

Obtener de la cuenta de Trakt de la persona lo que tiene a medias y el siguiente episodio de las series que sigue, venga de Stremio en la TV o de Kodi en otro dispositivo, para publicarlo en "Seguir viendo".

## ADDED Requirements

### Requirement: Conexión con código de dispositivo
El sistema SHALL conectar la cuenta de Trakt mostrando en la TV un código y la dirección de activación, sin pedir usuario ni clave con el mando, y SHALL quedar conectado cuando la persona introduzca el código desde otro dispositivo.

#### Scenario: Activación completada
- Given la fuente Trakt sin conectar, when la persona elige conectar e introduce el código mostrado en trakt.tv/activate, then el add-on muestra la cuenta como conectada

#### Scenario: Código caducado
- Given un código mostrado, when caduca sin activarse, then el add-on lo indica y ofrece pedir uno nuevo

### Requirement: Credenciales de la aplicación en la compilación
El sistema SHALL tomar el identificador, la URL de redirección y, si existe, el secreto de la aplicación de Trakt de la configuración local de compilación, que no se versiona; SHALL funcionar sin secreto, como las aplicaciones PKCE que Trakt crea ahora, y SHALL desactivar la fuente Trakt cuando falte el identificador.

#### Scenario: Compilación sin credenciales
- Given una compilación sin identificador de Trakt, when la persona abre el add-on, then la fuente Trakt aparece como no disponible con un mensaje que explica cómo configurarla

### Requirement: Renovación de la sesión
El sistema SHALL renovar el token de Trakt antes de que caduque y SHALL pedir reconectar la cuenta solo cuando la renovación sea rechazada.

#### Scenario: Token rechazado
- Given un token revocado desde trakt.tv, when se sincroniza, then la fuente pasa a "reconexión necesaria" y conserva sus entradas anteriores hasta que se reconecte o se desconecte

### Requirement: Elementos a medias
El sistema SHALL convertir cada película o episodio que Trakt tenga en reproducción pausada en un elemento "continuar", con su progreso y su fecha de pausa como última actividad.

#### Scenario: Episodio pausado en Kodi
- Given un episodio pausado al 40 % en Kodi de otro dispositivo y sincronizado con Trakt, when se sincroniza la fuente, then aparece un elemento "continuar" de ese episodio al 40 %

### Requirement: Siguiente episodio
El sistema SHALL convertir el siguiente episodio sin ver de cada serie con actividad reciente en un elemento "siguiente", salvo que ya exista un elemento "continuar" de esa serie.

#### Scenario: Episodio terminado
- Given la persona termina el episodio 3 de una serie en Stremio, when se sincroniza la fuente, then aparece un elemento "siguiente" con el episodio 4

#### Scenario: Serie con episodio a medias
- Given una serie con un episodio pausado, when se sincroniza la fuente, then solo aparece el elemento "continuar" de esa serie

### Requirement: App de destino configurable
El sistema SHALL abrir los elementos de Trakt en la app de destino elegida por la persona, Stremio por defecto.

#### Scenario: Destino por defecto
- Given la configuración por defecto, when la persona abre un elemento de Trakt, then se abre Stremio
