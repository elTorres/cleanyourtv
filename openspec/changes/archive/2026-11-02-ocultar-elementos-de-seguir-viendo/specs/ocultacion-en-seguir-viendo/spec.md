## Purpose

Que la persona pueda dejar de ver en "Seguir viendo" un episodio, una temporada, una serie o una película que publica el add-on, sin tocar su cuenta de Trakt ni de Movistar Plus, y deshacerlo desde los ajustes del add-on.

## ADDED Requirements

### Requirement: Menú de opciones de una entrada
El sistema SHALL mostrar, cuando un launcher le pide las opciones de una de sus entradas, un menú con las opciones de ocultar ese episodio, su temporada y la serie, o una única opción para no mostrarla más cuando la entrada no es un episodio, y SHALL cerrarlo sin cambios cuando la persona pulsa Atrás.

#### Scenario: Episodio con temporada
- Given una entrada del episodio 5 de la temporada 2 de una serie, when se abren sus opciones, then el menú ofrece "Ocultar este episodio", "Ocultar la temporada 2" y "Ocultar la serie"

#### Scenario: Episodio sin temporada
- Given una entrada con número de episodio pero sin temporada, when se abren sus opciones, then el menú ofrece "Ocultar este episodio" y "Ocultar la serie", sin opción de temporada

#### Scenario: Película
- Given una entrada de una película, when se abren sus opciones, then el menú ofrece solo "No mostrar más"

#### Scenario: Cerrar sin elegir
- Given el menú abierto, when la persona pulsa Atrás, then el menú se cierra, no se oculta nada y la entrada sigue publicada

#### Scenario: Entrada ya sustituida
- Given una entrada que una sincronización ha sustituido después de dibujarse la fila, when se piden sus opciones, then el add-on indica que la entrada ya no está y se cierra sin ocultar nada

### Requirement: Retirada inmediata
El sistema SHALL quitar de Watch Next, en cuanto la persona elige una opción, todas las entradas publicadas a las que alcanza la ocultación.

#### Scenario: Ocultar una serie con dos entradas
- Given dos entradas publicadas de la misma serie y fuente, when la persona elige "Ocultar la serie" en una de ellas, then las dos dejan de estar en Watch Next y no aparecen en la fila al volver a Home

### Requirement: Alcance de cada ocultación
El sistema SHALL ocultar con "Ocultar este episodio" solo ese episodio, con "Ocultar la temporada" todos los episodios de esa temporada de esa serie, y con "Ocultar la serie" o "No mostrar más" todas las entradas de esa serie o película, siempre dentro de la fuente que publicó la entrada.

#### Scenario: Siguiente episodio tras ocultar uno
- Given el episodio 3 de una serie oculto, when una sincronización devuelve el episodio 4 de esa serie, then se publica la entrada del episodio 4

#### Scenario: Otra temporada tras ocultar una
- Given la temporada 1 de una serie oculta, when una sincronización devuelve un episodio de la temporada 2, then se publica la entrada de ese episodio

#### Scenario: Serie oculta
- Given una serie oculta, when una sincronización devuelve cualquier episodio de esa serie, then no se publica

#### Scenario: Misma serie en otra fuente
- Given una serie oculta en las entradas de Trakt, when la sincronización de Movistar Plus devuelve un episodio de una serie con el mismo título, then se publica la entrada de Movistar Plus

### Requirement: Ocultación duradera
El sistema SHALL conservar los elementos ocultos en las sincronizaciones siguientes y al reiniciar la TV.

#### Scenario: La fuente vuelve a devolver el elemento
- Given un episodio oculto, when la siguiente sincronización lo devuelve de nuevo, then no se publica

#### Scenario: Reinicio de la TV
- Given una serie oculta, when la TV se reinicia y se sincroniza al arrancar, then la serie sigue sin publicarse

### Requirement: Restablecer los elementos ocultos
El sistema SHALL ofrecer en los ajustes del add-on una opción que indique cuántos elementos hay ocultos y que, tras pedir confirmación, los vuelva a mostrar todos y sincronice las fuentes conectadas.

#### Scenario: Restablecer
- Given dos elementos ocultos, when la persona elige la opción en los ajustes del add-on y la confirma, then la lista de ocultos queda vacía y, al terminar la sincronización, se publican de nuevo las entradas de esos elementos que las fuentes sigan devolviendo

#### Scenario: Cancelar
- Given dos elementos ocultos, when la persona elige la opción y cancela la confirmación, then los dos siguen ocultos

#### Scenario: Ninguno oculto
- Given ningún elemento oculto, when la persona abre los ajustes del add-on, then la opción indica que no hay elementos ocultos y no pide confirmación al elegirla

### Requirement: Ocultación sin cambios en los servicios
El sistema SHALL NOT modificar nada en Trakt ni en Movistar Plus al ocultar o restablecer elementos.

#### Scenario: Pausa en Trakt
- Given un episodio pausado en Trakt y oculto desde la fila, when la persona abre Trakt o Kodi en otro dispositivo, then el episodio sigue pausado allí
