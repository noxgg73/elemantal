#!/usr/bin/env bash
# Compile le mod Elemental Power et produit un .jar + un .zip a la racine du repo.
#
# IMPORTANT : le Java systeme (25) est incompatible avec Forge/Gradle.
# On compile donc avec le JDK 17 embarque par PrismLauncher (runtime "gamma").
#
# Usage : ./build-mod.sh
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

# JDK 17 fourni par PrismLauncher (Mojang java-runtime-gamma)
JDK17="$HOME/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-gamma"

if [ ! -x "$JDK17/bin/javac" ]; then
  echo "ERREUR : JDK 17 introuvable a : $JDK17" >&2
  echo "Adapte la variable JDK17 dans ce script vers un JDK 17." >&2
  exit 1
fi

export JAVA_HOME="$JDK17"
export PATH="$JAVA_HOME/bin:$PATH"

echo ">> Java utilise : $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
echo ">> Compilation (./gradlew build)..."
./gradlew build --console=plain

# Recupere le jar produit (nom = elementalpower-<version>.jar)
JAR="$(ls -t build/libs/elementalpower-*.jar 2>/dev/null | grep -v sources | head -1 || true)"
if [ -z "$JAR" ]; then
  echo "ERREUR : aucun jar trouve dans build/libs/" >&2
  exit 1
fi

VERSION="$(basename "$JAR" | sed -E 's/^elementalpower-(.*)\.jar$/\1/')"
ZIP="$REPO_DIR/elementalpower-${VERSION}.zip"

# Versions cibles lues depuis gradle.properties
MC_VERSION="$(sed -nE 's/^minecraft_version=([0-9.]+).*/\1/p' gradle.properties | head -1)"
FORGE_VERSION="$(sed -nE 's/^forge_version=([0-9.]+).*/\1/p' gradle.properties | head -1)"
MC_VERSION="${MC_VERSION:-1.20.1}"
FORGE_VERSION="${FORGE_VERSION:-47.2.0}"

# === Construit une INSTANCE PrismLauncher/MultiMC importable ===
# (Prism "Importer" attend un modpack, pas un .jar : on emballe une instance
#  complete Minecraft + Forge avec le mod deja dans .minecraft/mods/)
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

mkdir -p "$STAGE/.minecraft/mods"
cp "$JAR" "$STAGE/.minecraft/mods/"

cat > "$STAGE/instance.cfg" <<CFG
InstanceType=OneSix
name=Elemental Power ${VERSION}
iconKey=default
CFG

cat > "$STAGE/mmc-pack.json" <<JSON
{
    "components": [
        {
            "uid": "net.minecraft",
            "version": "${MC_VERSION}"
        },
        {
            "uid": "net.minecraftforge",
            "version": "${FORGE_VERSION}"
        }
    ],
    "formatVersion": 1
}
JSON

rm -f "$ZIP"
( cd "$STAGE" && zip -r "$ZIP" instance.cfg mmc-pack.json .minecraft ) >/dev/null

echo ""
echo ">> BUILD OK"
echo "   jar      : $JAR"
echo "   instance : $ZIP"
echo "              -> PrismLauncher : Ajouter une instance > Importer depuis un fichier zip"
echo "              (Minecraft ${MC_VERSION} + Forge ${FORGE_VERSION}, mod inclus)"
