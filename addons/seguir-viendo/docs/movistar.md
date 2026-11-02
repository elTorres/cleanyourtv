# Movistar Plus: resultados del spike

Notas para la fuente Movistar Plus de `seguir-viendo`. Salen de la app oficial instalada en la TCL C725, de su
interfaz web pública y del directorio de servicios público. No incluyen datos de ninguna cuenta.

## App oficial

| Dato | Valor |
|---|---|
| Paquete | `com.movistarplus.androidtv` |
| Versión probada | 26.06.103 (versionCode 144), `minSdk` 24 |
| Actividad | `com.movistarplus.androidtv/.MainActivity` |
| Esquema declarado | `appmovistar://movistarplus.es`, con `VIEW`, `DEFAULT` y `BROWSABLE`, sin ruta fija |

La app es un contenedor Cordova que carga su interfaz desde
`https://vodclientui.emisiondof6.com/prod/apps/android.tv/nux_26.06.100/index.html`. El deep link llega a esa
interfaz, que acepta rutas que contengan `ficha` o `play` y reenvía sus parámetros a su endpoint `deeplink`.

## Deep link

```
appmovistar://movistarplus.es/ficha?tipo=E&id=<id de contenido>
```

Comprobado con `am start -a android.intent.action.VIEW -d '<enlace>' com.movistarplus.androidtv` y un id del
catálogo público: abre la ficha de la serie. En arranque en frío la app enseña antes el selector de perfil, que se
cierra solo a los 10 s. Con `play` en lugar de `ficha` la app arranca la reproducción.

## Directorio de servicios

La app resuelve todos sus endpoints con un directorio JSON público:

```
https://sd.cdn.sve.video.telefonicaservices.com/VoD/vod.svc/android.tv/environments/prod_2606/ws-directory
```

`prod_2606` cambia con cada versión de la interfaz (`DS_NAME` en su bundle), así que el add-on debe leer el
directorio en lugar de fijar las URL.

| Uso | Servicio y endpoint | Host |
|---|---|---|
| Pedir código de dispositivo | `movistarplus/autenticacion` `device_authorization`, `POST /ctv/connect/deviceauthorization` | `oidc_fau_easy` |
| Canjear el código | `movistarplus/autenticacion` `token_dc`, `POST /ctv/connect/token`, `grant_type=urn:ietf:params:oauth:grant-type:device_code` | `oidc_fau_easy` |
| Login con usuario y clave | `movistarplus/autenticacion` `token`, `POST /connect/token`, `grant_type=password` | `oidc_fau` |
| Datos de la cuenta | `movistarplus/autenticacion` `account_info`, `GET /{deviceType}/accountInfo` | `login_fau` |
| Inicio de sesión del dispositivo | `movistarplus/initdata` `initdata`, `POST /initData` | `login_fau` |
| Alta de dispositivo | `movistarplus/devices` `activacion_dispositivo_cuenta_tk`, `POST /devices` | `login_fau` |
| Perfiles | `movistarplus/cuenta` `listaperfiles`, `GET /profiles` | `profile_fau` |
| Últimas reproducciones | `movistarplus/consultas` `ultimasreproducciones`, `GET .../profiles/{profileID}/contents/watching?container=trackedseries` | `series_fau` |
| Siguiente episodio | `movistarplus/consultas` `siguiente_episodio`, `GET /{deviceType}/episodes/{contentId}/next` | `series_fau_cdn` |

La app de TV no pide usuario y clave: usa el flujo de código de dispositivo (RFC 8628). Pide un código con
`device_id` y `device_type`, enseña `user_code` y `verification_uri`, y consulta `token_dc` hasta que la persona
lo activa desde otro dispositivo.

## Firma de las peticiones

`POST /ctv/connect/deviceauthorization` con los parámetros del directorio y `Authorization: Basic` de
`deviceId:deviceType` responde 401. La librería de la interfaz no manda esa cabecera tal cual: la sustituye por
`OPPlus <tipo>=<token>,Signature=<firma>`, calculada a partir del método, la URL y unas claves de cliente que
lleva ofuscadas en el bundle (`ua.query`, `ua.param`, `ua.jswbw`). Es una autenticación del cliente pensada para
que solo la app oficial use la API.

El add-on no reproduce esa firma: haría falta sacar y usar las claves ofuscadas de la app oficial. Por eso
quedan sin comprobar:

- Si `ultimasreproducciones` responde sin alta de dispositivo.
- Si el token trae `refresh_token` y qué caducidad tiene.

