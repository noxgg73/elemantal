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
3. produit a la racine du repo une **instance PrismLauncher** `elementalpower-<version>.zip`
   (format MultiMC : `instance.cfg` + `mmc-pack.json` declarant Minecraft + Forge,
   avec le mod deja dans `.minecraft/mods/`).

Sorties :
- `build/libs/elementalpower-<version>.jar` — le mod seul (a glisser dans le dossier Mods d'une instance existante)
- `elementalpower-<version>.zip` — a la racine : **instance importable** dans PrismLauncher
  (Ajouter une instance > Importer depuis un fichier zip)

> Important : Prism "Importer" attend un *modpack/instance*, pas un simple `.jar`.
> C'est pourquoi le zip est une instance complete et non juste le jar zippe.

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
