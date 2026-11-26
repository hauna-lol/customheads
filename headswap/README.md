# HeadSwap (Fabric 1.20.1, solo cliente)

Reemplaza **visualmente y solo en tu cliente** las cabezas de equipamiento (skulls) que llevan los jugadores
(Steve, Creeper, Skeleton, Wither Skeleton, Zombie, Piglin, Dragon, cabezas de jugador) por cabezas con la
textura que tu elijas. Tambien puede ocultarlas y excluir jugadores (Reset Head).
No toca el servidor, paquetes, inventarios, skins ni equipamiento real. Los demas jugadores no necesitan el mod.

## Compilar
Requisitos: JDK 17 y conexion a internet (Gradle descarga Minecraft/Yarn/Fabric).

    ./gradlew build          (Windows: gradlew.bat build)

El .jar queda en `build/libs/CustomHeads-1.20.1-Fabric.jar` (no uses el `-sources`). Copialo a `.minecraft/mods/`
junto con Fabric API (0.92.x para 1.20.1). En IntelliJ: File > Open > build.gradle > "Open as Project";
ejecuta la tarea `runClient` para probarlo.

## Configuracion: config/headswap.json
    {
      "enabled": true,              // reemplazo activado/desactivado
      "hideHeads": false,           // ocultar todas las cabezas de equipamiento
      "hideIgnoresExcluded": false, // true: los excluidos (Reset Head) conservan la cabeza aunque hideHeads este activo
      "replacements": [
        { "target": "*", "source": "069a79f4-44e9-4726-a5be-fca90e38aaf5", "enabled": true }
      ],
      "excludedPlayers": [ { "uuid": "...", "name": "..." } ]
    }

### UUID / texturas (replacements)
* `target`: item a reemplazar: `minecraft:creeper_head`, `minecraft:skeleton_skull`, `minecraft:wither_skeleton_skull`,
  `minecraft:zombie_head`, `minecraft:piglin_head`, `minecraft:dragon_head`, `minecraft:player_head`, o `*` (cualquiera).
  La regla exacta gana sobre `*`.
* `source` acepta:
  * UUID de un jugador premium (con o sin guiones): se descarga su skin de Mojang (asincrono; mientras carga se ve la original).
  * Hash de textura (hex de 64 caracteres) o URL `http(s)://textures.minecraft.net/texture/...` (inmediato, sin consultar UUID).
    Solo dominios de Mojang/Minecraft: es una restriccion de vanilla.
* En juego: `/headswap set creeper_head <uuid|hash|url>`, `/headswap set all <...>`, `/headswap remove <target>`,
  `/headswap list`, `/headswap reload` (relee el JSON).

### Ocultar todas las cabezas
`/headswap hide` (alterna), `/headswap hide on|off`, o `"hideHeads": true` en el JSON, o una tecla en
Opciones > Controles > HeadSwap. Solo oculta skulls: no oculta cascos, armaduras, capas, skins ni la cabeza del modelo.

### Reset Head (excluir jugadores)
* A ti: `/headswap reset self` (alterna) o `/resethead self`; tambien hay tecla asignable.
* A otro: `/headswap reset add <nombre|uuid>` (por nombre debe estar en linea; por UUID no hace falta).
* Reactivar: `/headswap reset remove <nombre|uuid>`. Ver: `/headswap reset list`. Vaciar: `/headswap reset clear`.
* Se guarda por UUID en el JSON y persiste entre sesiones. Un jugador excluido muestra su cabeza original.

## Como funciona (arquitectura)
* `config/` JSON + persistencia. `profile/` interpreta UUID/hash/URL y construye un ItemStack PLAYER_HEAD local (cache + hilo aparte).
* `exclusion/` lista Reset Head por UUID. `render/HeadRenderPolicy` decide: ocultar / original / reemplazo.
* `mixin/HeadFeatureRendererMixin`: UN solo @ModifyVariable en `HeadFeatureRenderer#render` que sustituye el
  ItemStack de la ranura de cabeza antes de dibujarlo; vanilla hace el resto (posicion, escala, rotacion, animaciones).
* `command/` y `input/`: comandos de cliente (Fabric API) y atajos de teclado.