Tampoco se puede aprovechar el registro de la app oficial en la TV: su token vive en su almacenamiento privado,
que Android solo deja leer con root.

## Ruta del reproductor web (Paco8)

`Paco8/plugin.video.movistarplus` (GPL-2.0, revisado en `6511fa8`, octubre de 2026) no usa la API de la app de TV
sino la del reproductor web `ver.movistarplus.es`, que no firma las peticiones. Se usa solo como referencia del
protocolo:

| Paso | Petición |
|---|---|
| Login | `POST https://auth.dof6.com/auth/oauth2/token?deviceClass=<tipo>`, formulario con `grant_type=password`, `username` y `password`. Devuelve `access_token` (JWT con `exp`), sin `refresh_token`. |
| Cuenta | `GET https://auth.dof6.com/movistarplus/api/devices/<tipo>/users/authenticate` con `Bearer`. De ahí salen `ofertas[0].accountNumber`, `ofertas[0].@id_perfil` y `cod_usuario_cifrado`. |
| Dispositivo | `POST https://auth.dof6.com/movistarplus/<tipo>/accounts/<cuenta>/devices/?qspVersion=ssp` da un id de dispositivo nuevo, y `POST .../devices/<id>` lo da de alta. Paco8 lo hace solo si `initData` falla, y tiene una opción para reutilizar un dispositivo ya registrado. |
| Sesión | `POST https://clientservices.dof6.com/movistarplus/<tipo>/sdp/mediaPlayers/<id>/initData` con `Bearer` y `X-Movistarplus-Deviceid` da el `token` de sesión (`X-HZId`) y la `demarcation`. |
| Perfiles | `GET https://grmovistar.imagenio.telefonica.net/asfe/rest/users/profiles?state=0&isForKids=0` con `X-HZId`. |
| Reproducciones | `GET https://perso.dof6.com/movistarplus/<tipo>/users/<cod_usuario_cifrado>/viewings?profile=<@id_perfil>&container=trackedseries&accountNumber=<cuenta>&...` |

Cuando el `access_token` caduca, Paco8 no lo renueva: avisa y pide volver a entrar, y puede guardar usuario y
clave para rellenar el formulario.

Comprobado sin cuenta: `POST /auth/oauth2/token` con un usuario inventado responde `401 {"error":"invalid_grant"}`,
el rechazo normal de OAuth y no el de una firma, así que esta ruta sigue abierta.

Comprobado con una cuenta real (noviembre de 2026):

- El login responde 200 con `access_token`, `token_type` y `expires_in`, sin `refresh_token`. El JWT dura 240 h
  (10 días) y lleva `accountNumber` entre sus claims.
- La cuenta y la lista de dispositivos responden con el `Bearer`.
- `viewings` responde 401 solo con el `Bearer`: hace falta además `X-HZId`, el token de sesión de `initData`, que
  exige un id de dispositivo registrado en la cuenta.
- `initData` responde 200 con el id de un dispositivo que ya estaba registrado (tipo `AMZTV`), sin dar de alta
  ninguno. Devuelve `token` (el `X-HZId`), `demarcation`, `pid` y `cod_usuario_cifrado`, entre otros.
- Los perfiles responden con `X-HZId`: `id`, `name`, `isForKids`, `typeID`, `imageID`.
- `viewings` con `Bearer`, `X-HZId` y `Accept: application/vnd.miviewtv.v1+json` responde 200. Cada elemento de
  `Contenidos` trae `DatosEditoriales` (`Id`, `TipoContenido`, `IdSerie`, `TituloSerie`, `TituloEpisodio`,
  `NumeroEpisodio`, `Imagenes`, `DuracionEnSegundos`) y `Bookmark` (`contentId`, `offset`, `duration`, `seen`).

## Conclusiones para el add-on

- Login con usuario y clave en `auth.dof6.com`. Como no hay `refresh_token` y el token dura 10 días, la clave se
  guarda cifrada en `SecretStore` para volver a entrar sin la persona; si el login falla, la fuente pasa a
  "reconexión necesaria".
- El add-on reutiliza un dispositivo ya registrado en la cuenta, elegido por la persona, en lugar de ocupar una
  plaza. Solo da de alta uno nuevo si no hay ninguno, y después de avisar.
- La sesión (`X-HZId`) se pide con `initData` en cada sincronización, con el perfil elegido.
- Cada elemento abre la app oficial con `appmovistar://movistarplus.es/ficha?id=<Id>`.
