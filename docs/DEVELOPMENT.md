# Développer

Prérequis : Git et JDK 25. Le Wrapper télécharge Gradle 9.7.1 et vérifie son
SHA-256. Il n'est pas nécessaire d'installer Gradle globalement. Java doit être
accessible dans `PATH` ou via `JAVA_HOME`. Python 3.11+ sert aux outils du dépôt.

```powershell
.\gradlew.bat build
.\gradlew.bat :core:test
.\gradlew.bat :fabric:runClient
python scripts/verify_repository.py
python -m unittest discover -s scripts/tests
```

Sur Linux/macOS, remplacer `.\gradlew.bat` par `./gradlew`.
Importer le projet Gradle racine dans IntelliJ IDEA est une option pratique.
Le `JAVA_HOME` d'une console peut être défini pour cette session sans changer
la configuration du système.

`fabric/run/` contient le profil de développement : il est ignoré par Git.
Ne jamais y copier sa seule sauvegarde personnelle. La tâche `runClient`
ne crée pas encore de monde Multisolo et ne propose pas encore de menu.

## Artefacts

`fabric/build/libs/multisolo-0.1.0-dev.1.jar` est le mod de développement.
`multisolo-…-sources.jar` contient les sources. Les sorties de `core/build/`
servent aux développeurs, pas aux joueurs.

La version exacte de Minecraft est inscrite dans le manifeste du mod : ne pas
élargir sa plage pour masquer un portage non testé. Minecraft 26.1+ utilise les
noms officiels non obfusqués : aucune dépendance Yarn n'est configurée.

`minecraft_version` est l'identifiant de téléchargement Mojang/Fabric Meta.
`minecraft_dependency_version` est sa forme normalisée par Fabric Loader :
pour `26.4-snapshot-3`, le Loader expose `26.4-alpha.3`. Le manifeste doit utiliser
cette seconde forme avec une égalité exacte, sinon le Loader refuse le mod.
Lors d'un portage, vérifier cette valeur avec un lancement réel.

## Dépendances

Le socle utilise uniquement Fabric Loader et les bibliothèques du jeu. Ajouter
Fabric API seulement pour une fonctionnalité concrète, avec ses modules
nécessaires inclus via Loom. Vérifier le JAR final et tester sans Fabric API
installé séparément. Éviter les téléchargements de code au lancement du jeu.
