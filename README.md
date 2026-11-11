# PT Mod Loader (Android)

Lanzador de Pizza Tower, sus mods y fangames, sin formatos nuevos: cada juego
es una carpeta normal, igual que en PC. El usuario aporta su propia copia legal;
este repo no incluye el juego ni mods de terceros.

## Cómo se usa
1. **Añadir juego** (carpeta o .zip): tu copia de Pizza Tower, o un fangame.
2. **Mod**: mantén pulsado el juego → *Duplicar* → en la copia, *Aplicar mod
   encima* (carpeta o .zip). Reemplaza archivos como en PC.
3. Toca un juego para jugar. El original nunca se modifica.

## Almacenamiento
```
games/<nombre>/game/          archivos del juego (data.win, audio...)
games/<nombre>/controls.json  (opcional) controles táctiles de ese juego
```
Cada copia ocupa lo que pese el juego.

## Controles
Sprites en `res/drawable-nodpi/spr_*` (joystick de pizza + botones z, x, c,
shift, pause, debug; 2 frames: suelto | pulsado). Ejemplo de layout en
`examples/controls.json`. `"joystick": null` lo oculta.

## Pendiente
- Lanzar el runner de GameMaker (`GameActivity`)
- Editor de layout de controles

## Compilar
`gradle assembleDebug` o GitHub Actions (artifact `loader-debug`).
Release firmado: tag `v1.0.0` + secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.
