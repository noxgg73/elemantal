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

rm -f "$ZIP"
( cd build/libs && zip -j "$ZIP" "$(basename "$JAR")" ) >/dev/null

echo ""
echo ">> BUILD OK"
echo "   jar : $JAR"
echo "   zip : $ZIP  (a importer/glisser dans PrismLauncher)"
