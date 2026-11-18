# Simple Proximity Chat

Addon de chat de **texto** para **Simple Voice Chat** y **Simple Radio** (Forge 1.20.1).
No afiliado a Simple Voice Chat ni a Simple Radio.

## Qué hace

**Chat por proximidad**
- Tu mensaje solo llega a los jugadores dentro de la **distancia de voz de Simple Voice Chat**.
- Hasta el 75 % de esa distancia se lee limpio; de ahí al límite se van perdiendo palabras (`hola ____ estás`).
- No cancela el chat: **la consola del servidor, Minecraft Comes Alive y el `frequency ...` de
  Simple Radio siguen recibiendo los mensajes**. Solo se elige a qué jugadores se les envía.

**Walkie-talkies (Simple Radio)**
- Walkie o transceptor **en una mano o en Curios** = lo que escribes sale por su frecuencia y modulación.
  En el inventario normal no hace nada: así se "apaga".
- Lo leen quienes tengan una radio activa en la misma frecuencia y modulación, y quienes estén dentro
  de su `speakingRange`. Si escribes dentro del `listeningRange` de la radio de otro jugador, también
  se transmite.
- Los mensajes por radio salen en ámbar.

**Torres de radio (Simple Radio)**
- Si escribes dentro del alcance de un **Micrófono encendido**, el texto sigue sus cables hasta los
  **Transmisores**, viaja por su frecuencia y llega a los **Receptores/Radios** en esa frecuencia;
  desde ahí sigue los cables hasta los **Altavoces** y lo leen quienes estén en su alcance.
- Walkies y torres se mezclan: un walkie llega a una torre y una torre a un walkie.

**Señal (igual que Simple Radio)**
- Llega si `potencia de transmisión + potencia de recepción ≥ distancia × transmissionDiminishment`.
  Las antenas cuentan, porque se usa la potencia que calcula el propio Simple Radio.
- Cuando la potencia sobrante baja de `diminishThreshold`, aparece estática (palabras perdidas).

**Radio entre dimensiones**
- Sigue la config de Simple Radio (sección `frequency`). Con `crossDimensional = false` (por defecto)
  la radio no sale de su dimensión. Con `true`, el texto cruza con la misma penalización que la voz:
  pierde `dimensionalFMInterference` / `dimensionalAMInterference` de potencia y la distancia se
  calcula con la escala de cada dimensión y `dimensionalScaling`.
- Igual que en Simple Radio, entre dimensiones con la misma escala de coordenadas (mundo normal y
  End) la señal no llega.

**Iconos (encima del micrófono de Voice Chat)**
- 📻 Radio: transmites y recibes · 🎙️ Micrófono: solo transmites · 🔊 Altavoz: solo recibes.
- Respetan la posición, la escala y la opción de ocultar iconos de Voice Chat.
- Requieren el mod en el cliente. Sin él se puede entrar igual, solo no se ven.

**VibrativeVoice (opcional)**
- Escribir genera una vibración débil (sculk/Warden), y también cada walkie o altavoz que reproduce
  el mensaje. Respeta el cooldown y las restricciones de VibrativeVoice.

**Beautified Chat Server (opcional)**
- Los mensajes normales pasan por su formato como siempre. Los distorsionados y los de radio se
  formatean con su propia función, así que también llevan rango, hora y colores de nombre.
- Los mensajes de radio conservan siempre su color ámbar.

**Sin config propia:** todo sale de la config de Voice Chat y de Simple Radio.

## Dependencias

| Mod | Tipo |
|---|---|
| Forge 47+ (Minecraft 1.20.1) | Obligatoria |
| Simple Voice Chat | Obligatoria |
| Simple Radio | Opcional (walkies y torres) |
| Curios | Opcional (radio en un hueco de Curios) |
| VibrativeVoice | Opcional (vibraciones) |
| Beautified Chat Server | Opcional (formato de chat) |

Instálalo **en el servidor y en los clientes** (en los clientes solo hace falta para ver los iconos).
**No lo combines con Proximity Text Chat**: cancela el chat y anularía este mod (y a MCA).

## Diagnóstico

`/simpleproximitychat debug` muestra la distancia de voz, las radios activas de cada jugador
(tipo, frecuencia, FM/AM, potencias) y cómo le llegaría ahora un mensaje tuyo a cada jugador.

## Licencia

Copyright (C) 2026 Dankest0. Licensed under the GNU GPL v3 or later. See LICENSE.
