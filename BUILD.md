# Compilation du mod

Le Java systeme de cette machine (Java 25) est **incompatible** avec Forge/Gradle
(`Unsupported class file major version 69`). On compile donc avec le **JDK 17**
embarque par PrismLauncher (le runtime Mojang `java-runtime-gamma`).

## Build standard (a faire pour chaque version)

```bash
./build-mod.sh
```

Le script :
1. force `JAVA_HOME` vers le JDK 17 de PrismLauncher,
2. lance `./gradlew build`,
3. produit a la racine du repo un **`elementalpower-<version>.zip`** (contenant le `.jar`),
   pret a etre glisse/importe dans PrismLauncher.

Sorties :
- `build/libs/elementalpower-<version>.jar` — le mod
- `elementalpower-<version>.zip` — a la racine, facile a trouver

## Build manuel (equivalent)

```bash
JAVA_HOME="$HOME/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-gamma" \
  ./gradlew build
```

## Changer de version

Editer `mod_version` dans `gradle.properties`, puis relancer `./build-mod.sh`.
Le nom du jar et du zip suit automatiquement la version.

## Cible

Minecraft **1.20.1** + Forge **47.x** (voir `gradle.properties`).
