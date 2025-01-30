#!/bin/bash

#requires libxml2-utils

# Vérification de l'existence du fichier pom.xml
if [ ! -f "pom.xml" ]; then
  echo "Erreur : Fichier pom.xml introuvable dans le répertoire courant."
  exit 1
fi

# Extraction de la version actuelle depuis le pom.xml
current_version=$(xmllint --xpath "string(//*/*[local-name()='version'])" pom.xml)

# Vérification que la version actuelle est trouvée
if [[ -z "$current_version" ]]; then
  echo "Erreur : Impossible de trouver la version actuelle dans le fichier pom.xml."
  exit 1
fi

echo "Version actuelle : $current_version"

# Découpage de la version (format attendu : major.minor.patch-SNAPSHOT ou major.minor.patch)
IFS='.' read -r major minor patch_snapshot <<< "$current_version"

# Extraction du numéro de version mineur et gestion des suffixes comme -SNAPSHOT
patch=$(echo "$patch_snapshot" | cut -d'-' -f1)
suffix=$(echo "$patch_snapshot" | cut -s -d'-' -f2)

# Vérification du format de la version
if [[ -z "$major" || -z "$minor" || -z "$patch" ]]; then
  echo "Erreur : La version actuelle ('$current_version') n'est pas dans un format valide (major.minor.patch ou major.minor.patch-SNAPSHOT)."
  exit 1
fi

# Incrémentation de la version pathc
new_patch=$((patch + 1))
new_version="$major.$minor.$new_patch"

# Ajout du suffixe si présent (exemple : -SNAPSHOT)
if [[ -n "$suffix" ]]; then
  new_version="$new_version-$suffix"
fi

echo "Nouvelle version : $new_version"

# Mise à jour de la version avec Maven
mvn versions:set -DnewVersion="$new_version"

# Vérification du succès de la mise à jour
if [ $? -eq 0 ]; then
  echo "La version a été mise à jour avec succès à $new_version."
else
  echo "Erreur : La commande Maven a échoué."
  exit 1
fi
